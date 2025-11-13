package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMktUserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String status;
    private String role;
    private Long points;
    private LocalDateTime createdAt;
}

