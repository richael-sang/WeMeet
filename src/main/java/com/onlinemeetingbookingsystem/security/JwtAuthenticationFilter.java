package com.onlinemeetingbookingsystem.security;

import com.onlinemeetingbookingsystem.util.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String AUTH_TOKEN_COOKIE_NAME = "AUTH-TOKEN";

    // Define paths that should bypass JWT authentication
    private static final List<String> AUTH_WHITELIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/captchaImage",
            "/api/auth/sendRegisterCode",
            "/api/auth/sendForgetPwdCode",
            "/api/auth/forgetPwd",
            "/autoLogin" // Assuming autoLogin might not need JWT
    );
    
    // Whitelist for static resources and public pages
    private static final List<String> PUBLIC_PATH_PREFIXES = Arrays.asList(
            "/css/", "/scripts/", "/images/", "/webjars/", "/error", 
            "/login", "/register", "/forgot-password"
    );
    
    private static final List<String> PUBLIC_EXACT_PATHS = Arrays.asList("/");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.debug("JwtAuthenticationFilter: Processing request for path: {}", path);
        String jwt = null;

        try {
            // Check if path should be excluded from JWT processing
            if (isPathExcluded(path)) {
                logger.debug("JwtAuthenticationFilter: Path {} is excluded from JWT check.", path);
                filterChain.doFilter(request, response);
                return;
            }

            // 1. Attempt to extract JWT from Authorization header
            jwt = extractJwtFromHeader(request);

            // 2. If not found in header, attempt to extract from cookie
            if (!StringUtils.hasText(jwt)) {
                jwt = extractJwtFromCookie(request);
                if (StringUtils.hasText(jwt)) {
                    logger.debug("JwtAuthenticationFilter: Found JWT in cookie for path: {}", path);
                } else {
                     logger.debug("JwtAuthenticationFilter: No JWT found in header or cookie for path: {}", path);
                     // Continue chain, maybe it's handled by form login or other means
                     filterChain.doFilter(request, response);
                     return;
                }
            } else {
                 logger.debug("JwtAuthenticationFilter: Found JWT in header: {}...", jwt.substring(0, Math.min(jwt.length(), 10)));
            }

            // --- JWT Validation and Authentication Context setting --- 
            try {
                if (jwtUtils.validateToken(jwt)) {
                    logger.debug("JwtAuthenticationFilter: JWT validation successful.");
                    String username = jwtUtils.getUsernameFromToken(jwt);
                    logger.debug("JwtAuthenticationFilter: Username extracted from JWT: {}", username);

                    // Optional: Validate against Redis (if you use Redis for token revocation/management)
                    String redisKey = "token:" + username;
                    String redisToken = null;
                    try {
                        redisToken = redisTemplate.opsForValue().get(redisKey);
                         if (redisToken == null) {
                             logger.warn("JwtAuthenticationFilter: Token for user {} not found in Redis (key: {}). Assuming expired or logged out.", username, redisKey);
                             SecurityContextHolder.clearContext(); // Ensure context is cleared if token is invalid in Redis
                             // Do not return here, let the chain continue so Spring can redirect if needed
                         } else if (!redisToken.equals(jwt)) {
                             logger.warn("JwtAuthenticationFilter: JWT mismatch for user {} in Redis. Provided token might be old or invalid.", username);
                             SecurityContextHolder.clearContext();
                             // Do not return here
                         } else {
                             redisTemplate.expire(redisKey, jwtUtils.getExpiration(), TimeUnit.SECONDS);

                             logger.debug("JwtAuthenticationFilter: Token validated against Redis successfully for user {}.", username);
                             
                             // --- Start: Modified logic to always update context if user differs ---
                             Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                             boolean shouldSetAuth = true; // Default to setting/updating context

                             if (existingAuth != null && existingAuth.isAuthenticated()) {
                                 String existingUsername = null;
                                 if (existingAuth.getPrincipal() instanceof UserDetails) {
                                     existingUsername = ((UserDetails) existingAuth.getPrincipal()).getUsername();
                                 } else if (existingAuth.getPrincipal() instanceof String) {
                                     existingUsername = (String) existingAuth.getPrincipal();
                                 }

                                 if (username.equals(existingUsername)) {
                                     // Context already holds the correct user, no need to update
                                     shouldSetAuth = false;
                                     logger.debug("JwtAuthenticationFilter: Authentication already exists in context for the correct user: {}", username);
                                 } else {
                                     // Context has different user, clear it before setting new one
                                     logger.warn("JwtAuthenticationFilter: Context contained authentication for \'{}', but token is for \'{}'. Clearing and setting new authentication.", existingUsername, username);
                                     SecurityContextHolder.clearContext(); 
                                 }
                             }

                             // Set authentication only if needed (context was null or user mismatched)
                             if (shouldSetAuth) {
                                 logger.debug("JwtAuthenticationFilter: Setting/Updating authentication context for user {}.", username);
                                 UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                 logger.debug("JwtAuthenticationFilter: UserDetails loaded: Username={}, Authorities={}", userDetails.getUsername(), userDetails.getAuthorities());
                                 UsernamePasswordAuthenticationToken authentication =
                                         new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                 authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                 logger.debug("JwtAuthenticationFilter: Created Authentication object: {}", authentication);
                                 SecurityContextHolder.getContext().setAuthentication(authentication);
                                 logger.info("JwtAuthenticationFilter: Successfully set Authentication in SecurityContext for user: {}", username);
                             }
                             // --- End: Modified logic --- 
                         }
                    } catch (Exception e) {
                        logger.error("JwtAuthenticationFilter: Error accessing Redis for token validation (key: {}).", redisKey, e);
                        SecurityContextHolder.clearContext();
                        // Do not return here
                    }
                } else {
                    logger.warn("JwtAuthenticationFilter: JWT validation failed (jwtUtils.validateToken returned false).");
                    SecurityContextHolder.clearContext(); // Clear context if token is invalid
                }
            } catch (ExpiredJwtException e) {
                logger.warn("JwtAuthenticationFilter: JWT token expired: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (UnsupportedJwtException e) {
                logger.warn("JwtAuthenticationFilter: JWT token is unsupported: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (MalformedJwtException e) {
                logger.warn("JwtAuthenticationFilter: Invalid JWT token format: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (IllegalArgumentException e) {
                logger.warn("JwtAuthenticationFilter: JWT claims string is empty or invalid: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) { // Catch other potential errors during validation/loading
                logger.error("JwtAuthenticationFilter: Error processing JWT token.", e);
                SecurityContextHolder.clearContext();
            }
            // --- End JWT Validation --- 

        } catch (Exception e) {
            // Catch broader exceptions happening in the filter itself
            logger.error("JwtAuthenticationFilter: Unexpected error in filter chain.", e);
            SecurityContextHolder.clearContext(); // Ensure context is cleared on unexpected errors
        }

        // Continue the filter chain regardless of authentication outcome here
        logger.debug("JwtAuthenticationFilter: Continuing filter chain for path: {}", path);
        filterChain.doFilter(request, response);
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        logger.trace("JwtAuthenticationFilter: No 'Bearer ' token found in Authorization header.");
        return null;
    }

    private String extractJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    logger.trace("JwtAuthenticationFilter: Found '{}' cookie.", AUTH_TOKEN_COOKIE_NAME);
                    return cookie.getValue();
                }
            }
        }
        logger.trace("JwtAuthenticationFilter: No '{}' cookie found.", AUTH_TOKEN_COOKIE_NAME);
        return null;
    }

    private boolean isPathExcluded(String path) {
        if (path == null) return true;
        
        // Check exact public paths
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }
        
        // Check public path prefixes (static resources, public pages)
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        
        // Check API auth whitelist prefixes
        for (String prefix : AUTH_WHITELIST) {
             if (path.startsWith(prefix)) {
                 return true;
             }
         }

        return false;
    }
} 