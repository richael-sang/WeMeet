package com.onlinemeetingbookingsystem.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CaptchaUtils {

    private final RedisTemplate<String, String> redisTemplate;
    private final Random random = new Random();
    private static final int CAPTCHA_EXPIRATION = 5; // 验证码有效期（分钟）

    @Autowired
    public CaptchaUtils(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a new captcha image and key
     */
    public Map<String, Object> generateCaptcha() {
        String code = generateRandomCode(4);
        BufferedImage image = createCaptchaImage(code);
        String captchaKey = UUID.randomUUID().toString();

        // Store the captcha in Redis with 5-minute expiration
        try {
            redisTemplate.opsForValue().set(
                    "captcha:" + captchaKey,
                    code,
                    CAPTCHA_EXPIRATION,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            // 如果Redis连接失败，使用内存备份方案
            MemoryCaptchaStore.saveCaptcha(captchaKey, code, System.currentTimeMillis() + (CAPTCHA_EXPIRATION * 60 * 1000));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("key", captchaKey);
        result.put("image", image);
        return result;
    }

    /**
     * Validate a captcha against a key
     */
    public boolean validateCaptcha(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        
        String storedCode = null;
        
        try {
            // 尝试从Redis获取验证码
            storedCode = redisTemplate.opsForValue().get("captcha:" + key);
            // 验证成功后从Redis删除验证码
            if (storedCode != null && storedCode.equalsIgnoreCase(code)) {
                redisTemplate.delete("captcha:" + key);
                return true;
            }
        } catch (Exception e) {
            // 如果Redis连接失败，使用内存备份
            storedCode = MemoryCaptchaStore.getCaptcha(key);
            if (storedCode != null && storedCode.equalsIgnoreCase(code)) {
                MemoryCaptchaStore.removeCaptcha(key);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Generate a random alphanumeric code
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Create a captcha image from a code
     */
    private BufferedImage createCaptchaImage(String code) {
        int width = 100;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Set background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw random lines
        g.setColor(getRandomLightColor());
        for (int i = 0; i < 20; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // Draw code
        g.setFont(new Font("Arial", Font.BOLD, 25));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(getRandomColor());
            g.drawString(String.valueOf(code.charAt(i)), 
                    15 + i * 20 + random.nextInt(5), 
                    25 + random.nextInt(10));
        }

        g.dispose();
        return image;
    }

    /**
     * Generate a random color
     */
    private Color getRandomColor() {
        return new Color(
                random.nextInt(100), 
                random.nextInt(100), 
                random.nextInt(100));
    }
    
    /**
     * Generate a random light color
     */
    private Color getRandomLightColor() {
        return new Color(
                100 + random.nextInt(155),
                100 + random.nextInt(155),
                100 + random.nextInt(155));
    }
    
    /**
     * 内存存储验证码备份，当Redis不可用时使用
     */
    private static class MemoryCaptchaStore {
        private static final Map<String, CaptchaEntry> captchas = new HashMap<>();
        
        public static void saveCaptcha(String key, String code, long expiry) {
            captchas.put(key, new CaptchaEntry(code, expiry));
            // 清理过期验证码
            clearExpiredCaptchas();
        }
        
        public static String getCaptcha(String key) {
            CaptchaEntry entry = captchas.get(key);
            if (entry != null && System.currentTimeMillis() < entry.expiry) {
                return entry.code;
            }
            // 如果验证码已过期，移除它
            captchas.remove(key);
            return null;
        }
        
        public static void removeCaptcha(String key) {
            captchas.remove(key);
        }
        
        private static void clearExpiredCaptchas() {
            long now = System.currentTimeMillis();
            captchas.entrySet().removeIf(entry -> now >= entry.getValue().expiry);
        }
        
        private static class CaptchaEntry {
            final String code;
            final long expiry;
            
            CaptchaEntry(String code, long expiry) {
                this.code = code;
                this.expiry = expiry;
            }
        }
    }
} 