package com.xsecret.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmissionRequest {
    private String idNumber;
    private String fullName;
}

