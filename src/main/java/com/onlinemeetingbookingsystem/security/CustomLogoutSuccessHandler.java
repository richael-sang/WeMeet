package com.onlinemeetingbookingsystem.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler implements LogoutSuccessHandler {

    // Define the cookie name consistently
    private static final String AUTH_TOKEN_COOKIE_NAME = "AUTH-TOKEN";

    public CustomLogoutSuccessHandler() {
        // Set the default target URL to redirect to after successful logout
        super.setDefaultTargetUrl("/login?logout");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // Create a cookie with the same name to overwrite/delete the existing one
        Cookie cookie = new Cookie(AUTH_TOKEN_COOKIE_NAME, null); // Value doesn't matter for deletion
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Expire the cookie immediately
        // cookie.setSecure(true); // Ensure this matches the setting during creation (use in HTTPS)
        // cookie.setSameSite("Strict"); // Ensure this matches

        response.addCookie(cookie);
        System.out.println("AUTH-TOKEN cookie cleared.");

        // Call the parent handler to perform the redirect
        super.onLogoutSuccess(request, response, authentication);
    }
} 