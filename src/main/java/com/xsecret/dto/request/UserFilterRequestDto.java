package com.xsecret.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterRequestDto {
    
    private String role; // USER, ADMIN
    private String status; // ACTIVE, INACTIVE, SUSPENDED, BANNED
    private String searchTerm; // Tìm kiếm theo username, email, fullName
    private String sortBy; // createdAt, username, fullName, balance
    private String sortDirection; // asc, desc
    private Integer page;
    private Integer size;
}