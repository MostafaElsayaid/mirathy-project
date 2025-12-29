package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.DTOs.response.FullInheritanceResponse;
import com.NTG.mirathy.DTOs.response.InheritanceMemberResponse;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.exceptionHandler.InvalidInheritanceCaseException;
import com.NTG.mirathy.rule.InheritanceRule;
import com.NTG.mirathy.util.InheritanceCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;




@Slf4j
@Service
@RequiredArgsConstructor
public class InheritanceCalculationService {

    private final List<InheritanceRule> rules;
    private final ArabicInheritanceTextService arabicInheritanceTextService;

    public FullInheritanceResponse calculateProblem(InheritanceCalculationRequest request) {
        log.info("=== START INHERITANCE CALCULATION ===");

        try {
            validateRequest(request);

            InheritanceCase inheritanceCase = new InheritanceCase(
                    request.totalEstate(),
                    request.debts(),
                    request.will(),
                    request.heirs()
            );

            log.info("Created inheritance case with {} heirs", request.heirs().size());

            String title = arabicInheritanceTextService.generateText(request);
            log.info("Generated title: {}", title);

            // الخطوة 1: حساب الأنصبة الأساسية
            List<InheritanceShareDto> shares = calculateAllShares(inheritanceCase);
            log.info("Initial shares calculated: {}", shares.size());

            // التحقق من وجود null في القائمة
            if (shares.contains(null)) {
                log.warn("Found null in shares list. Filtering out null values.");
                shares = shares.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            BigDecimal netEstate = inheritanceCase.getNetEstate();
            log.info("Net estate: {}", netEstate);

            // الخطوة 2: حساب الباقي بعد الفروض
            BigDecimal remainingEstate = calculateRemainingEstate(shares, netEstate);
            log.info("Remaining estate after fixed shares: {}", remainingEstate);

            // الخطوة 3: توزيع الباقي على العصبة (إذا كان هناك باقي)
            if (remainingEstate.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Distributing remaining estate to Aasaba...");
                shares = calculateResidualShares(shares, inheritanceCase, remainingEstate);
                // إعادة حساب الباقي بعد توزيع التعصيب
                remainingEstate = calculateRemainingEstate(shares, netEstate);
            }

            // الخطوة 4: تطبيق الرد (إذا كان هناك باقي ولا يوجد عصبة)
            if (remainingEstate.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Applying Rad to fixed share holders...");
                shares = applyRad(shares, remainingEstate);
                remainingEstate = BigDecimal.ZERO;
            }

            // الخطوة 5: معالجة العول (إذا كان مجموع الأنصبة أكبر من التركة)
            shares = handleAul(shares, netEstate);

            // الخطوة 6: إعادة حساب الباقي النهائي
            remainingEstate = calculateRemainingEstate(shares, netEstate);

            // الخطوة 7: تدوير المبالغ إلى منزلتين عشريتين
            shares = roundShares(shares);

            // التحقق النهائي من عدم وجود null
            List<InheritanceShareDto> finalShares = shares.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("=== CALCULATION COMPLETE ===");
            log.info("Total shares in response: {}", finalShares.size());
            log.info("Final remaining estate: {}", remainingEstate);

            return new FullInheritanceResponse(
                    title,
                    request.totalEstate().doubleValue(),
                    netEstate.doubleValue(),
                    finalShares,
                    remainingEstate.doubleValue()
            );

        } catch (InvalidInheritanceCaseException e) {
            log.error("Invalid inheritance case: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during inheritance calculation: ", e);
            throw new InvalidInheritanceCaseException("An unexpected error occurred: " + e.getMessage());
        }
    }

    private List<InheritanceShareDto> calculateAllShares(InheritanceCase inheritanceCase) {
        List<InheritanceShareDto> shares = new ArrayList<>();

        if (rules == null || rules.isEmpty()) {
            log.warn("No inheritance rules found!");
            return shares;
        }

        log.info("Applying {} inheritance rules...", rules.size());

        for (InheritanceRule rule : rules) {
            try {
                if (rule.canApply(inheritanceCase)) {
                    InheritanceShareDto share = rule.calculate(inheritanceCase);

                    if (share != null && share.heirType() != null) {
                        log.debug("Added share for {}: amount={}, reason={}",
                                share.heirType(), share.amount(), share.reason());
                        shares.add(share);
                    } else {
                        log.warn("Rule {} returned invalid share (null or missing heirType)",
                                rule.getClass().getSimpleName());
                    }
                }
            } catch (Exception e) {
                log.error("Error applying rule {}: {}",
                        rule.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.info("Successfully calculated {} shares", shares.size());
        return shares;
    }

    private List<InheritanceShareDto> calculateResidualShares(
            List<InheritanceShareDto> shares,
            InheritanceCase inheritanceCase,
            BigDecimal remainingEstate
    ) {
        if (shares == null) {
            return new ArrayList<>();
        }

        // تحديد العصبة
        List<HeirType> aasabaHeirs = identifyAasaba(inheritanceCase);
        log.debug("Aasaba heirs identified: {}", aasabaHeirs);

        if (aasabaHeirs.isEmpty()) {
            log.debug("No Aasaba heirs found for residual distribution");
            return shares;
        }

        // حساب أنصبة العصبة
        Map<HeirType, Double> residualShares = calculateAasabaShares(aasabaHeirs, inheritanceCase, remainingEstate);
        log.debug("Residual shares calculated: {}", residualShares);

        // تحديث أو إضافة الأنصبة التعصيبية
        List<InheritanceShareDto> updatedShares = new ArrayList<>(shares);

        for (Map.Entry<HeirType, Double> entry : residualShares.entrySet()) {
            HeirType heirType = entry.getKey();
            Double amount = entry.getValue();

            Optional<InheritanceShareDto> existingShare = shares.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> s.heirType() == heirType)
                    .findFirst();

            if (existingShare.isPresent()) {
                InheritanceShareDto oldShare = existingShare.get();
                int index = updatedShares.indexOf(oldShare);

                Double oldAmount = oldShare.amount() != null ? oldShare.amount() : 0.0;
                Double newAmount = oldAmount + amount;

                InheritanceShareDto updatedShare = new InheritanceShareDto(
                        newAmount,
                        heirType,
                        ShareType.TAASIB,
                        oldShare.fixedShare(),
                        (oldShare.reason() != null ? oldShare.reason() : "") + " + تعصيب"
                );

                updatedShares.set(index, updatedShare);
                log.debug("Updated existing share for {}: {} + {} = {}",
                        heirType, oldAmount, amount, newAmount);
            } else {
                updatedShares.add(new InheritanceShareDto(
                        amount,
                        heirType,
                        ShareType.TAASIB,
                        null,
                        "تعصيب"
                ));
                log.debug("Added new Aasaba share for {}: amount={}", heirType, amount);
            }
        }

        return updatedShares;
    }

    private List<HeirType> identifyAasaba(InheritanceCase inheritanceCase) {
        List<HeirType> aasaba = new ArrayList<>();

        // ترتيب الأولوية للعصبة
        if (inheritanceCase.has(HeirType.SON)) {
            aasaba.add(HeirType.SON);
            if (inheritanceCase.has(HeirType.DAUGHTER)) {
                aasaba.add(HeirType.DAUGHTER);
            }
        } else if (inheritanceCase.has(HeirType.SON_OF_SON)) {
            aasaba.add(HeirType.SON_OF_SON);
            if (inheritanceCase.has(HeirType.DAUGHTER_OF_SON)) {
                aasaba.add(HeirType.DAUGHTER_OF_SON);
            }
        } else if (inheritanceCase.has(HeirType.FATHER) && !inheritanceCase.hasChildren()) {
            aasaba.add(HeirType.FATHER);
        } else if (inheritanceCase.has(HeirType.GRANDFATHER) && !inheritanceCase.hasChildren() && !inheritanceCase.has(HeirType.FATHER)) {
            aasaba.add(HeirType.GRANDFATHER);
        } else if (inheritanceCase.has(HeirType.FULL_BROTHER) && !inheritanceCase.hasMaleChild()) {
            aasaba.add(HeirType.FULL_BROTHER);
            if (inheritanceCase.has(HeirType.FULL_SISTER)) {
                aasaba.add(HeirType.FULL_SISTER);
            }
        }

        return aasaba;
    }

    private Map<HeirType, Double> calculateAasabaShares(
            List<HeirType> aasabaHeirs,
            InheritanceCase inheritanceCase,
            BigDecimal remainingEstate
    ) {
        Map<HeirType, Double> shares = new HashMap<>();

        if (aasabaHeirs.isEmpty() || remainingEstate == null) {
            return shares;
        }

        double remainingAmount = remainingEstate.doubleValue();

        if (aasabaHeirs.contains(HeirType.SON) && aasabaHeirs.contains(HeirType.DAUGHTER)) {
            // أبناء ذكور وإناث: للذكر مثل حظ الأنثيين
            int maleCount = inheritanceCase.countMaleChildren();
            int femaleCount = inheritanceCase.countFemaleChildren();
            int totalUnits = (maleCount * 2) + femaleCount;

            double sharePerUnit = remainingAmount / totalUnits;
            double sharePerMale = sharePerUnit * 2;
            double sharePerFemale = sharePerUnit;

            shares.put(HeirType.SON, sharePerMale * maleCount);
            shares.put(HeirType.DAUGHTER, sharePerFemale * femaleCount);

            log.debug("Aasaba distribution (male:female = 2:1): maleCount={}, femaleCount={}, sharePerUnit={}",
                    maleCount, femaleCount, sharePerUnit);
        } else {
            // توزيع متساوي لباقي العصبة
            double sharePerHeir = remainingAmount / aasabaHeirs.size();
            for (HeirType heir : aasabaHeirs) {
                shares.put(heir, sharePerHeir);
            }
            log.debug("Equal Aasaba distribution: {} heirs, sharePerHeir={}",
                    aasabaHeirs.size(), sharePerHeir);
        }

        return shares;
    }

    private List<InheritanceShareDto> applyRad(
            List<InheritanceShareDto> shares,
            BigDecimal remainingEstate
    ) {
        if (shares == null || shares.isEmpty() || remainingEstate == null) {
            return shares != null ? shares : new ArrayList<>();
        }

        // الحصول على الأنصبة الثابتة فقط
        List<InheritanceShareDto> fixedShares = shares.stream()
                .filter(Objects::nonNull)
                .filter(share -> share.shareType() == ShareType.FIXED)
                .collect(Collectors.toList());

        if (fixedShares.isEmpty()) {
            log.debug("No fixed shares found for Rad distribution");
            return shares;
        }

        // حساب مجموع الأنصبة الثابتة
        double totalFixed = fixedShares.stream()
                .mapToDouble(share -> share.amount() != null ? share.amount() : 0.0)
                .sum();

        if (totalFixed <= 0) {
            log.debug("Total fixed shares is {} (<= 0), cannot apply Rad", totalFixed);
            return shares;
        }

        double remainingAmount = remainingEstate.doubleValue();
        log.debug("Applying Rad: totalFixed={}, remainingAmount={}", totalFixed, remainingAmount);

        // توزيع الباقي بنفس النسب
        List<InheritanceShareDto> updatedShares = new ArrayList<>();

        for (InheritanceShareDto share : shares) {
            if (share == null) {
                continue;
            }

            if (share.shareType() == ShareType.FIXED && share.amount() != null) {
                double proportion = share.amount() / totalFixed;
                double additionalAmount = remainingAmount * proportion;
                double newAmount = share.amount() + additionalAmount;

                log.debug("Rad for {}: {} + {} = {}",
                        share.heirType(), share.amount(), additionalAmount, newAmount);

                updatedShares.add(new InheritanceShareDto(
                        newAmount,
                        share.heirType(),
                        ShareType.RADD,
                        share.fixedShare(),
                        (share.reason() != null ? share.reason() : "") + " + رد"
                ));
            } else {
                updatedShares.add(share);
            }
        }

        return updatedShares;
    }

    private List<InheritanceShareDto> handleAul(
            List<InheritanceShareDto> shares,
            BigDecimal netEstate
    ) {
        if (shares == null || shares.isEmpty() || netEstate == null) {
            return shares != null ? shares : new ArrayList<>();
        }

        // حساب مجموع الأنصبة
        BigDecimal totalShares = shares.stream()
                .filter(Objects::nonNull)
                .map(share -> {
                    Double amount = share.amount();
                    return amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Aul check: totalShares={}, netEstate={}", totalShares, netEstate);

        // إذا كانت التركة تكفي أو زائدة، لا يوجد عول
        if (totalShares.compareTo(netEstate) <= 0) {
            log.debug("No Aul needed (totalShares <= netEstate)");
            return shares;
        }

        // معالجة العول: تخفيض جميع الأنصبة بنسبة متساوية
        BigDecimal ratio = netEstate.divide(totalShares, 10, RoundingMode.HALF_UP);
        log.info("Aul detected! Applying ratio: {}", ratio);

        List<InheritanceShareDto> adjustedShares = new ArrayList<>();

        for (InheritanceShareDto share : shares) {
            if (share == null) {
                continue;
            }

            Double originalAmount = share.amount();
            Double adjustedAmount = originalAmount != null ?
                    BigDecimal.valueOf(originalAmount).multiply(ratio).doubleValue() : 0.0;

            String newReason = share.reason() != null ?
                    share.reason() + " (بعد العول)" : "بعد العول";

            adjustedShares.add(new InheritanceShareDto(
                    adjustedAmount,
                    share.heirType(),
                    share.shareType(),
                    share.fixedShare(),
                    newReason
            ));

            log.debug("Aul adjustment for {}: {} -> {}",
                    share.heirType(), originalAmount, adjustedAmount);
        }

        return adjustedShares;
    }

    private BigDecimal calculateRemainingEstate(List<InheritanceShareDto> shares, BigDecimal netEstate) {
        if (shares == null || netEstate == null) {
            return netEstate != null ? netEstate : BigDecimal.ZERO;
        }

        double totalDistributed = shares.stream()
                .filter(Objects::nonNull)
                .mapToDouble(share -> share.amount() != null ? share.amount() : 0.0)
                .sum();

        BigDecimal remaining = netEstate.subtract(BigDecimal.valueOf(totalDistributed));
        log.debug("Remaining estate calculation: {} - {} = {}",
                netEstate, totalDistributed, remaining);

        return remaining;
    }

    private List<InheritanceShareDto> roundShares(List<InheritanceShareDto> shares) {
        if (shares == null || shares.isEmpty()) {
            return new ArrayList<>();
        }

        return shares.stream()
                .filter(Objects::nonNull)
                .map(share -> {
                    Double amount = share.amount();
                    if (amount == null) {
                        amount = 0.0;
                    }

                    BigDecimal roundedAmount = BigDecimal.valueOf(amount)
                            .setScale(2, RoundingMode.HALF_UP);

                    return new InheritanceShareDto(
                            roundedAmount.doubleValue(),
                            share.heirType(),
                            share.shareType(),
                            share.fixedShare(),
                            share.reason()
                    );
                })
                .collect(Collectors.toList());
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

        // التحقق من أن التركة موجبة
        if (request.totalEstate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInheritanceCaseException("Total estate must be positive");
        }

        // التحقق من أن الديون والوصية غير سالبة
        if (request.debts().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInheritanceCaseException("Debts cannot be negative");
        }

        if (request.will().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInheritanceCaseException("Will cannot be negative");
        }

        // التحقق من أن الديون والوصية لا تتجاوزان التركة
        BigDecimal totalDeductions = request.debts().add(request.will());
        if (totalDeductions.compareTo(request.totalEstate()) > 0) {
            throw new InvalidInheritanceCaseException(
                    "Debts and will cannot exceed total estate"
            );
        }
    }

    // للتوافق مع الإصدارات السابقة
    public InheritanceMemberResponse calculateSingleMember(InheritanceCalculationRequest request) {
        try {
            FullInheritanceResponse fullResponse = calculateProblem(request);

            if (fullResponse.shares().isEmpty()) {
                return new InheritanceMemberResponse(
                        fullResponse.title(),
                        HeirType.FATHER,
                        ShareType.FIXED,
                        FixedShare.SIXTH,
                        0.0,
                        0,
                        "لا يوجد ورثة"
                );
            }

            InheritanceShareDto firstShare = fullResponse.shares().get(0);

            return new InheritanceMemberResponse(
                    fullResponse.title(),
                    firstShare.heirType(),
                    firstShare.shareType(),
                    firstShare.fixedShare(),
                    calculateSharePercentage(firstShare.amount(), fullResponse.netEstate()),
                    getCount(request.heirs(), firstShare.heirType()),
                    firstShare.reason()
            );
        } catch (Exception e) {
            log.error("Error in calculateSingleMember: ", e);
            return new InheritanceMemberResponse(
                    "خطأ في الحساب",
                    HeirType.FATHER,
                    ShareType.FIXED,
                    FixedShare.SIXTH,
                    0.0,
                    0,
                    "حدث خطأ أثناء الحساب: " + e.getMessage()
            );
        }
    }

    private Double calculateSharePercentage(Double amount, Double netEstate) {
        if (netEstate == null || netEstate == 0) {
            return 0.0;
        }
        return (amount / netEstate) * 100;
    }

    private Integer getCount(Map<HeirType, Integer> heirs, HeirType heirType) {
        return heirs != null ? heirs.getOrDefault(heirType, 0) : 0;
    }
}
