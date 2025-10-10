package com.xsecret.controller;

import com.xsecret.dto.request.BetRequest;
import com.xsecret.dto.response.BetResponse;
import com.xsecret.dto.response.BetStatisticsResponse;
import com.xsecret.entity.User;
import com.xsecret.service.BetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/bets", "/bets"})
@RequiredArgsConstructor
@Slf4j
public class BetController {

    private final BetService betService;

    /**
     * Đặt cược mới
     */
    @PostMapping("/place")
    public ResponseEntity<Map<String, Object>> placeBet(
            @Valid @RequestBody BetRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            log.info("User {} placing bet: {}", userId, request);
            
            BetResponse bet = betService.placeBet(request, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đặt cược thành công");
            response.put("data", bet);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error placing bet: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy danh sách bet của user
     */
    @GetMapping("/my-bets")
    public ResponseEntity<Map<String, Object>> getMyBets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<BetResponse> bets = betService.getUserBets(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bets.getContent());
            response.put("pagination", Map.of(
                "page", bets.getNumber(),
                "size", bets.getSize(),
                "totalElements", bets.getTotalElements(),
                "totalPages", bets.getTotalPages()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user bets: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy bet theo ID
     */
    @GetMapping("/{betId}")
    public ResponseEntity<Map<String, Object>> getBetById(
            @PathVariable Long betId,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            BetResponse bet = betService.getBetById(betId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bet);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting bet by id: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy bet gần đây của user
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentBets(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            List<BetResponse> bets = betService.getRecentBets(userId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bets);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting recent bets: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thống kê bet của user
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getBetStatistics(Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            BetStatisticsResponse statistics = betService.getUserBetStatistics(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting bet statistics: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hủy bet
     */
    @PostMapping("/{betId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBet(
            @PathVariable Long betId,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            BetResponse bet = betService.cancelBet(betId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Hủy cược thành công");
            response.put("data", bet);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error cancelling bet: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kiểm tra kết quả cho 1 bet cụ thể
     */
    @PostMapping("/{betId}/check-result")
    public ResponseEntity<Map<String, Object>> checkSingleBetResult(
            @PathVariable Long betId,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            BetResponse bet = betService.checkSingleBetResult(betId, userId);
            
            // Lấy số điểm mới nhất
            User user = betService.getUserWithCurrentPoints(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bet);
            response.put("currentPoints", user.getPoints());
            
            if (bet.getStatus().equals("WON")) {
                response.put("message", "🎉 Chúc mừng! Bạn đã thắng " + bet.getWinAmount() + " điểm!");
            } else if (bet.getStatus().equals("LOST")) {
                response.put("message", "Rất tiếc! Bạn đã trượt cược này.");
            } else {
                response.put("message", "Kết quả: " + bet.getStatus());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking bet result: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kiểm tra kết quả bet (admin endpoint - để test)
     */
    @PostMapping("/check-results")
    public ResponseEntity<Map<String, Object>> checkBetResults(Authentication authentication) {
        
        try {
            // Chỉ admin mới được gọi API này
            // if (!isAdmin(authentication)) {
            //     throw new RuntimeException("Không có quyền truy cập");
            // }
            
            betService.checkBetResults();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã kiểm tra kết quả tất cả bet");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking bet results: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User không được xác thực");
        }
        
        // Get userId from UserPrincipal
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.xsecret.security.UserPrincipal) {
                return ((com.xsecret.security.UserPrincipal) principal).getId();
            } else {
                throw new RuntimeException("Principal không phải là UserPrincipal");
            }
        } catch (Exception e) {
            log.error("Error getting userId from authentication: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy userId từ authentication: " + e.getMessage());
        }
    }
}
