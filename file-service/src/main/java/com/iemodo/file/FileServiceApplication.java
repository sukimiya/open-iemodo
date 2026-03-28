package com.iemodo.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * File Storage Service - provides object storage via MinIO.
 * 
 * <p>Features:
 * <ul>
 *   <li>File upload (multipart/form-data)
 *   <li>Pre-signed URL generation for secure downloads
 *   <li>File deletion
 *   <li>Multiple buckets (product-images, user-avatars, documents)
 * </ul>
 * 
 * <p>Port: 8090
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.iemodo.file", "com.iemodo.common"})
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
