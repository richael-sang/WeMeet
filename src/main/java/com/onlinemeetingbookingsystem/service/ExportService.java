package com.onlinemeetingbookingsystem.service;

import com.onlinemeetingbookingsystem.entity.AuditLog;
import com.onlinemeetingbookingsystem.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ExportService {

    /**
     * 将用户列表导出为CSV文件
     * @param users 要导出的用户列表
     * @param fields 要包含的字段名称集合（User实体的属性名）。如果为null或空，则使用默认字段
     * @param response HttpServletResponse对象，用于写入文件内容
     * @throws IOException 如果写入响应时发生I/O错误
     */
    void exportUsersToCsv(List<User> users, Set<String> fields, HttpServletResponse response) throws IOException;

    /**
     * 将用户列表导出为JSON文件
     * @param users 要导出的用户列表
     * @param fields 要包含的字段名称集合（User实体的属性名）。如果为null或空，则使用默认字段
     * @param response HttpServletResponse对象，用于写入文件内容
     * @throws IOException 如果处理JSON或写入响应时发生I/O错误
     */
    void exportUsersToJson(List<User> users, Set<String> fields, HttpServletResponse response) throws IOException;

    /**
     * 将审计日志列表导出为CSV文件
     * @param logs 要导出的审计日志列表
     * @param fields 要包含的字段名称集合
     * @param response HttpServletResponse对象
     * @throws IOException 如果写入响应时发生I/O错误
     */
    void exportAuditLogsToCsv(List<AuditLog> logs, Set<String> fields, HttpServletResponse response) throws IOException;
    
    /**
     * 将审计日志列表导出为JSON文件
     * @param logs 要导出的审计日志列表 
     * @param fields 要包含的字段名称集合
     * @param response HttpServletResponse对象
     * @throws IOException 如果处理JSON或写入响应时发生I/O错误
     */
    void exportAuditLogsToJson(List<AuditLog> logs, Set<String> fields, HttpServletResponse response) throws IOException;

    // 如果需要支持 Excel 等其他格式，在这里添加相应的方法声明
    // void exportUsersToExcel(List<User> users, Set<String> fields, HttpServletResponse response) throws IOException;
}
