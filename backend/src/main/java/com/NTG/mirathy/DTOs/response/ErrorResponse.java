package com.NTG.mirathy.DTOs.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;


import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
     LocalDateTime timestamp,
     int status,
     String error,
     String message,
     String path,
     List<String> details
){}