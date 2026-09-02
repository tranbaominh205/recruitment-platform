package com.tbm.recruitment.resume.dto.response;

public record ResumeDownloadResponse(String fileName, String contentType, byte[] content) {}
