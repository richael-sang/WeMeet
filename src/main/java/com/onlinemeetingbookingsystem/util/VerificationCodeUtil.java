package com.onlinemeetingbookingsystem.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Component
public class VerificationCodeUtil {
    private static final Logger logger = Logger.getLogger(VerificationCodeUtil.class.getName());

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${register.code.expire}")
    private Integer codeExpire;

    // 验证码存储前缀
    private static final String REGISTER_PREFIX = "register:";
    private static final String FORGETPWD_PREFIX = "forgetPwd:";

    // 内存备份存储
    private static final Map<String, CodeEntry> CODE_CACHE = new ConcurrentHashMap<>();

    /**
     * 发送验证码（通用方法）
     */
    public ResponseEntity<?> sendVerificationCode(String email, String businessCode, String subject) {
        String sendCodeKey = businessCode + email;
        String code = null;

        try {
            // 检查Redis中是否已存在验证码
            code = redisTemplate.opsForValue().get(sendCodeKey);
            if (code != null) {
                logger.info("Found existing code in Redis for: " + email);
                return ResponseEntity.ok().body(Map.of("code", "0", "message", "请稍后再试，每隔" + codeExpire + "分钟才能发送一次验证码。"));
            }

            // 生成新验证码
            code = generateVerificationCode();
            logger.info("Generated new code for: " + email + ", code: " + code);

            // 存储验证码到Redis
            redisTemplate.opsForValue().set(sendCodeKey, code, codeExpire, TimeUnit.MINUTES);
            logger.info("Stored code in Redis for: " + email);

        } catch (Exception e) {
            logger.warning("Redis error: " + e.getMessage());
            // Redis连接失败，检查内存缓存
            CodeEntry codeEntry = CODE_CACHE.get(sendCodeKey);
            if (codeEntry != null && !codeEntry.isExpired()) {
                logger.info("Found existing code in memory for: " + email);
                return ResponseEntity.ok().body(Map.of("code", "0", "message", "请稍后再试，每隔" + codeExpire + "分钟才能发送一次验证码。"));
            }

            // 生成新验证码
            code = generateVerificationCode();
            logger.info("Generated new code for memory storage: " + email + ", code: " + code);

            // 存储到内存缓存
            long expiryTime = System.currentTimeMillis() + (codeExpire * 60 * 1000);
            CODE_CACHE.put(sendCodeKey, new CodeEntry(code, expiryTime));
            logger.info("Stored code in memory for: " + email);
        }

        // 发送验证码邮件
        try {
            logger.info("Sending email to: " + email);
            sendEmail(email, code, subject);
            logger.info("Email sent successfully to: " + email);
            return ResponseEntity.ok().body(Map.of("code", "1", "message", "验证码已发送，请检查您的邮箱。"));
        } catch (Exception e) {
            logger.severe("Email send error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "发送验证码失败: " + e.getMessage()));
        }
    }

    /**
     * 验证码校验（通用方法）
     */
    public boolean verifyCode(String email, String code, String businessCode, StringBuffer msgBuffer) {
        if (email == null || code == null) {
            msgBuffer.append("邮箱或验证码不能为空");
            return false;
        }

        String codeKey = businessCode + email;
        String correctCode = null;

        try {
            // 尝试从Redis获取验证码
            correctCode = redisTemplate.opsForValue().get(codeKey);
            logger.info("Retrieved code from Redis for: " + email + ", code: " + correctCode);
        } catch (Exception e) {
            logger.warning("Redis verification error: " + e.getMessage());
            // Redis连接失败，从内存缓存获取
            CodeEntry codeEntry = CODE_CACHE.get(codeKey);
            if (codeEntry != null && !codeEntry.isExpired()) {
                correctCode = codeEntry.getCode();
                logger.info("Retrieved code from memory for: " + email + ", code: " + correctCode);
            }
        }

        if (correctCode == null) {
            msgBuffer.append("验证码已过期，请重新获取");
            return false;
        }

        if (!correctCode.equals(code)) {
            msgBuffer.append("验证码输入错误，请重试");
            return false;
        }

        // 验证成功后清除验证码
        clearVerifyCode(email, businessCode);
        return true;
    }

    /**
     * 验证成功后清除验证码
     */
    public void clearVerifyCode(String email, String businessCode) {
        String codeKey = businessCode + email;
        try {
            redisTemplate.delete(codeKey);
            logger.info("Cleared code from Redis for: " + email);
        } catch (Exception e) {
            logger.warning("Redis delete error: " + e.getMessage());
            // Redis连接失败，从内存缓存清除
            CODE_CACHE.remove(codeKey);
            logger.info("Cleared code from memory for: " + email);
        }
    }

    /**
     * 发送注册验证码
     */
    public ResponseEntity<?> sendRegisterCode(String email) {
        return sendVerificationCode(email, REGISTER_PREFIX, "注册验证码");
    }

    /**
     * 发送忘记密码验证码
     */
    public ResponseEntity<?> sendForgetPasswordCode(String email) {
        return sendVerificationCode(email, FORGETPWD_PREFIX, "密码重置验证码");
    }

    /**
     * 生成6位随机验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送验证码邮件
     */
    private void sendEmail(String toEmail, String code, String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String content = "<html><body>" +
                    "<h2>" + subject + "</h2>" +
                    "<p>您的验证码是：<b style='color:#ff6600;font-size:20px'>" + code + "</b></p>" +
                    "<p>有效期为" + codeExpire + "分钟。</p>" +
                    "<p>如果这不是您请求的，请忽略此邮件。</p>" +
                    "</body></html>";

            helper.setText(content, true);
            mailSender.send(message);
            logger.info("Email sent content: " + content);
        } catch (MessagingException e) {
            logger.severe("MessagingException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("发送邮件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证码缓存条目
     */
    private static class CodeEntry {
        private final String code;
        private final long expiryTime;

        public CodeEntry(String code, long expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }

        public String getCode() {
            return code;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
} 