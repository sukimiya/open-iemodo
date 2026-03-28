package com.iemodo.file.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response after successful file upload.
 */
@Data
@Builder
public class FileUploadResponse {

    /** The bucket where the file was stored */
    private String bucket;

    /** The object key (unique identifier) */
    private String objectKey;

    /** Original filename */
    private String originalFilename;

    /** Content type */
    private String contentType;

    /** File size in bytes */
    private long size;

    /** Pre-signed URL for accessing the file (if requested) */
    private String accessUrl;
}
