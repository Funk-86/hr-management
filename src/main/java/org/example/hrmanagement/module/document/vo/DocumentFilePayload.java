package org.example.hrmanagement.module.document.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class DocumentFilePayload {
    private String fileName;
    private String contentType;
    private long contentLength;
    private InputStream inputStream;
}
