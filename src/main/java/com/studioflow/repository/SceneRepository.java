package com.studioflow.repository;

import com.studioflow.model.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SceneRepository extends JpaRepository<Scene, Long> {

    List<Scene> findByProductionIdOrderByScheduledTimeAsc(Long productionId);

    List<Scene> findByProductionIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
            Long productionId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT DISTINCT s FROM Scene s LEFT JOIN FETCH s.actors WHERE s.production.id = :productionId ORDER BY s.scheduledTime ASC")
    List<Scene> findAllByProductionIdWithActors(@Param("productionId") Long productionId);

    @Query("SELECT DISTINCT s FROM Scene s JOIN s.actors a WHERE a.id = :actorId ORDER BY s.scheduledTime ASC")
    List<Scene> findScenesByActorId(@Param("actorId") Long actorId);

    @Query("SELECT DISTINCT s FROM Scene s JOIN s.actors a WHERE a.id IN :actorIds AND s.scheduledTime >= :startTime AND s.scheduledTime <= :endTime")
    List<Scene> findCandidateScenesForActors(
            @Param("actorIds") Collection<Long> actorIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
