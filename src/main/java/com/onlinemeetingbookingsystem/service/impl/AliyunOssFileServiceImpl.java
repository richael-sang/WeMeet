package com.onlinemeetingbookingsystem.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.onlinemeetingbookingsystem.service.FileService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class AliyunOssFileServiceImpl implements FileService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.urlPrefix}")
    private String urlPrefix;

    /**
     * Upload a file to Aliyun OSS
     */
    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String objectName = "avatars/" + UUID.randomUUID().toString() + "." + (extension != null ? extension : "");
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType() != null ? file.getContentType() : getContentType(extension));
            metadata.setContentLength(file.getSize());
            ossClient.putObject(bucketName, objectName, file.getInputStream(), metadata);
            ossClient.setObjectAcl(bucketName, objectName, CannedAccessControlList.PublicRead);
            return "https://" + urlPrefix + "/" + objectName;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * Uploads a file represented by a byte array to Aliyun OSS.
     */
    @Override
    public String uploadFile(byte[] data, String filename, String contentType) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // Use the provided filename or generate one if needed (though filename should be provided)
            // Ensure the filename includes a path like "avatars/"
            String objectName = filename.startsWith("avatars/") ? filename : "avatars/" + filename;
            
            // Set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");
            metadata.setContentLength(data.length);
            
            // Upload file from byte array
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(data), metadata);
            
            // Set public read access
            ossClient.setObjectAcl(bucketName, objectName, CannedAccessControlList.PublicRead);
            
            // Return public URL
            // Ensure double slashes are handled correctly if urlPrefix already ends with /
            String separator = urlPrefix.endsWith("/") || objectName.startsWith("/") ? "" : "/";
            return "https://" + urlPrefix + separator + objectName;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * Delete a file from Aliyun OSS
     */
    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(urlPrefix)) {
             System.err.println("Invalid or non-OSS URL provided for deletion: " + fileUrl);
            return false;
        }
        
        String objectName = fileUrl.substring(fileUrl.indexOf(urlPrefix) + urlPrefix.length());
        if (objectName.startsWith("/")) {
             objectName = objectName.substring(1);
        }
        
        if (objectName.isEmpty()) {
            System.err.println("Could not extract object name from URL: " + fileUrl);
            return false;
        }
        
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            System.out.println("Attempting to delete OSS object: " + objectName);
            ossClient.deleteObject(bucketName, objectName);
            System.out.println("Successfully deleted OSS object: " + objectName);
            return true;
        } catch (Exception e) {
             System.err.println("Failed to delete OSS object " + objectName + ": " + e.getMessage());
             return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * Get the content type based on file extension
     */
    private String getContentType(String fileExtension) {
        if (fileExtension == null) {
            return "application/octet-stream";
        }
        switch (fileExtension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }
} 