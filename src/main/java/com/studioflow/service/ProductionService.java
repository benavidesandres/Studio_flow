package com.studioflow.service;

import com.studioflow.dto.production.ProductionRequest;
import com.studioflow.dto.production.ProductionResponse;

import java.util.List;

public interface ProductionService {

    ProductionResponse createProduction(ProductionRequest request);

    List<ProductionResponse> getAllProductions();

    ProductionResponse getProductionById(Long id);

    ProductionResponse updateProduction(Long id, ProductionRequest request);

    void deleteProduction(Long id);

    void recalculateMetrics(Long productionId);
}
