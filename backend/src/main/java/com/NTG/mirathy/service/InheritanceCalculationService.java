package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.DTOs.response.InheritanceMemberResponse;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.Entity.InheritanceMember;
import com.NTG.mirathy.exceptionHandler.InvalidInheritanceCaseException;
import com.NTG.mirathy.rule.InheritanceRule;
import com.NTG.mirathy.util.InheritanceCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InheritanceCalculationService {

    private final List<InheritanceRule> rules;
    private final ArabicInheritanceTextService arabicInheritanceTextService;

    public InheritanceMemberResponse calculateProblem(
            InheritanceCalculationRequest request
    ) {

        validateRequest(request);
        InheritanceCase inheritanceCase = new InheritanceCase(request.totalEstate(), request.debts(), request.will(), request.heirs());
        String title = arabicInheritanceTextService.generateText(request);


                List <InheritanceShareDto> members = new ArrayList<>();

        for (InheritanceRule rule : rules) {
            if (rule.canApply(inheritanceCase)) {
                members.add(rule.calculate(inheritanceCase));
            }
        }

        // مؤقتًا – بعدين هترجع list كاملة
        return new InheritanceMemberResponse(
                title,
                HeirType.FATHER,
                ShareType.FIXED,
                FixedShare.SIXTH,
                54.2,
                1,
                "res"
        );
    }

    private void validateRequest(InheritanceCalculationRequest request) {

        if (request == null) {
            throw new InvalidInheritanceCaseException("Request must not be null");
        }

        Map<HeirType, Integer> heirs = request.heirs();

        if (heirs == null || heirs.isEmpty()) {
            throw new InvalidInheritanceCaseException("Heirs must not be empty");
        }


        for (Map.Entry<HeirType, Integer> entry : heirs.entrySet()) {

            HeirType type = entry.getKey();
            Integer count = entry.getValue();

            if (type == null) {
                throw new InvalidInheritanceCaseException("Heir type must not be null");
            }

            if (count == null || count <= 0) {
                throw new InvalidInheritanceCaseException(
                        "Invalid count for heir: " + type
                );
            }

            Integer maxAllowed = type.getMAX_ALLOWED();

            if (maxAllowed != null && count > maxAllowed) {
                throw new InvalidInheritanceCaseException(
                        type + " must not be more than " + maxAllowed
                );
            }


        }

    }

}
