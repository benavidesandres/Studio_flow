package com.studioflow.service;

import com.studioflow.dto.production.ProductionRequest;
import com.studioflow.dto.production.ProductionResponse;
import com.studioflow.model.Production;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import com.studioflow.repository.ProductionRepository;
import com.studioflow.service.impl.ProductionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionServiceImplTest {

    @Mock
    private ProductionRepository productionRepository;

    @InjectMocks
    private ProductionServiceImpl productionService;

    private Production testProduction;

    @BeforeEach
    void setUp() {
        testProduction = Production.builder()
                .id(1L)
                .title("Oppenheimer: The Series")
                .description("Producción cinematográfica biográfica")
                .totalBudget(BigDecimal.valueOf(100000))
                .spentBudget(BigDecimal.ZERO)
                .progressPercentage(0)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .scenes(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Debe crear una producción con presupuesto y métricas iniciales")
    void shouldCreateProductionSuccessfully() {
        ProductionRequest request = ProductionRequest.builder()
                .title("Oppenheimer: The Series")
                .description("Producción cinematográfica biográfica")
                .totalBudget(BigDecimal.valueOf(100000))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();

        when(productionRepository.save(any(Production.class))).thenAnswer(invocation -> {
            Production p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductionResponse response = productionService.createProduction(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Oppenheimer: The Series", response.getTitle());
        assertEquals(BigDecimal.valueOf(100000), response.getTotalBudget());
        assertEquals(BigDecimal.ZERO, response.getSpentBudget());
        assertEquals(0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Debe recalcular correctamente el presupuesto gastado y el porcentaje de avance")
    void shouldRecalculateMetricsCorrectly() {
        // 4 escenas: 2 completadas, coste total: 5000 + 7000 + 3000 + 5000 = 20000
        Scene s1 = Scene.builder().id(1L).sceneNumber(1).cost(BigDecimal.valueOf(5000)).status(SceneStatus.COMPLETED).build();
        Scene s2 = Scene.builder().id(2L).sceneNumber(2).cost(BigDecimal.valueOf(7000)).status(SceneStatus.COMPLETED).build();
        Scene s3 = Scene.builder().id(3L).sceneNumber(3).cost(BigDecimal.valueOf(3000)).status(SceneStatus.READY).build();
        Scene s4 = Scene.builder().id(4L).sceneNumber(4).cost(BigDecimal.valueOf(5000)).status(SceneStatus.DELAYED).build();

        testProduction.getScenes().addAll(List.of(s1, s2, s3, s4));

        when(productionRepository.findById(1L)).thenReturn(Optional.of(testProduction));
        when(productionRepository.save(any(Production.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productionService.recalculateMetrics(1L);

        // 2 de 4 completadas = 50%
        assertEquals(50, testProduction.getProgressPercentage());
        // 20000 consumido
        assertEquals(BigDecimal.valueOf(20000), testProduction.getSpentBudget());
        verify(productionRepository).save(testProduction);
    }
}
