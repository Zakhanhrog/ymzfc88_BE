package com.xsecret.dto.response;

import com.xsecret.entity.Bet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetResponse {
    
    private Long id;
    private String region;
    private String betType;
    private List<String> selectedNumbers;
    private BigDecimal betAmount;
    private BigDecimal pricePerPoint;
    private BigDecimal totalAmount;
    private BigDecimal odds;
    private BigDecimal potentialWin;
    private Bet.BetStatus status;
    private Boolean isWin;
    private BigDecimal winAmount;
    private List<String> winningNumbers;
    private String resultDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resultCheckedAt;
    
    // Thông tin user (chỉ hiển thị khi cần)
    private Long userId;
    private String username;
    
    public static BetResponse fromEntity(Bet bet) {
        return BetResponse.builder()
                .id(bet.getId())
                .region(bet.getRegion())
                .betType(bet.getBetType())
                .selectedNumbers(parseJsonArray(bet.getSelectedNumbers()))
                .betAmount(bet.getBetAmount())
                .pricePerPoint(bet.getPricePerPoint())
                .totalAmount(bet.getTotalAmount())
                .odds(bet.getOdds())
                .potentialWin(bet.getPotentialWin())
                .status(bet.getStatus())
                .isWin(bet.getIsWin())
                .winAmount(bet.getWinAmount())
                .winningNumbers(parseJsonArray(bet.getWinningNumbers()))
                .resultDate(bet.getResultDate())
                .createdAt(bet.getCreatedAt())
                .updatedAt(bet.getUpdatedAt())
                .resultCheckedAt(bet.getResultCheckedAt())
                .userId(bet.getUser().getId())
                .username(bet.getUser().getUsername())
                .build();
    }
    
    private static List<String> parseJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            // Simple JSON array parsing for strings
            String clean = jsonArray.trim();
            if (clean.startsWith("[") && clean.endsWith("]")) {
                clean = clean.substring(1, clean.length() - 1);
                if (clean.trim().isEmpty()) {
                    return List.of();
                }
                
                return List.of(clean.split(","))
                        .stream()
                        .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                        .toList();
            }
            return List.of(clean);
        } catch (Exception e) {
            return List.of(jsonArray);
        }
    }
}
