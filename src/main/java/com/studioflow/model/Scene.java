package com.studioflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "scenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @Column(name = "scene_number", nullable = false, length = 50)
    private String sceneNumber;

    @Column(nullable = false)
    private String title;

    @Column(name = "shoot_date")
    private LocalDate shootDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SceneStatus status = SceneStatus.READY;

    @Column(nullable = false)
    private String location;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 60;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "scene_actors",
            joinColumns = @JoinColumn(name = "scene_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> actors = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LocalDateTime getScheduledTime() {
        if (scheduledTime != null) {
            return scheduledTime;
        }
        if (shootDate != null && startTime != null) {
            return LocalDateTime.of(shootDate, startTime);
        }
        return null;
    }

    /** Absolute end of the shoot, used by the conflict engine. */
    public LocalDateTime getEndDateTime() {
        if (shootDate != null && endTime != null) {
            return LocalDateTime.of(shootDate, endTime);
        }
        if (scheduledTime != null) {
            return scheduledTime.plusMinutes(durationMinutes != null ? durationMinutes : 60);
        }
        return null;
    }

    public static class SceneBuilder {
        public SceneBuilder sceneNumber(Integer sceneNumber) {
            this.sceneNumber = sceneNumber != null ? String.valueOf(sceneNumber) : null;
            return this;
        }

        public SceneBuilder sceneNumber(String sceneNumber) {
            this.sceneNumber = sceneNumber;
            return this;
        }
    }
}
