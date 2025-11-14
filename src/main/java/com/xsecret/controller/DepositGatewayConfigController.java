package com.xsecret.controller;

import com.xsecret.dto.request.DepositGatewayConfigRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.DepositGatewayConfigResponse;
import com.xsecret.service.DepositGatewayConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/deposit-gateway-configs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DepositGatewayConfigController {

    private final DepositGatewayConfigService configService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DepositGatewayConfigResponse>>> getConfigs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 200)), sort);
        Page<DepositGatewayConfigResponse> configs = configService.searchConfigs(keyword, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepositGatewayConfigResponse>> getConfig(@PathVariable Long id) {
        DepositGatewayConfigResponse config = configService.getConfig(id);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepositGatewayConfigResponse>> createConfig(
            @Valid @RequestBody DepositGatewayConfigRequest request
    ) {
        DepositGatewayConfigResponse response = configService.createConfig(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo cấu hình thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepositGatewayConfigResponse>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody DepositGatewayConfigRequest request
    ) {
        DepositGatewayConfigResponse response = configService.updateConfig(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình thành công", response));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<DepositGatewayConfigResponse>> toggleStatus(@PathVariable Long id) {
        DepositGatewayConfigResponse response = configService.toggleStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        configService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.<Void>success("Xóa cấu hình thành công", null));
    }
}

