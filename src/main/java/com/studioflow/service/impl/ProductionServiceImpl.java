package com.studioflow.service.impl;

import com.studioflow.dto.production.ProductionRequest;
import com.studioflow.dto.production.ProductionResponse;
import com.studioflow.exception.ResourceNotFoundException;
import com.studioflow.model.Production;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import com.studioflow.repository.ProductionRepository;
import com.studioflow.service.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionServiceImpl implements ProductionService {

    private final ProductionRepository productionRepository;

    @Override
    @Transactional
    public ProductionResponse createProduction(ProductionRequest request) {
        log.info("Creando nueva producción audiovisual: {}", request.getTitle());

        Production production = Production.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .totalBudget(request.getTotalBudget() != null ? request.getTotalBudget() : BigDecimal.ZERO)
                .spentBudget(BigDecimal.ZERO)
                .progressPercentage(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Production saved = productionRepository.save(production);
        return ProductionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionResponse> getAllProductions() {
        return productionRepository.findAll().stream()
                .map(ProductionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionResponse getProductionById(Long id) {
        Production production = findProductionOrThrow(id);
        return ProductionResponse.fromEntity(production);
    }

    @Override
    @Transactional
    public ProductionResponse updateProduction(Long id, ProductionRequest request) {
        log.info("Actualizando datos de producción id: {}", id);
        Production production = findProductionOrThrow(id);

        production.setTitle(request.getTitle().trim());
        production.setDescription(request.getDescription());
        if (request.getTotalBudget() != null) {
            production.setTotalBudget(request.getTotalBudget());
        }
        production.setStartDate(request.getStartDate());
        production.setEndDate(request.getEndDate());

        Production updated = productionRepository.save(production);
        return ProductionResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteProduction(Long id) {
        log.info("Eliminando producción id: {}", id);
        Production production = findProductionOrThrow(id);
        productionRepository.delete(production);
    }

    @Override
    @Transactional
    public void recalculateMetrics(Long productionId) {
        Production production = findProductionOrThrow(productionId);
        List<Scene> scenes = production.getScenes();

        if (scenes == null || scenes.isEmpty()) {
            production.setSpentBudget(BigDecimal.ZERO);
            production.setProgressPercentage(0);
        } else {
            BigDecimal totalSpent = scenes.stream()
                    .map(s -> s.getCost() != null ? s.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completedCount = scenes.stream()
                    .filter(s -> s.getStatus() == SceneStatus.COMPLETED)
                    .count();

            int progress = (int) Math.round(((double) completedCount / scenes.size()) * 100);

            production.setSpentBudget(totalSpent);
            production.setProgressPercentage(progress);
        }

        productionRepository.save(production);
        log.info("Métricas recalculadas para producción {}: gastado={}, avance={}%",
                productionId, production.getSpentBudget(), production.getProgressPercentage());
    }

    private Production findProductionOrThrow(Long id) {
        return productionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producción", "id", id));
    }
}
