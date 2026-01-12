package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.DTOs.response.FullInheritanceResponse;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.Entity.User;
import com.NTG.mirathy.exceptionHandler.InvalidInheritanceCaseException;
import com.NTG.mirathy.rule.InheritanceRule;
import com.NTG.mirathy.util.InheritanceCase;
import com.NTG.mirathy.util.SecurityUtil;
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

        // تسجيل معلومات التصحيح
        logCaseInfo(c);

        // معالجة الحالات الخاصة أولاً
        if (handleSpecialCases(c)) {
            return createSpecialCaseResponse(request, netEstate, c);
        }

        // تطبيق جميع القواعد
        List<InheritanceShareDto> allShares = new ArrayList<>();
        for (InheritanceRule rule : rules) {
            if (rule.canApply(c)) {
                InheritanceShareDto dto = rule.calculate(c);
                if (dto != null) {
                    allShares.add(dto.withCount(c.count(dto.heirType())));
                }
            }
        }

        // فصل الفروض عن العصبات
        List<InheritanceShareDto> fixedShares = new ArrayList<>();
        List<InheritanceShareDto> asabaShares = new ArrayList<>();

        for (InheritanceShareDto dto : allShares) {
            if (dto.shareType() == ShareType.FIXED || dto.shareType() == ShareType.MIXED) {
                fixedShares.add(dto);
            }
            if (dto.shareType() == ShareType.TAASIB || dto.shareType() == ShareType.MIXED) {
                asabaShares.add(dto);
            }
        }

        // حساب أصل المسألة
        int origin = calculateOrigin(fixedShares);

        Map<HeirType, InheritanceShareDto> dtoMap = new LinkedHashMap<>();
        Map<HeirType, BigDecimal> sharesMap = new LinkedHashMap<>();
        Map<HeirType, Integer> countMap = new LinkedHashMap<>();

        // ====== معالجة المسألة الحمارية أولاً إذا كانت موجودة ======
        if (c.isHimariyyaCase()) {
            handleHimariyyaCase(c, fixedShares, dtoMap, sharesMap, countMap, origin);
        } else {
            // توزيع الفروض الثابتة (للحالات غير الحمارية)
            distributeFixedShares(c, fixedShares, dtoMap, sharesMap, countMap, origin);
        }

        // حساب الباقي بعد الفروض
        BigDecimal fixedSum = sharesMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = BigDecimal.valueOf(origin).subtract(fixedSum);

        // توزيع العصبات (مع استثناء المسألة الحمارية)
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && !asabaShares.isEmpty() && !c.isHimariyyaCase()) {
            distributeAsaba(c, asabaShares, dtoMap, countMap, sharesMap, remaining);
        }

        // تطبيق العول والرد
        applyAwlAndRadd(sharesMap, dtoMap, origin, c);

        // تحويل الأسهم إلى مبالغ
        List<InheritanceShareDto> finalShares = convertToAmounts(
                dtoMap, sharesMap, countMap, origin, netEstate
        );

        // إنشاء الرد
        FullInheritanceResponse response = createResponse(
                request, netEstate, finalShares, c
        );

        // حفظ المسألة إذا كان هناك مستخدم مسجل
        saveInheritanceProblem(response);

        return response;
    }

    // ==================== معالجة المسألة الحمارية ====================

    private void handleHimariyyaCase(
            InheritanceCase c,
            List<InheritanceShareDto> fixedShares,
            Map<HeirType, InheritanceShareDto> dtoMap,
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, Integer> countMap,
            int origin
    ) {
        System.out.println("===== معالجة المسألة الحمارية =====");

        // 1. حساب نصيب الزوج/الزوجة أولاً
        BigDecimal spouseShare = BigDecimal.ZERO;
        HeirType spouseType = null;
        FixedShare spouseFixedShare = null;

        // البحث عن الزوج/الزوجة في fixedShares
        for (InheritanceShareDto dto : fixedShares) {
            if (dto.heirType() == HeirType.HUSBAND || dto.heirType() == HeirType.WIFE) {
                spouseType = dto.heirType();
                spouseFixedShare = dto.fixedShare();
                break;
            }
        }

        if (spouseFixedShare != null && spouseType != null) {
            spouseShare = BigDecimal.valueOf(origin)
                    .multiply(BigDecimal.valueOf(spouseFixedShare.getNumerator()))
                    .divide(BigDecimal.valueOf(spouseFixedShare.getDenominator()), 10, RoundingMode.HALF_UP);

            // إضافة الزوج/الزوجة إلى الخرائط
            for (InheritanceShareDto dto : fixedShares) {
                if (dto.heirType() == spouseType) {
                    dtoMap.put(spouseType, dto);
                    sharesMap.put(spouseType, spouseShare);
                    countMap.put(spouseType, dto.count());
                    break;
                }
            }
        }

        // 2. الباقي بعد الزوج/الزوجة
        BigDecimal remainingAfterSpouse = BigDecimal.valueOf(origin).subtract(spouseShare);

        // 3. ثلث الباقي للأم والإخوة لأم
        BigDecimal thirdOfRemaining = remainingAfterSpouse
                .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);

        // 4. تقسيم الثلث بين الأم والإخوة لأم
        int maternalMales = c.count(HeirType.MATERNAL_BROTHER);
        int maternalFemales = c.count(HeirType.MATERNAL_SISTER);

        // الأم تأخذ النصف من الثلث (أي سدس الباقي)
        BigDecimal motherShare = thirdOfRemaining
                .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);

        // الإخوة لأم يأخذون النصف الآخر من الثلث (سدس الباقي)
        BigDecimal maternalSiblingsShare = thirdOfRemaining.subtract(motherShare);

        // 5. توزيع حصة الإخوة لأم (للذكر مثل حظ الأنثيين)
        int totalMaternalUnits = (maternalMales * 2) + maternalFemales;

        if (totalMaternalUnits > 0) {
            BigDecimal unitValue = maternalSiblingsShare
                    .divide(BigDecimal.valueOf(totalMaternalUnits), 10, RoundingMode.HALF_UP);

            // توزيع على الإخوة الذكور لأم
            if (maternalMales > 0) {
                BigDecimal maleShare = unitValue.multiply(BigDecimal.valueOf(maternalMales * 2));
                for (InheritanceShareDto dto : fixedShares) {
                    if (dto.heirType() == HeirType.MATERNAL_BROTHER) {
                        dtoMap.put(HeirType.MATERNAL_BROTHER, dto);
                        sharesMap.put(HeirType.MATERNAL_BROTHER, maleShare);
                        countMap.put(HeirType.MATERNAL_BROTHER, maternalMales);
                        break;
                    }
                }
            }

            // توزيع على الأخوات لأم
            if (maternalFemales > 0) {
                BigDecimal femaleShare = unitValue.multiply(BigDecimal.valueOf(maternalFemales));
                for (InheritanceShareDto dto : fixedShares) {
                    if (dto.heirType() == HeirType.MATERNAL_SISTER) {
                        dtoMap.put(HeirType.MATERNAL_SISTER, dto);
                        sharesMap.put(HeirType.MATERNAL_SISTER, femaleShare);
                        countMap.put(HeirType.MATERNAL_SISTER, maternalFemales);
                        break;
                    }
                }
            }
        }

        // 6. الأم
        for (InheritanceShareDto dto : fixedShares) {
            if (dto.heirType() == HeirType.MOTHER) {
                dtoMap.put(HeirType.MOTHER, dto);
                sharesMap.put(HeirType.MOTHER, motherShare);
                countMap.put(HeirType.MOTHER, dto.count());
                break;
            }
        }

        // 7. الإخوة الأشقاء - محجوبون حجب حرمان (لا نضيفهم)
        System.out.println("تم تطبيق المسألة الحمارية بنجاح");
        System.out.println("الزوج/الزوجة: " + spouseShare + " من " + origin);
        System.out.println("الأم: " + motherShare + " من " + origin);
        System.out.println("الإخوة لأم: " + maternalSiblingsShare + " من " + origin);
    }

    private void distributeFixedShares(
            InheritanceCase c,
            List<InheritanceShareDto> fixedShares,
            Map<HeirType, InheritanceShareDto> dtoMap,
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, Integer> countMap,
            int origin
    ) {
        // فصل حالات THIRD_REMAINDER_SHARED (للمسائل المشتركة)
        List<InheritanceShareDto> thirdRemainingSharedList = new ArrayList<>();

        for (InheritanceShareDto dto : fixedShares) {
            if (dto.fixedShare() == null || dto.count() == 0) continue;

            FixedShare fs = dto.fixedShare();

            // إذا كان THIRD_REMAINDER_SHARED، نجمعه للمعالجة لاحقاً
            if (fs == FixedShare.THIRD_REMAINDER_SHARED) {
                thirdRemainingSharedList.add(dto);
                continue;
            }

            BigDecimal shareUnits;

            // المسألة العمرية (ثلث الباقي للأم)
            if (fs == FixedShare.THIRD_OF_REMAINDER) {
                shareUnits = calculateThirdOfRemaining(c, fixedShares, origin);
            } else {
                // باقي الفروض
                shareUnits = BigDecimal.valueOf(origin)
                        .multiply(BigDecimal.valueOf(fs.getNumerator()))
                        .divide(BigDecimal.valueOf(fs.getDenominator()), 10, RoundingMode.HALF_UP);
            }

            dtoMap.put(dto.heirType(), dto);
            sharesMap.put(dto.heirType(), shareUnits);
            countMap.put(dto.heirType(), dto.count());
        }

        // معالجة حالات THIRD_REMAINDER_SHARED إذا وجدت
        if (!thirdRemainingSharedList.isEmpty()) {
            handleThirdRemainingShared(c, thirdRemainingSharedList, dtoMap, sharesMap, countMap, origin);
        }
    }

    private BigDecimal calculateThirdOfRemaining(
            InheritanceCase c,
            List<InheritanceShareDto> fixedShares,
            int origin
    ) {
        BigDecimal spouseShare = fixedShares.stream()
                .filter(d -> d.heirType() == HeirType.WIFE || d.heirType() == HeirType.HUSBAND)
                .map(d -> {
                    FixedShare s = d.fixedShare();
                    return BigDecimal.valueOf(origin)
                            .multiply(BigDecimal.valueOf(s.getNumerator()))
                            .divide(BigDecimal.valueOf(s.getDenominator()), 10, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainder = BigDecimal.valueOf(origin).subtract(spouseShare);
        return remainder.divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
    }

    private void handleThirdRemainingShared(
            InheritanceCase c,
            List<InheritanceShareDto> thirdRemainingSharedList,
            Map<HeirType, InheritanceShareDto> dtoMap,
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, Integer> countMap,
            int origin
    ) {
        // حساب نصيب الزوج/الزوجة أولاً
        BigDecimal spouseShare = BigDecimal.ZERO;
        if (sharesMap.containsKey(HeirType.HUSBAND)) {
            spouseShare = sharesMap.get(HeirType.HUSBAND);
        } else if (sharesMap.containsKey(HeirType.WIFE)) {
            spouseShare = sharesMap.get(HeirType.WIFE);
        }

        // الباقي بعد الزوج/الزوجة
        BigDecimal remainingAfterSpouse = BigDecimal.valueOf(origin).subtract(spouseShare);

        // ثلث الباقي
        BigDecimal thirdOfRemaining = remainingAfterSpouse
                .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);

        // تقسيم الثلث بين جميع أصحاب THIRD_REMAINDER_SHARED
        int totalUnits = 0;
        Map<HeirType, Integer> unitsMap = new HashMap<>();

        for (InheritanceShareDto dto : thirdRemainingSharedList) {
            HeirType type = dto.heirType();
            int count = dto.count();
            int units = 0;

            switch (type) {
                case MOTHER:
                    units = count * 1;
                    break;
                case MATERNAL_BROTHER:
                    units = count * 2;
                    break;
                case MATERNAL_SISTER:
                    units = count * 1;
                    break;
                default:
                    units = count * 1; // وحدة افتراضية لأنواع أخرى
            }

            unitsMap.put(type, units);
            totalUnits += units;
        }

        if (totalUnits == 0) return;

        BigDecimal unitValue = thirdOfRemaining
                .divide(BigDecimal.valueOf(totalUnits), 10, RoundingMode.HALF_UP);

        // توزيع الثلث
        for (Map.Entry<HeirType, Integer> entry : unitsMap.entrySet()) {
            HeirType type = entry.getKey();
            BigDecimal share = unitValue.multiply(BigDecimal.valueOf(entry.getValue()));

            // البحث عن الـ DTO المناسب
            InheritanceShareDto dto = thirdRemainingSharedList.stream()
                    .filter(d -> d.heirType() == type)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("لم يتم العثور على DTO للوريث: " + type));

            dtoMap.put(type, dto);
            sharesMap.put(type, share);
            countMap.put(type, c.count(type));
        }
    }

    private void distributeAsaba(
            InheritanceCase c,
            List<InheritanceShareDto> asabaShares,
            Map<HeirType, InheritanceShareDto> dtoMap,
            Map<HeirType, Integer> countMap,
            Map<HeirType, BigDecimal> sharesMap,
            BigDecimal remaining
    ) {
        // التحقق من المسألة الحجرية: وجود فرع وارث ذكر مع أب
        boolean hasMaleDescendant = c.hasMaleDescendant();
        boolean hasFather = c.has(HeirType.FATHER);
        boolean hasFemaleDescendantOnly = c.hasFemaleDescendantOnly();

        // في المسألة الحجرية: استبعاد الأب من العصبات
        if (hasMaleDescendant && hasFather) {
            asabaShares.removeIf(dto -> dto.heirType() == HeirType.FATHER);
            asabaShares.removeIf(dto -> dto.heirType() == HeirType.GRANDFATHER);
        }

        // إذا كان هناك بنات فقط مع أب، نضمن دخول الأب في العصبة
        if (hasFemaleDescendantOnly && hasFather) {
            boolean fatherExists = asabaShares.stream()
                    .anyMatch(dto -> dto.heirType() == HeirType.FATHER);

            if (!fatherExists) {
                InheritanceShareDto fatherDto = dtoMap.get(HeirType.FATHER);
                if (fatherDto != null && fatherDto.shareType() == ShareType.MIXED) {
                    asabaShares.add(fatherDto);
                }
            }
        }

        int totalUnits = 0;
        Map<HeirType, Integer> unitsMap = new LinkedHashMap<>();

        for (InheritanceShareDto dto : asabaShares) {
            HeirType type = dto.heirType();
            int count = c.count(type);

            if (count > 0 && type.getAsabaUnit() > 0) {
                int units = count * type.getAsabaUnit();
                unitsMap.put(type, units);
                totalUnits += units;
            }
        }

        if (totalUnits == 0) return;

        BigDecimal unitValue = remaining.divide(BigDecimal.valueOf(totalUnits), 10, RoundingMode.HALF_UP);

        for (Map.Entry<HeirType, Integer> entry : unitsMap.entrySet()) {
            HeirType type = entry.getKey();
            BigDecimal totalShare = unitValue.multiply(BigDecimal.valueOf(entry.getValue()));

            dtoMap.put(type, asabaShares.stream()
                    .filter(d -> d.heirType() == type)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("لم يتم العثور على DTO للعاصب: " + type)));

            sharesMap.put(type, totalShare);
            countMap.put(type, c.count(type));
        }
    }

    private void applyAwlAndRadd(
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, InheritanceShareDto> dtoMap,
            int origin,
            InheritanceCase c
    ) {
        BigDecimal total = sharesMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal originBD = BigDecimal.valueOf(origin);

        // العول (إذا زادت الأسهم عن الأصل)
        if (total.compareTo(originBD) > 0) {
            applyAwl(sharesMap, total, originBD);
            return;
        }

        // الرد
        if (total.compareTo(originBD) < 0) {
            applyRadd(sharesMap, dtoMap, total, originBD, c);
        }
    }

    private void applyAwl(
            Map<HeirType, BigDecimal> sharesMap,
            BigDecimal total,
            BigDecimal originBD
    ) {
        for (HeirType type : sharesMap.keySet()) {
            BigDecimal adjusted = sharesMap.get(type)
                    .multiply(originBD)
                    .divide(total, 10, RoundingMode.HALF_UP);
            sharesMap.put(type, adjusted);
        }
    }

    private void applyRadd(
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, InheritanceShareDto> dtoMap,
            BigDecimal total,
            BigDecimal originBD,
            InheritanceCase c
    ) {
        BigDecimal remaining = originBD.subtract(total);

        // حالة المسألة الحجرية: الرد للفرع الذكر فقط
        if (c.isHijriyyaCase()) {
            distributeRaddToMaleDescendant(sharesMap, remaining);
            return;
        }

        // حالة عدم وجود عصبات: الرد على أصحاب الفروض
        if (!c.hasAsaba()) {
            distributeRaddToFixedHeirs(sharesMap, dtoMap, remaining);
            return;
        }

        // حالة وجود عصبات: الرد على العصبات حسب الأولوية
        distributeRaddToAsaba(sharesMap, dtoMap, remaining, c);
    }

    private void distributeRaddToMaleDescendant(
            Map<HeirType, BigDecimal> sharesMap,
            BigDecimal remaining
    ) {
        for (HeirType type : sharesMap.keySet()) {
            if (type == HeirType.SON || type == HeirType.SON_OF_SON) {
                BigDecimal newShare = sharesMap.get(type).add(remaining);
                sharesMap.put(type, newShare);
                break;
            }
        }
    }

    private void distributeRaddToFixedHeirs(
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, InheritanceShareDto> dtoMap,
            BigDecimal remaining
    ) {
        BigDecimal fixedTotal = BigDecimal.ZERO;

        for (HeirType type : sharesMap.keySet()) {
            InheritanceShareDto dto = dtoMap.get(type);
            if (dto.shareType() == ShareType.FIXED && !type.isSpouse()) {
                fixedTotal = fixedTotal.add(sharesMap.get(type));
            }
        }

        if (fixedTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (HeirType type : sharesMap.keySet()) {
                InheritanceShareDto dto = dtoMap.get(type);
                if (dto.shareType() == ShareType.FIXED && !type.isSpouse()) {
                    BigDecimal current = sharesMap.get(type);
                    BigDecimal fraction = current.divide(fixedTotal, 10, RoundingMode.HALF_UP);
                    BigDecimal additional = remaining.multiply(fraction);
                    sharesMap.put(type, current.add(additional));
                }
            }
        }
    }

    private void distributeRaddToAsaba(
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, InheritanceShareDto> dtoMap,
            BigDecimal remaining,
            InheritanceCase c
    ) {
        List<HeirType> asabaList = new ArrayList<>();

        for (HeirType type : sharesMap.keySet()) {
            if (type.canBeAsaba(c)) {
                asabaList.add(type);
            }
        }

        if (asabaList.isEmpty()) return;

        int totalUnits = getTotalAsabaUnits(asabaList, c);
        BigDecimal unitValue = remaining.divide(
                BigDecimal.valueOf(totalUnits), 10, RoundingMode.HALF_UP
        );

        for (HeirType type : asabaList) {
            int units = calculateAsabaUnits(type, c);
            BigDecimal additional = unitValue.multiply(BigDecimal.valueOf(units));
            sharesMap.put(type, sharesMap.get(type).add(additional));
        }
    }

    private List<InheritanceShareDto> convertToAmounts(
            Map<HeirType, InheritanceShareDto> dtoMap,
            Map<HeirType, BigDecimal> sharesMap,
            Map<HeirType, Integer> countMap,
            int origin,
            BigDecimal netEstate
    ) {
        List<InheritanceShareDto> finalShares = new ArrayList<>();

        if (origin == 0) {
            return finalShares;
        }

        BigDecimal shareValue = netEstate.divide(
                BigDecimal.valueOf(origin), 10, RoundingMode.HALF_UP
        );

        for (HeirType type : dtoMap.keySet()) {
            BigDecimal totalAmount = sharesMap.get(type)
                    .multiply(shareValue)
                    .setScale(2, RoundingMode.HALF_UP);

            int count = countMap.getOrDefault(type, 1);
            double amountPerPerson = totalAmount.divide(
                    BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP
            ).doubleValue();

            finalShares.add(dtoMap.get(type).withAmounts(
                    amountPerPerson, totalAmount.doubleValue()
            ));
        }

        return finalShares;
    }

    private FullInheritanceResponse createResponse(
            InheritanceCalculationRequest request,
            BigDecimal netEstate,
            List<InheritanceShareDto> finalShares,
            InheritanceCase c
    ) {
        // حساب مجموع المبالغ الموزعة
        double totalDistributed = finalShares.stream()
                .mapToDouble(InheritanceShareDto::totalAmount)
                .sum();

        // حساب الفرق (إن وجد)
        double difference = netEstate.doubleValue() - totalDistributed;

        // تقريب الفرق
        difference = Math.round(difference * 100.0) / 100.0;

        return new FullInheritanceResponse(
                arabicInheritanceTextService.generateText(request),
                request.totalEstate().doubleValue(),
                netEstate.doubleValue(),
                finalShares,
                difference
        );
    }

    private FullInheritanceResponse createSpecialCaseResponse(
            InheritanceCalculationRequest request,
            BigDecimal netEstate,
            InheritanceCase c
    ) {
        // إنشاء رد للحالات الخاصة
        List<InheritanceShareDto> specialShares = new ArrayList<>();

        if (c.mapSize() == 1) {
            // حالة وريث واحد فقط
            for (Map.Entry<HeirType, Integer> entry : c.getHeirs().entrySet()) {
                HeirType type = entry.getKey();
                int count = entry.getValue();
                double amountPerPerson = netEstate.doubleValue() / count;

                specialShares.add(new InheritanceShareDto(
                        type,
                        count,
                        amountPerPerson,
                        netEstate.doubleValue(),
                        ShareType.TAASIB,
                        null,
                        "يرث كل التركة لعدم وجود وارث آخر"
                ));
            }
        }

        return new FullInheritanceResponse(
                arabicInheritanceTextService.generateText(request),
                request.totalEstate().doubleValue(),
                netEstate.doubleValue(),
                specialShares,
                0.0
        );
    }

    // ==================== دوال مساعدة ====================

    private int getTotalAsabaUnits(List<HeirType> asabaList, InheritanceCase c) {
        int total = 0;
        for (HeirType type : asabaList) {
            total += calculateAsabaUnits(type, c);
        }
        return total;
    }

    private int calculateAsabaUnits(HeirType type, InheritanceCase c) {
        int count = c.count(type);
        int unit = type.getAsabaUnit();
        return count * unit;
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
        return a * (b / gcd(a, b));
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private void validateRequest(InheritanceCalculationRequest request) {
        if (request == null) {
            throw new InvalidInheritanceCaseException("Request must not be null");
        }
        if (request.heirs() == null || request.heirs().isEmpty()) {
            throw new InvalidInheritanceCaseException("Heirs must not be empty");
        }
        if (request.totalEstate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInheritanceCaseException("Estate must be positive");
        }
    }

    private void logCaseInfo(InheritanceCase c) {
        System.out.println("=== معلومات الحالة ===");
        System.out.println("حالة حجرية: " + (c.isHijriyyaCase() ? "نعم" : "لا"));
        System.out.println("حالة عمرية: " + (c.isUmariyyaCase() ? "نعم" : "لا"));
        System.out.println("حالة حمارية: " + (c.isHimariyyaCase() ? "نعم" : "لا"));
        System.out.println("عدد الورثة: " + c.totalHeirsCount());
        System.out.println("التركة الصافية: " + c.getNetEstate());
    }

    private void saveInheritanceProblem(FullInheritanceResponse response) {
        User currentUser = securityUtil.getCurrentUser();
        if (currentUser != null) {
            inheritanceProblemService.saveInheritanceProblem(response, currentUser);
        }
    }

    private boolean handleSpecialCases(InheritanceCase c) {
        // معالجة الحالات الخاصة التي تتطلب منطقاً مختلفاً

        // حالة: متوفى فقط مع زوج/زوجة
        if (c.mapSize() == 1 && c.hasSpouse()) {
            return true;
        }

        // حالة: متوفى فقط مع أم
        if (c.mapSize() == 1 && c.has(HeirType.MOTHER)) {
            return true;
        }

        // حالة: متوفى فقط مع أب
        if (c.mapSize() == 1 && c.has(HeirType.FATHER)) {
            return true;
        }

        return false;
    }

    private void adjustForRoundingErrors(
            Map<HeirType, BigDecimal> sharesMap,
            BigDecimal netEstate
    ) {
        // تصحيح أخطاء التقريب
        BigDecimal totalDistributed = sharesMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = netEstate.subtract(totalDistributed);

        if (difference.abs().compareTo(new BigDecimal("0.01")) > 0) {
            // توزيع الفرق على أكبر نصيب
            HeirType largestShareType = null;
            BigDecimal largestShare = BigDecimal.ZERO;

            for (Map.Entry<HeirType, BigDecimal> entry : sharesMap.entrySet()) {
                if (entry.getValue().compareTo(largestShare) > 0) {
                    largestShare = entry.getValue();
                    largestShareType = entry.getKey();
                }
            }

            if (largestShareType != null) {
                sharesMap.put(largestShareType, largestShare.add(difference));
            }
        }
    }
}