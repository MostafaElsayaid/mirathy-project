package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.response.ProblemReportResponse;
import com.NTG.mirathy.Entity.Enum.ProblemReportStauts;
import com.NTG.mirathy.Entity.InheritanceProblem;
import com.NTG.mirathy.Entity.ProblemReport;
import com.NTG.mirathy.Mapper.ProblemReportsMapper;
import com.NTG.mirathy.Repository.InheritanceProblemRepo;
import com.NTG.mirathy.Repository.ProblemReportRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemReportService {
    private final ProblemReportRepo problemReportRepo;
    private final InheritanceProblemRepo inheritanceProblemRepo;
    private final ProblemReportsMapper problemReportsMapper;

    @Transactional
    public ProblemReportResponse reportProblem(Long problemId, String description) {
        InheritanceProblem returnedProblem = inheritanceProblemRepo.findById(problemId).orElseThrow(
                () -> new IllegalArgumentException("Problem with ID " + problemId + " not found.")
        );
        ProblemReport problemReport = new ProblemReport();
        problemReport.setProblem(returnedProblem);
        problemReport.setCreatedAt(LocalDateTime.now());
        problemReport.setDescription(description);
        problemReport.setStatus(ProblemReportStauts.PENDING);
        problemReport.setUser(returnedProblem.getUser());
        problemReport.setIsDeleted(false);
        ProblemReport savedProblemReport= problemReportRepo.save(problemReport);
        return problemReportsMapper.toProblemReportResponse(savedProblemReport);
    }

    public List<ProblemReportResponse> getAllProblemReports() {
        List<ProblemReport> problemReports = problemReportRepo.findAll();
        return problemReportsMapper.toProblemReportResponses(problemReports);
    }

    public void changeProblemReportStatus(Long id ,ProblemReportStauts status) {
        ProblemReport problemReport = problemReportRepo.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Problem Report with ID " + id + " not found.")
        );
        problemReport.setStatus(status);
        problemReportRepo.save(problemReport);
    }
}
