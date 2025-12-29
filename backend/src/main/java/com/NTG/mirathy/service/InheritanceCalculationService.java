package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.DTOs.response.FullInheritanceResponse;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.exceptionHandler.InvalidInheritanceCaseException;
import com.NTG.mirathy.rule.InheritanceRule;
import com.NTG.mirathy.util.InheritanceCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InheritanceCalculationService {

    private final List<InheritanceRule> rules;
    private final ArabicInheritanceTextService arabicInheritanceTextService;

    public FullInheritanceResponse calculateProblem(InheritanceCalculationRequest request) {
        validateRequest(request);

        InheritanceCase c = new InheritanceCase(
                request.totalEstate(),
                request.debts(),
                request.will(),
                request.heirs()
        );

        BigDecimal netEstate = c.getNetEstate();

        // حساب الفروض الثابتة
        List<InheritanceShareDto> fixedShares = rules.stream()
                .filter(r -> r.canApply(c))
                .map(r -> r.calculate(c))
                .filter(s -> s != null && s.shareType() == ShareType.FIXED)
                .toList();

        int origin = calculateOrigin(fixedShares);
        Map<InheritanceShareDto, BigDecimal> shareMap = calculateFixedShares(origin, fixedShares);

        //  مجموع الفروض
        BigDecimal fixedSum = shareMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // التعصيب: الابناء والبنات فقط
        BigDecimal remaining = BigDecimal.valueOf(origin).subtract(fixedSum);
        boolean hasChildren = c.countMaleChildren() > 0 || c.countFemaleChildren() > 0;

        if (remaining.compareTo(BigDecimal.ZERO) > 0 && hasChildren) {
            applyChildrenAsaba(c, shareMap, remaining);
        }

        // الرد الشرعي إذا لم يوجد عاصب
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && !hasChildren) {
            applyRaddExcludingSpouses(shareMap, remaining);
        }

        // العَول (Awl) لو مجموع الأسهم تجاوز الأصل
        shareMap = applyAwlIfNeeded(shareMap, origin);

        // تحويل الأسهم لمبالغ دقيقة
        List<InheritanceShareDto> finalShares = convertToAmountsPrecise(netEstate, origin, shareMap);

        BigDecimal distributed = finalShares.stream()
                .map(s -> BigDecimal.valueOf(s.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingEstate = netEstate.subtract(distributed);

        return new FullInheritanceResponse(
                arabicInheritanceTextService.generateText(request),
                request.totalEstate().doubleValue(),
                netEstate.doubleValue(),
                finalShares,
                remainingEstate.doubleValue()
        );
    }


    private int calculateOrigin(List<InheritanceShareDto> shares) {
        return shares.stream()
                .filter(s -> s.shareType() == ShareType.FIXED && s.fixedShare() != null)
                .map(s -> s.fixedShare().getDenominator())
                .reduce(this::lcm)
                .orElse(1);
    }

    private Map<InheritanceShareDto, BigDecimal> calculateFixedShares(int origin, List<InheritanceShareDto> fixedShares) {
        Map<InheritanceShareDto, BigDecimal> map = new LinkedHashMap<>();
        for (InheritanceShareDto s : fixedShares) {
            if (s.fixedShare() != null) {
                BigDecimal shares = BigDecimal.valueOf((origin / s.fixedShare().getDenominator()) * s.fixedShare().getNumerator());
                map.put(s, shares);
            }
        }
        return map;
    }

    private void applyChildrenAsaba(InheritanceCase c, Map<InheritanceShareDto, BigDecimal> shareMap, BigDecimal remaining) {
        int sons = c.countMaleChildren();
        int daughters = c.countFemaleChildren();

        if (sons + daughters == 0) return;

        BigDecimal totalUnits = BigDecimal.valueOf(sons * 2 + daughters);

        if (sons > 0) {
            shareMap.put(new InheritanceShareDto(null, HeirType.SON, ShareType.TAASIB, null,
                            "تعصيب: للذكر مثل حظ الأنثيين"),
                    remaining.multiply(BigDecimal.valueOf(sons * 2)).divide(totalUnits, 10, RoundingMode.HALF_UP));
        }

        if (daughters > 0) {
            shareMap.put(new InheritanceShareDto(null, HeirType.DAUGHTER, ShareType.TAASIB, null,
                            "تعصيب: للذكر مثل حظ الأنثيين"),
                    remaining.multiply(BigDecimal.valueOf(daughters)).divide(totalUnits, 10, RoundingMode.HALF_UP));
        }
    }

    private void applyRaddExcludingSpouses(Map<InheritanceShareDto, BigDecimal> shareMap, BigDecimal remaining) {
        List<Map.Entry<InheritanceShareDto, BigDecimal>> eligible = shareMap.entrySet().stream()
                .filter(e -> e.getKey().shareType() == ShareType.FIXED &&
                        e.getKey().heirType() != HeirType.HUSBAND &&
                        e.getKey().heirType() != HeirType.WIFE)
                .toList();

        BigDecimal totalEligibleShares = eligible.stream()
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalEligibleShares.compareTo(BigDecimal.ZERO) == 0) return;

        for (Map.Entry<InheritanceShareDto, BigDecimal> entry : eligible) {
            BigDecimal extra = remaining.multiply(entry.getValue()).divide(totalEligibleShares, 10, RoundingMode.HALF_UP);
            entry.setValue(entry.getValue().add(extra));
        }
    }

    private Map<InheritanceShareDto, BigDecimal> applyAwlIfNeeded(Map<InheritanceShareDto, BigDecimal> shareMap, int origin) {
        BigDecimal totalShares = shareMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalShares.compareTo(BigDecimal.valueOf(origin)) <= 0) return shareMap;

        Map<InheritanceShareDto, BigDecimal> adjusted = new LinkedHashMap<>();
        for (Map.Entry<InheritanceShareDto, BigDecimal> entry : shareMap.entrySet()) {
            BigDecimal newShares = entry.getValue().multiply(BigDecimal.valueOf(origin))
                    .divide(totalShares, 10, RoundingMode.HALF_UP);
            adjusted.put(entry.getKey(), newShares);
        }
        return adjusted;
    }

    private List<InheritanceShareDto> convertToAmountsPrecise(BigDecimal netEstate, int origin, Map<InheritanceShareDto, BigDecimal> shareMap) {
        BigDecimal shareValue = netEstate.divide(BigDecimal.valueOf(origin), 10, RoundingMode.HALF_UP);
        List<InheritanceShareDto> result = new ArrayList<>();
        for (Map.Entry<InheritanceShareDto, BigDecimal> entry : shareMap.entrySet()) {
            BigDecimal amount = shareValue.multiply(entry.getValue())
                    .setScale(2, RoundingMode.HALF_UP);
            result.add(entry.getKey().withAmount(amount.doubleValue()));
        }
        return result;
    }

    private int lcm(int a, int b) { return a * (b / gcd(a, b)); }
    private int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

    private void validateRequest(InheritanceCalculationRequest request) {
        if (request == null) throw new InvalidInheritanceCaseException("Request must not be null");
        if (request.heirs() == null || request.heirs().isEmpty())
            throw new InvalidInheritanceCaseException("Heirs must not be empty");
        if (request.totalEstate().compareTo(BigDecimal.ZERO) <= 0)
            throw new InvalidInheritanceCaseException("Estate must be positive");
    }
}
