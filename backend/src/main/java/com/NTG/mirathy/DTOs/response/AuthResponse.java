package com.NTG.mirathy.DTOs.response;

import com.NTG.mirathy.Entity.Enum.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuthResponse(

     String accessToken,
     Long userId,
     String fullName,
     String email,
     Role role,
     Boolean isActive,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
     LocalDateTime createdAt,

     String message,
     Boolean success
){}
