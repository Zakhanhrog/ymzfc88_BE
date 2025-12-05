package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stream_configs", uniqueConstraints = @UniqueConstraint(columnNames = {"game_type", "table_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 50)
    private GameType gameType;
    
    @Column(name = "table_number")
    private Integer tableNumber; // null for games without tables (like XocDia)
    
    @Column(name = "stream_key", nullable = false, length = 100)
    private String streamKey; // Stream key for OBS (e.g., "xocdia", "sicbo-table1")
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_live_paused", nullable = false)
    @Builder.Default
    private Boolean isLivePaused = false;
    
    @Column(name = "is_live_ended", nullable = false)
    @Builder.Default
    private Boolean isLiveEnded = false;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum GameType {
        XOC_DIA,
        SICBO
    }
}

