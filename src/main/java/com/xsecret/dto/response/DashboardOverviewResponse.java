package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {

    private Summary summary;
    private List<ChartPoint> chart;
    private List<ActivityItem> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalUsers;
        private long activeUsers;
        private long newUsersToday;
        private long onlineUsers;
        private BigDecimal revenueToday;
        private long transactionsTodayCount;
        private BigDecimal transactionsTodayAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private long totalBets;
        private long transactions;
        private BigDecimal transactionAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String id;
        private String type;
        private String username;
        private String description;
        private BigDecimal amount;
        private String status;
        private LocalDateTime time;
    }
}


