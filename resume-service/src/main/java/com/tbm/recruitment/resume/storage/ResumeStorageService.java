package com.tbm.recruitment.resume.storage;

import com.tbm.recruitment.resume.configuration.MinioProperties;
import com.tbm.recruitment.resume.exception.AppException;
import com.tbm.recruitment.resume.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResumeStorageService {

  MinioClient minioClient;
  MinioProperties minioProperties;

  public void upload(String storageKey, MultipartFile file) {

    try {
      ensureBucketExists();

      String contentType = file.getContentType();

      if (contentType == null || contentType.isBlank()) {
        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
      }

      try (InputStream inputStream = file.getInputStream()) {
        minioClient.putObject(
            PutObjectArgs.builder().bucket(minioProperties.bucket()).object(storageKey).stream(
                    inputStream, file.getSize(), -1L)
                .contentType(contentType)
                .build());
      }

    } catch (Exception exception) {
      throw new AppException(ErrorCode.STORAGE_ERROR, exception);
    }
  }

  public void deleteQuietly(String storageKey) {

    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(minioProperties.bucket()).object(storageKey).build());

    } catch (Exception exception) {
      log.warn("Failed to remove orphan Resume object {}: {}", storageKey, exception.getMessage());
    }
  }

  private void ensureBucketExists() throws Exception {

    boolean bucketExists =
        minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(minioProperties.bucket()).build());

    if (!bucketExists) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.bucket()).build());
    }
  }
}
