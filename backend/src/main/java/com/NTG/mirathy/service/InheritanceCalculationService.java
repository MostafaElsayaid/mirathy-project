package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.DTOs.response.FullInheritanceResponse;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.Entity.User;
import com.NTG.mirathy.exceptionHandler.InvalidInheritanceCaseException;
import com.NTG.mirathy.rule.InheritanceRule;
import com.NTG.mirathy.util.InheritanceCase;
import com.NTG.mirathy.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InheritanceCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(InheritanceCalculationService.class);

    private final List<InheritanceRule> rules;
    private final ArabicInheritanceTextService arabicInheritanceTextService;
    private final InheritanceProblemService inheritanceProblemService;
    private final SecurityUtil securityUtil;

    public FullInheritanceResponse calculateProblem(InheritanceCalculationRequest request) {
        validateRequest(request);

        InheritanceCase c = new InheritanceCase(
                request.totalEstate(),
                request.debts(),
                request.will(),
                request.heirs()
        );

        BigDecimal netEstate = c.getNetEstate();


        System.out.println(">>> START calculateProblem, heirs: " + c.getHeirs());

        // 1. تطبيق القواعد لإنتاج الأسهم الأولية (قد تحتوي على بيانات مكررة أو متضاربة)
        List<InheritanceShareDto> allShares = new ArrayList<>();
        for (InheritanceRule rule : rules) {
            if (rule.canApply(c)) {
                InheritanceShareDto dto = rule.calculate(c);
                if (dto != null) allShares.add(dto.withCount(c.count(dto.heirType())));
            }
        }
        System.out.println(">>> allShares raw: " + allShares);
        logger.debug("allShares raw: {}", allShares);

     // 2. تنظيف ودمج الملفات المكررة في allShares
        Map<HeirType, List<InheritanceShareDto>> grouped = allShares.stream()
                .collect(Collectors.groupingBy(InheritanceShareDto::heirType, LinkedHashMap::new, Collectors.toList()));

        Map<HeirType, InheritanceShareDto> cleanedMap = new LinkedHashMap<>();
        for (Map.Entry<HeirType, List<InheritanceShareDto>> entry : grouped.entrySet()) {
            HeirType type = entry.getKey();
            List<InheritanceShareDto> list = entry.getValue();

            if (list.size() == 1) {
                cleanedMap.put(type, list.get(0));
                continue;
            }

       // يُفضّل استخدام THIRD_OF_REMAINDER إن وُجد (في حالات الأم)
            Optional<InheritanceShareDto> thirdOpt = list.stream()
                    .filter(d -> d.fixedShare() == FixedShare.THIRD_OF_REMAINDER)
                    .findFirst();
            if (thirdOpt.isPresent()) {
                cleanedMap.put(type, thirdOpt.get());
                continue;
            }

        // يُفضّل استخدام FIXED على MIXED على TAASIB
            Optional<InheritanceShareDto> fixedOpt = list.stream()
                    .filter(d -> d.shareType() == ShareType.FIXED)
                    .findFirst();
            if (fixedOpt.isPresent()) {
                cleanedMap.put(type, fixedOpt.get());
                continue;
            }

            Optional<InheritanceShareDto> mixedOpt = list.stream()
                    .filter(d -> d.shareType() == ShareType.MIXED)
                    .findFirst();
            if (mixedOpt.isPresent()) {
                cleanedMap.put(type, mixedOpt.get());
                continue;
            }

            // الرجوع إلى الخيار الأول
            cleanedMap.put(type, list.get(0));
        }

        List<InheritanceShareDto> cleanedAllShares = new ArrayList<>(cleanedMap.values());
        System.out.println(">>> allShares cleaned/merged: " + cleanedAllShares);
        logger.debug("allShares cleaned: {}", cleanedAllShares);

                // 3. اكتشاف حالة الهجر
        int fullSiblingsCount = c.count(HeirType.FULL_BROTHER) + c.count(HeirType.FULL_SISTER);
        int maternalSiblingsCount = c.count(HeirType.MATERNAL_BROTHER) + c.count(HeirType.MATERNAL_SISTER);

        boolean hajariDetected = c.isHajariCase() || (fullSiblingsCount > 0 && maternalSiblingsCount > 0);
        System.out.println(">>> hajariDetected: " + hajariDetected + " (full=" + fullSiblingsCount + ", maternal=" + maternalSiblingsCount + ")");

        // في حال تم الكشف عن حجرية، نقم بإزالة الأشقاء
        // الأموميين من cleanedAllShares قبل إنشاء fixedShares/origin
        if (hajariDetected) {
            cleanedAllShares.removeIf(d -> d.heirType() == HeirType.MATERNAL_BROTHER || d.heirType() == HeirType.MATERNAL_SISTER);
            System.out.println(">>> cleanedAllShares after forced hajari cleanup: " + cleanedAllShares);
            logger.debug("cleanedAllShares after forced hajari cleanup: {}", cleanedAllShares);
        }

        // 4. فصل الأسهم الثابتة وأسهم عصبة من cleanallShares
        List<InheritanceShareDto> fixedShares = new ArrayList<>();
        List<InheritanceShareDto> asabaShares = new ArrayList<>();
        for (InheritanceShareDto dto : cleanedAllShares) {
            if (dto.shareType() == ShareType.FIXED || dto.shareType() == ShareType.MIXED) fixedShares.add(dto);
            if (dto.shareType() == ShareType.TAASIB || dto.shareType() == ShareType.MIXED) asabaShares.add(dto);
        }
        System.out.println(">>> fixedShares before origin: " + fixedShares);
        logger.debug("fixedShares before origin: {}", fixedShares);

        // 5. حساب الأصل بعد تنظيف الأسهم الثابتة
        int origin = calculateOrigin(fixedShares);
        System.out.println(">>> origin: " + origin);
        logger.debug("origin: {}", origin);

        Map<HeirType, InheritanceShareDto> dtoMap = new LinkedHashMap<>();
        Map<HeirType, BigDecimal> sharesMap = new LinkedHashMap<>();
        Map<HeirType, Integer> countMap = new LinkedHashMap<>();

        // 6. توزيع حصص ثابتة (تأجيل توزيع الثلث المتبقي)
        List<InheritanceShareDto> thirdOfRemainderDtos = new ArrayList<>();
        for (InheritanceShareDto dto : fixedShares) {
            if (dto.fixedShare() == null || dto.count() == 0) continue;

            FixedShare fs = dto.fixedShare();
            if (fs == FixedShare.THIRD_OF_REMAINDER) {
                thirdOfRemainderDtos.add(dto.withCount(dto.count()));
                continue;
            }

            BigDecimal shareUnits = BigDecimal.valueOf(origin)
                    .multiply(BigDecimal.valueOf(fs.getNumerator()))
                    .divide(BigDecimal.valueOf(fs.getDenominator()), 10, RoundingMode.HALF_UP);

            dtoMap.put(dto.heirType(), dto);
            sharesMap.put(dto.heirType(), shareUnits);
            countMap.put(dto.heirType(), dto.count());
        }
        System.out.println(">>> sharesMap after fixed distribution (pre-hajari): " + sharesMap);
        logger.debug("sharesMap after fixed distribution (pre-hajari): {}", sharesMap);
        logger.debug("thirdOfRemainderDtos (deferred): {}", thirdOfRemainderDtos);
        // 7. في حالة اكتشاف حالة حجرية، يتم توزيع الثلث بالتساوي بين جميع الأشقاء (الأشقاء + الأموميين)

        if (hajariDetected) {
            BigDecimal oneThird = BigDecimal.valueOf(origin).divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);

            int totalSiblings = fullSiblingsCount + maternalSiblingsCount;
            if (totalSiblings > 0) {
                BigDecimal sharePerSibling = oneThird.divide(BigDecimal.valueOf(totalSiblings), 10, RoundingMode.HALF_UP);
                String hajariReason = "مسألة حجرية: الإخوة الأشقاء + الإخوة لأم يشتركون في الثلث بالسوية.";

                putSiblingShare(HeirType.MATERNAL_BROTHER, c, dtoMap, sharesMap, countMap, sharePerSibling, hajariReason);
                putSiblingShare(HeirType.MATERNAL_SISTER, c, dtoMap, sharesMap, countMap, sharePerSibling, hajariReason);
                putSiblingShare(HeirType.FULL_BROTHER, c, dtoMap, sharesMap, countMap, sharePerSibling, hajariReason);
                putSiblingShare(HeirType.FULL_SISTER, c, dtoMap, sharesMap, countMap, sharePerSibling, hajariReason);
            }

            // التأكد من حصول الأم على ثلث الوحدات المتبقية (الوحدات الفعلية، وليس السبب فقط)
            if (c.count(HeirType.MOTHER) > 0) {
                BigDecimal spouseUnits = BigDecimal.ZERO;
                if (sharesMap.containsKey(HeirType.HUSBAND)) spouseUnits = spouseUnits.add(sharesMap.get(HeirType.HUSBAND));
                if (sharesMap.containsKey(HeirType.WIFE)) spouseUnits = spouseUnits.add(sharesMap.get(HeirType.WIFE));

                BigDecimal remainderAfterSpouse = BigDecimal.valueOf(origin).subtract(spouseUnits);
                BigDecimal motherUnits = BigDecimal.ZERO;
                if (remainderAfterSpouse.compareTo(BigDecimal.ZERO) > 0) {
                    motherUnits = remainderAfterSpouse.divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
                }

                InheritanceShareDto motherDto = dtoMap.get(HeirType.MOTHER);
                if (motherDto == null) {
                    InheritanceShareDto newMotherDto = new InheritanceShareDto(
                            HeirType.MOTHER, c.count(HeirType.MOTHER), null, null, ShareType.FIXED, FixedShare.THIRD_OF_REMAINDER, "ثلث الباقي (العمريّة)"
                    );
                    dtoMap.put(HeirType.MOTHER, newMotherDto);
                } else {
                    dtoMap.put(HeirType.MOTHER, motherDto.withReason("ثلث الباقي (العمريّة)"));
                }
                sharesMap.put(HeirType.MOTHER, motherUnits);
                countMap.put(HeirType.MOTHER, c.count(HeirType.MOTHER));

                System.out.println(">>> motherUnits set to: " + motherUnits);
                logger.debug("motherUnits set to: {}", motherUnits);
            }

            System.out.println(">>> sharesMap after hajari processing: " + sharesMap);
            logger.debug("sharesMap after hajari processing: {}", sharesMap);
        }

        // 8. معالجة إدخالات THIRD_OF_REMAINDER المؤجلة (إن وجدت) بأمان
        if (!thirdOfRemainderDtos.isEmpty()) {
            BigDecimal spouseUnits = BigDecimal.ZERO;
            if (sharesMap.containsKey(HeirType.HUSBAND)) spouseUnits = spouseUnits.add(sharesMap.get(HeirType.HUSBAND));
            if (sharesMap.containsKey(HeirType.WIFE)) spouseUnits = spouseUnits.add(sharesMap.get(HeirType.WIFE));

            BigDecimal remainder = BigDecimal.valueOf(origin).subtract(spouseUnits);
            BigDecimal thirdUnit = BigDecimal.ZERO;
            if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                thirdUnit = remainder.divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
            }

            for (InheritanceShareDto dto : thirdOfRemainderDtos) {
                if (!dtoMap.containsKey(dto.heirType())) {
                    dtoMap.put(dto.heirType(), dto.withReason("ثلث الباقي (العمّريّة)"));
                } else {
                    dtoMap.put(dto.heirType(), dtoMap.get(dto.heirType()).withReason("ثلث الباقي (العمّريّة)"));
                }
                sharesMap.put(dto.heirType(), thirdUnit.multiply(BigDecimal.valueOf(dto.count())));
                countMap.put(dto.heirType(), dto.count());
            }
            System.out.println(">>> sharesMap after deferred third processing: " + sharesMap);
            logger.debug("sharesMap after deferred third processing: {}", sharesMap);
        }

        // 9. إعادة حساب المجموع الثابت والوحدات المتبقية
        BigDecimal fixedSum = sharesMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = BigDecimal.valueOf(origin).subtract(fixedSum);
        System.out.println(">>> fixedSum: " + fixedSum + ", remaining units: " + remaining);
        logger.debug("fixedSum: {}, remaining units: {}", fixedSum, remaining);

        //10. توزيع المتبقي (عصبة) إذا كانت هناك وحدات متبقية
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && !asabaShares.isEmpty()) {
            distributeAsaba(c, asabaShares, dtoMap, countMap, sharesMap, remaining);
            System.out.println(">>> sharesMap after asaba distribution: " + sharesMap);
            logger.debug("sharesMap after asaba distribution: {}", sharesMap);
        }
        //١١. تطبيق العول والرد

        applyAwlAndRadd(sharesMap, dtoMap, origin);
        System.out.println(">>> sharesMap after awl/radd: " + sharesMap);
        logger.debug("sharesMap after awl/radd: {}", sharesMap);

        // ١٢. تحويل الوحدات إلى مبالغ نقدية
        BigDecimal shareValue = netEstate.divide(BigDecimal.valueOf(origin), 10, RoundingMode.HALF_UP);
        List<InheritanceShareDto> finalShares = new ArrayList<>();

        for (HeirType type : dtoMap.keySet()) {
            BigDecimal units = sharesMap.getOrDefault(type, BigDecimal.ZERO);
            BigDecimal totalAmount = units.multiply(shareValue).setScale(2, RoundingMode.HALF_UP);
            int count = countMap.getOrDefault(type, 1);
            double amountPerPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue();

            finalShares.add(dtoMap.get(type).withAmounts(amountPerPerson, totalAmount.doubleValue()));
        }

        FullInheritanceResponse response = new FullInheritanceResponse(
                arabicInheritanceTextService.generateText(request),
                request.totalEstate().doubleValue(),
                netEstate.doubleValue(),
                finalShares,
                0.0
        );

        User currentUser = securityUtil.getCurrentUser();
        if (currentUser != null) {
            inheritanceProblemService.saveInheritanceProblem(response, currentUser);
        }

        System.out.println(">>> END calculateProblem, final shares: " + finalShares);
        logger.debug("END calculateProblem, final shares: {}", finalShares);

        return response;
    }

    // Helper Methods
    private void putSiblingShare(HeirType type, InheritanceCase c,
                                 Map<HeirType, InheritanceShareDto> dtoMap,
                                 Map<HeirType, BigDecimal> sharesMap,
                                 Map<HeirType, Integer> countMap,
                                 BigDecimal sharePerPerson,
                                 String reason) {
        int count = c.count(type);
        if (count == 0) return;
        InheritanceShareDto dto = new InheritanceShareDto(type, count, null, null, ShareType.FIXED, null, reason);
        dtoMap.put(type, dto);
        sharesMap.put(type, sharePerPerson.multiply(BigDecimal.valueOf(count)));
        countMap.put(type, count);
    }

    private void distributeAsaba(InheritanceCase c, List<InheritanceShareDto> asabaShares,
                                 Map<HeirType, InheritanceShareDto> dtoMap,
                                 Map<HeirType, Integer> countMap,
                                 Map<HeirType, BigDecimal> sharesMap,
                                 BigDecimal remaining) {
        int totalUnits = 0;
        Map<HeirType, Integer> unitsMap = new LinkedHashMap<>();
        for (InheritanceShareDto dto : asabaShares) {
            HeirType type = dto.heirType();
            int count = c.count(type);
            if (count > 0) {
                int units = count * type.getAsabaUnit(type);
                unitsMap.put(type, units);
                totalUnits += units;
            }
        }
        if (totalUnits == 0) return;

        BigDecimal unitValue = remaining.divide(BigDecimal.valueOf(totalUnits), 10, RoundingMode.HALF_UP);
        for (Map.Entry<HeirType, Integer> entry : unitsMap.entrySet()) {
            HeirType type = entry.getKey();
            BigDecimal totalShare = unitValue.multiply(BigDecimal.valueOf(entry.getValue()));
            InheritanceShareDto dto = dtoMap.get(type);
            if (dto == null) {
                dto = asabaShares.stream().filter(d -> d.heirType() == type).findFirst().orElse(null);
            }
            if (dto != null) dtoMap.put(type, dto);
            sharesMap.put(type, totalShare);
            countMap.put(type, c.count(type));
        }
    }

    private void applyAwlAndRadd(Map<HeirType, BigDecimal> sharesMap,
                                 Map<HeirType, InheritanceShareDto> dtoMap,
                                 int origin) {
        BigDecimal total = sharesMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal originBD = BigDecimal.valueOf(origin);

        if (total.compareTo(originBD) > 0) {
            for (HeirType type : new ArrayList<>(sharesMap.keySet())) {
                BigDecimal adjusted = sharesMap.get(type).multiply(originBD).divide(total, 10, RoundingMode.HALF_UP);
                sharesMap.put(type, adjusted);
            }
            return;
        }

        boolean hasAsaba = dtoMap.values().stream().anyMatch(dto -> dto.shareType() == ShareType.TAASIB || dto.shareType() == ShareType.MIXED);
        if (hasAsaba) return;

        if (total.compareTo(originBD) < 0) {
            BigDecimal remaining = originBD.subtract(total);
            BigDecimal fixedTotal = BigDecimal.ZERO;
            for (HeirType type : sharesMap.keySet()) {
                InheritanceShareDto dto = dtoMap.get(type);
                if (dto != null && dto.shareType() == ShareType.FIXED && !type.isSpouse()) {
                    fixedTotal = fixedTotal.add(sharesMap.get(type));
                }
            }
            if (fixedTotal.compareTo(BigDecimal.ZERO) > 0) {
                for (HeirType type : new ArrayList<>(sharesMap.keySet())) {
                    InheritanceShareDto dto = dtoMap.get(type);
                    if (dto != null && dto.shareType() == ShareType.FIXED && !type.isSpouse()) {
                        BigDecimal current = sharesMap.get(type);
                        BigDecimal fraction = current.divide(fixedTotal, 10, RoundingMode.HALF_UP);
                        sharesMap.put(type, current.add(remaining.multiply(fraction)));
                    }
                }
            }
        }
    }

    private int calculateOrigin(List<InheritanceShareDto> shares) {
        return shares.stream()
                .map(InheritanceShareDto::fixedShare)
                .filter(Objects::nonNull)
                .map(FixedShare::getDenominator)
                .reduce(this::lcm)
                .orElse(1);
    }

    private int lcm(int a, int b) {
        return a / gcd(a, b) * b;
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private void validateRequest(InheritanceCalculationRequest request) {
        if (request == null) throw new InvalidInheritanceCaseException("Request must not be null");
        if (request.heirs() == null || request.heirs().isEmpty()) throw new InvalidInheritanceCaseException("Heirs must not be empty");
        if (request.totalEstate() == null) throw new InvalidInheritanceCaseException("Estate must not be null");
        if (request.totalEstate().compareTo(BigDecimal.ZERO) <= 0) throw new InvalidInheritanceCaseException("Estate must be positive");
    }
}
