package com.xsecret.dto.request;

import lombok.Data;

@Data
public class UpdateStaffRoleRequest {
    /**
     * Giá trị role mới. Để null hoặc chuỗi rỗng để bỏ phân quyền.
     * Cho phép các giá trị: AGENT, STAFF_TX1, STAFF_TX2, STAFF_XD, STAFF_MKT, STAFF_XNK
     */
    private String staffRole;
}


