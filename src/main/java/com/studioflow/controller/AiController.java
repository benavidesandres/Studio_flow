package com.studioflow.controller;

import com.studioflow.dto.ai.ScriptBreakdownRequest;
import com.studioflow.dto.ai.ScriptBreakdownResponse;
import com.studioflow.service.ai.ScriptBreakdownService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ScriptBreakdownService scriptBreakdownService;

    /** Solo analiza: no guarda nada. Las escenas se crean después con POST /scenes. */
    @PostMapping("/script-breakdown")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<ScriptBreakdownResponse> breakdownScript(@Valid @RequestBody ScriptBreakdownRequest request) {
        return ResponseEntity.ok(scriptBreakdownService.breakdown(request.getScript()));
    }
}
