package com.onlinemeetingbookingsystem.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {
    
    /**
     * Uploads a file represented by MultipartFile.
     *
     * @param file The MultipartFile to upload.
     * @return The URL of the uploaded file.
     * @throws IOException If an I/O error occurs during upload.
     */
    String uploadFile(MultipartFile file) throws IOException;
    
    /**
     * Uploads a file represented by a byte array.
     *
     * @param data The byte array containing the file data.
     * @param filename The desired filename for the uploaded file (including extension).
     * @param contentType The MIME type of the file (e.g., "image/png").
     * @return The URL of the uploaded file.
     * @throws IOException If an I/O error occurs during upload.
     */
    String uploadFile(byte[] data, String filename, String contentType) throws IOException;
    
    /**
     * Delete a file from storage
     * @param fileUrl URL of the file to delete
     * @return true if deletion was successful
     */
    boolean deleteFile(String fileUrl);
} 