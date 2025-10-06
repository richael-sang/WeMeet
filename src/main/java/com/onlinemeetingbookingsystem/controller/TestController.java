package com.onlinemeetingbookingsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test Controller - 仅用于开发环境
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    /**
     * 测试Redis连接
     */
    @GetMapping("/redis")
    public Map<String, Object> testRedis() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试写入
            String key = "test:redis:" + System.currentTimeMillis();
            String value = "测试值：" + System.currentTimeMillis();
            
            stringRedisTemplate.opsForValue().set(key, value, 60, TimeUnit.SECONDS);
            String retrieved = stringRedisTemplate.opsForValue().get(key);
            
            result.put("status", "success");
            result.put("message", "Redis连接正常");
            result.put("written", value);
            result.put("retrieved", retrieved);
            result.put("match", value.equals(retrieved));
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Redis连接异常: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            result.put("cause", e.getCause() != null ? e.getCause().getMessage() : "未知");
        }
        return result;
    }
    
    /**
     * 测试邮件发送
     */
    @GetMapping("/email")
    public Map<String, Object> testEmail(@RequestParam String to) {
        Map<String, Object> result = new HashMap<>();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            // 发件人设置为配置的邮箱
            helper.setFrom("cpt202_group_50@163.com");
            // 收件人
            helper.setTo(to);
            // 主题
            helper.setSubject("测试邮件 - " + System.currentTimeMillis());
            // 内容
            String content = "<html><body>" +
                    "<h2>这是一封测试邮件</h2>" +
                    "<p>发送时间：" + System.currentTimeMillis() + "</p>" +
                    "<p>这封邮件用于测试邮件服务是否正常工作。</p>" +
                    "</body></html>";
            helper.setText(content, true);
            
            // 发送邮件
            mailSender.send(message);
            
            result.put("status", "success");
            result.put("message", "邮件已发送，请查收");
            result.put("sentTo", to);
            
        } catch (MessagingException e) {
            result.put("status", "error");
            result.put("message", "邮件发送失败: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            result.put("cause", e.getCause() != null ? e.getCause().getMessage() : "未知");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "邮件服务异常: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            result.put("cause", e.getCause() != null ? e.getCause().getMessage() : "未知");
        }
        return result;
    }
    
    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Redis状态
        boolean redisAvailable = false;
        try {
            redisAvailable = stringRedisTemplate.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            // Redis不可用
        }
        
        // 邮件服务状态
        boolean mailServiceAvailable = mailSender != null;
        
        status.put("redis", redisAvailable ? "可用" : "不可用");
        status.put("mailService", mailServiceAvailable ? "可用" : "不可用");
        status.put("timestamp", System.currentTimeMillis());
        status.put("javaVersion", System.getProperty("java.version"));
        
        return status;
    }
} 