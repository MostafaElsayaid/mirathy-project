package com.NTG.mirathy.controller;

import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.DTOs.request.ProblemReportRequest;
import com.NTG.mirathy.DTOs.response.FullInheritanceResponse;
import com.NTG.mirathy.DTOs.response.InheritanceMemberResponse;
import com.NTG.mirathy.DTOs.response.ProblemReportResponse;
import com.NTG.mirathy.service.InheritanceCalculationService;
import com.NTG.mirathy.service.ProblemReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class UserController {
    private final InheritanceCalculationService inheritanceCalculationService;
    private final ProblemReportService problemReportService;
    @PostMapping("calculate")
    public ResponseEntity<FullInheritanceResponse> calculate(
            @Valid @RequestBody InheritanceCalculationRequest request){
        FullInheritanceResponse response = inheritanceCalculationService.calculateProblem(request);

        return ResponseEntity.ok(response);
    }
    @PostMapping("submit-report")
    public ResponseEntity<ProblemReportResponse> submitReport(
            @Valid @RequestBody ProblemReportRequest request
    ) {
        ProblemReportResponse response= problemReportService.reportProblem(request.problemId(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
