package com.onlinemeetingbookingsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlinemeetingbookingsystem.entity.AuditLog;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.ExportService;
import com.onlinemeetingbookingsystem.service.UserService;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExportServiceImpl implements ExportService {

    // --- 依赖注入 ---
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // --- 用户导出相关常量 ---
    private static final List<String> DEFAULT_USER_FIELDS = List.of(
            "id", "username", "email", "avatar", "role",
            "locked", "emailVerified", "failedAttempts",
            "createdAt", "lastLoginAt", "lastPasswordChangeAt", "lockUntil"
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- 审计日志导出相关常量 ---
    private static final List<String> DEFAULT_AUDIT_LOG_FIELDS = List.of(
        "id", "timestamp", "adminUsername", "action", "targetUsername", "details"
    );

    // --- 构造函数 ---
    @Autowired
    public ExportServiceImpl(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper.copy();
        configureObjectMapper();
    }

    private void configureObjectMapper() {
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ========================================================================
    //  用户导出方法
    // ========================================================================

    @Override
    public void exportUsersToCsv(List<User> users, Set<String> fields, HttpServletResponse response) throws IOException {
        List<String> finalFields = determineFields(fields, DEFAULT_USER_FIELDS);
        String[] headers = generateHeaders(finalFields);
        log.info("启动User CSV导出，共{}用户，导出字段: {}", users.size(), finalFields);
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"users_export.csv\"");
        writeDataToCsv(response, headers, users, finalFields, this::mapUserToCsvRow);
    }

    @Override
    public void exportUsersToJson(List<User> users, Set<String> fields, HttpServletResponse response) throws IOException {
        List<String> finalFields = determineFields(fields, DEFAULT_USER_FIELDS);
        log.info("启动User JSON导出，共{}用户，导出字段: {}", users.size(), finalFields);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"users_export.json\"");
        List<Map<String, Object>> dataToExport = users.stream()
                .map(user -> mapEntityToMap(user, finalFields, this::getUserFieldValue))
                .collect(Collectors.toList());
        writeDataToJson(response, dataToExport);
    }

    // ========================================================================
    //  审计日志导出方法
    // ========================================================================

    @Override
    public void exportAuditLogsToCsv(List<AuditLog> logs, Set<String> fields, HttpServletResponse response) throws IOException {
        List<String> finalFields = determineFields(fields, DEFAULT_AUDIT_LOG_FIELDS);
        String[] headers = generateHeaders(finalFields);
        log.info("启动AuditLog CSV导出，共{}条日志，导出字段: {}", logs.size(), finalFields);
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit_logs_export.csv\"");
        writeDataToCsv(response, headers, logs, finalFields, this::mapAuditLogToCsvRow);
    }

    @Override
    public void exportAuditLogsToJson(List<AuditLog> logs, Set<String> fields, HttpServletResponse response) throws IOException {
        List<String> finalFields = determineFields(fields, DEFAULT_AUDIT_LOG_FIELDS);
        log.info("启动AuditLog JSON导出，共{}条日志，导出字段: {}", logs.size(), finalFields);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit_logs_export.json\"");
        List<Map<String, Object>> dataToExport = logs.stream()
                .map(logEntry -> mapEntityToMap(logEntry, finalFields, this::getAuditLogFieldValue))
                .collect(Collectors.toList());
        writeDataToJson(response, dataToExport);
    }

    // ========================================================================
    //  通用辅助方法
    // ========================================================================

    // 决定最终要导出的字段
    private List<String> determineFields(Set<String> requestedFields, List<String> defaultFields) {
        if (requestedFields == null || requestedFields.isEmpty()) {
            return defaultFields;
        }
        return requestedFields.stream()
                .filter(defaultFields::contains) // 确保只导出允许的字段
                .collect(Collectors.toList());
    }

    // 生成CSV头部
    private String[] generateHeaders(List<String> fields) {
        // 将字段名转换为适合显示的标题
        return fields.stream()
            .map(field -> {
                if ("timestamp".equals(field)) return "时间";
                if ("id".equals(field)) return "ID";
                if ("adminUsername".equals(field)) return "操作管理员";
                if ("action".equals(field)) return "操作类型";
                if ("targetUsername".equals(field)) return "目标用户";
                if ("details".equals(field)) return "详情";
                return field; // 其他字段保持不变
            })
            .toArray(String[]::new);
    }

    // 将数据写入CSV
    private <T> void writeDataToCsv(HttpServletResponse response, String[] headers, List<T> dataList, List<String> fields, CsvRowMapper<T> rowMapper) throws IOException {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            CSVWriter csvWriter = new CSVWriter(outputStreamWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END))
        {
            outputStreamWriter.write('\ufeff'); // BOM for Excel UTF-8
            csvWriter.writeNext(headers);
            for (T item : dataList) {
                String[] data = rowMapper.map(item, fields);
                csvWriter.writeNext(data);
            }
            csvWriter.flush();
            log.info("CSV数据写入完成");
        } catch (IOException e) {
            log.error("写入CSV数据时发生错误: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("CSV数据写入过程中发生意外错误: {}", e.getMessage(), e);
            throw new IOException("写入CSV数据失败", e);
        }
    }

    // 将数据写入JSON
    private void writeDataToJson(HttpServletResponse response, List<Map<String, Object>> dataToExport) throws IOException {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataToExport);
            response.getWriter().write(json);
            log.info("JSON数据写入完成");
        } catch (IOException e) {
            log.error("写入JSON数据时发生错误: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("JSON数据写入过程中发生意外错误: {}", e.getMessage(), e);
            throw new IOException("写入JSON数据失败", e);
        }
    }

    // 获取用户字段值
    private Object getUserFieldValue(User user, String fieldName) {
        try {
            switch (fieldName) {
                case "id": return user.getId();
                case "username": return user.getUsername();
                case "email": return user.getEmail();
                case "avatar": return user.getAvatar();
                case "role": return user.getRole();
                case "locked": return user.isLocked();
                case "emailVerified": return user.isEmailVerified();
                case "failedAttempts": return user.getFailedAttempts();
                case "createdAt": return user.getCreatedAt();
                case "lastLoginAt": return user.getLastLoginAt();
                case "lastPasswordChangeAt": return user.getLastPasswordChangeAt();
                case "lockUntil": return user.getLockUntil();
                default: return null;
            }
        } catch (Exception e) {
            log.warn("获取用户字段值时出错 (field: {}, userId: {}): {}", fieldName, user.getId(), e.getMessage());
            return null;
        }
    }

    // 获取审计日志字段值
    private Object getAuditLogFieldValue(AuditLog auditLog, String fieldName) {
        try {
            switch (fieldName) {
                case "id": return auditLog.getId();
                case "timestamp": 
                    // 如果有格式化日期，优先使用格式化日期
                    if (auditLog.getFormattedDate() != null && !auditLog.getFormattedDate().isEmpty()) {
                        return auditLog.getFormattedDate();
                    }
                    return auditLog.getTimestamp();
                case "adminId": return auditLog.getAdminId();
                case "adminUsername": return auditLog.getAdminUsername();
                case "targetUserId": return auditLog.getTargetUserId();
                case "targetUsername": return auditLog.getTargetUsername();
                case "action": return auditLog.getAction();
                case "details": return auditLog.getDetails();
                default: return null;
            }
        } catch (Exception e) {
            log.warn("获取审计日志字段值时出错 (field: {}, logId: {}): {}", fieldName, auditLog.getId(), e.getMessage());
            return null;
        }
    }

    // 将用户映射到CSV行
    private String[] mapUserToCsvRow(User user, List<String> fields) {
        return fields.stream()
                .map(field -> formatValueAsString(getUserFieldValue(user, field)))
                .toArray(String[]::new);
    }

    // 将审计日志映射到CSV行
    private String[] mapAuditLogToCsvRow(AuditLog logEntry, List<String> fields) {
        String[] row = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            if ("timestamp".equals(field) && logEntry.getFormattedDate() != null) {
                // 对于时间戳字段，直接使用格式化好的日期字符串
                row[i] = logEntry.getFormattedDate();
            } else {
                // 对于其他字段，使用通用方法
                row[i] = formatValueAsString(getAuditLogFieldValue(logEntry, field));
            }
        }
        return row;
    }

    // 将实体映射到Map
    private <T> Map<String, Object> mapEntityToMap(T entity, List<String> fields, FieldValueGetter<T> getter) {
        if (entity == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (String field : fields) {
            map.put(field, getter.get(entity, field));
        }
        return map;
    }

    // 将值格式化为字符串
    private String formatValueAsString(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).format(DATE_TIME_FORMATTER);
        if (value instanceof Enum) return ((Enum<?>) value).name();
        // 其他情况直接返回字符串表示
        return String.valueOf(value);
    }

    // 内部接口: CSV行映射器
    @FunctionalInterface
    private interface CsvRowMapper<T> {
        String[] map(T entity, List<String> fields);
    }

    // 内部接口: 字段值获取器
    @FunctionalInterface
    private interface FieldValueGetter<T> {
        Object get(T entity, String fieldName);
    }
} 