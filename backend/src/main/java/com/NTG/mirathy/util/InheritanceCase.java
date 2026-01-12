package com.NTG.mirathy.util;

import com.NTG.mirathy.Entity.Enum.HeirType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InheritanceCase {
    private final BigDecimal totalEstate;
    private final BigDecimal debts;
    private final BigDecimal will;
    private final Map<HeirType, Integer> heirs;


    public InheritanceCase(
            BigDecimal totalEstate,
            BigDecimal debts,
            BigDecimal will,
            Map<HeirType, Integer> heirs
    ) {
        this.totalEstate = totalEstate != null ? totalEstate : BigDecimal.ZERO;
        this.debts = debts != null ? debts : BigDecimal.ZERO;
        this.will = will != null ? will : BigDecimal.ZERO;
        this.heirs = heirs != null ? new HashMap<>(heirs) : new HashMap<>();
    }

    // ==================== دوال العد والتحقق ====================
    public int count(HeirType type) {
        return heirs.getOrDefault(type, 0);
    }

    public boolean has(HeirType type) {
        return count(type) > 0;
    }

    public boolean hasChildren() {
        return has(HeirType.SON) || has(HeirType.DAUGHTER);
    }

    public boolean hasMaleChild() {
        return has(HeirType.SON);
    }

    public boolean hasDescendant() {
        return has(HeirType.SON) || has(HeirType.DAUGHTER)
                || has(HeirType.DAUGHTER_OF_SON)
                || has(HeirType.SON_OF_SON);
    }

    public boolean hasSpouse() {
        return has(HeirType.HUSBAND) || has(HeirType.WIFE);
    }

    public int countMaleChildren() {
        return count(HeirType.SON);
    }

    public int countFemaleChildren() {
        return count(HeirType.DAUGHTER);
    }

    public boolean hasMaleDescendant() {
        return has(HeirType.SON) ||
                (has(HeirType.SON_OF_SON) && !has(HeirType.SON));
    }

    public boolean hasFemaleDescendantOnly() {
        return (has(HeirType.DAUGHTER) || has(HeirType.DAUGHTER_OF_SON)) &&
                !hasMaleDescendant();
    }

    // المسألة الحجرية: وجود أب مع فرع وارث ذكر
    public boolean isHijriyyaCase() {
        return has(HeirType.FATHER) && hasMaleDescendant();
    }

    public int countTotalChildren() {
        return countMaleChildren() + countFemaleChildren();
    }

    public boolean hasBrothersOrSisters() {
        return has(HeirType.FULL_BROTHER) || has(HeirType.FULL_SISTER)
                || has(HeirType.PATERNAL_BROTHER) || has(HeirType.PATERNAL_SISTER)
                || has(HeirType.MATERNAL_BROTHER) || has(HeirType.MATERNAL_SISTER);
    }

    public boolean hasSiblings() {
        return has(HeirType.FULL_BROTHER) || has(HeirType.FULL_SISTER) ||
                has(HeirType.PATERNAL_BROTHER) || has(HeirType.PATERNAL_SISTER);
    }

    public boolean hasMaternalSiblings() {
        return has(HeirType.MATERNAL_BROTHER) || has(HeirType.MATERNAL_SISTER);
    }

    public boolean hasPaternalSiblings() {
        return has(HeirType.PATERNAL_BROTHER) || has(HeirType.PATERNAL_SISTER);
    }

    public boolean hasFullSiblings() {
        return has(HeirType.FULL_BROTHER) || has(HeirType.FULL_SISTER);
    }

    // ==================== دوال الحساب ====================
    public BigDecimal getNetEstate() {
        BigDecimal net = totalEstate.subtract(debts).subtract(will);
        return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
    }

    public Map<HeirType, Integer> getHeirs() {
        return new HashMap<>(heirs);
    }

    public int countSiblings() {
        return count(HeirType.FULL_BROTHER) + count(HeirType.FULL_SISTER)
                + count(HeirType.PATERNAL_BROTHER) + count(HeirType.PATERNAL_SISTER)
                + count(HeirType.MATERNAL_BROTHER) + count(HeirType.MATERNAL_SISTER);
    }

    public int mapSize() {
        return heirs.size();
    }

    // ==================== دوال إضافية ====================

    // التحقق من المسألة العمرية
    public boolean isUmariyyaCase() {
        return has(HeirType.FATHER)
                && has(HeirType.MOTHER)
                && hasSpouse()
                && mapSize() == 3;
    }

    // التحقق من المسألة المشتركة (الجد مع الإخوة)
    public boolean isMusharakaCase() {
        return has(HeirType.GRANDFATHER) &&
                !has(HeirType.FATHER) &&
                (hasFullSiblings() || hasPaternalSiblings());
    }

    // التحقق من وجود فرع وارث ذكر يحجب الجد
    public boolean hasDescendantThatBlocksGrandfather() {
        return has(HeirType.SON) || has(HeirType.DAUGHTER);
    }

    // التحقق من وجود عصبات
    public boolean hasAsaba() {
        return heirs.keySet().stream()
                .anyMatch(type -> type.getAsabaUnit() > 0);
    }

    // الحصول على إجمالي عدد الورثة
    public int totalHeirsCount() {
        return heirs.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    // التحقق من وجود وريث معين بعدد معين
    public boolean hasAtLeast(HeirType type, int minimumCount) {
        return count(type) >= minimumCount;
    }

    // الحصول على قيمة التركة الصافية كـ double (لتسهيل الحسابات)
    public double getNetEstateAsDouble() {
        return getNetEstate().doubleValue();
    }

    @Override
    public String toString() {
        return "InheritanceCase{" +
                "totalEstate=" + totalEstate +
                ", debts=" + debts +
                ", will=" + will +
                ", heirs=" + heirs +
                '}';
    }

    // ==================== دوال التجميع ====================

    // جمع عدد الذكور من فئة معينة
    public int countMalesOfType(HeirType type) {
        // يمكن توسيع هذا المنطق ليشمل أنواعاً محددة
        if (type == HeirType.SON || type == HeirType.SON_OF_SON ||
                type == HeirType.FULL_BROTHER || type == HeirType.PATERNAL_BROTHER) {
            return count(type);
        }
        return 0;
    }

    // جمع عدد الإناث من فئة معينة
    public int countFemalesOfType(HeirType type) {
        if (type == HeirType.DAUGHTER || type == HeirType.DAUGHTER_OF_SON ||
                type == HeirType.FULL_SISTER || type == HeirType.PATERNAL_SISTER) {
            return count(type);
        }
        return 0;
    }
    public boolean isHimariyyaCase() {
        // المسألة الحمارية: زوج/زوجة + أم + إخوة لأم + إخوة أشقاء
        return hasSpouse() &&
                has(HeirType.MOTHER) &&
                hasMaternalSiblings() &&
                hasFullSiblings();
    }

    public int countMaternalSiblings() {
        return count(HeirType.MATERNAL_BROTHER) + count(HeirType.MATERNAL_SISTER);
    }

    public int countFullSiblings() {
        return count(HeirType.FULL_BROTHER) + count(HeirType.FULL_SISTER);
    }

    public boolean isSimpleUmariyya() {
        // المسألة العمرية البسيطة: زوج/زوجة + أب + أم فقط
        return hasSpouse() && has(HeirType.FATHER) && has(HeirType.MOTHER) && mapSize() == 3;
    }
    // التحقق من أن جميع الورثة من فئة معينة
    public boolean allHeirsAreOfTypes(HeirType... types) {
        if (heirs.isEmpty()) return false;

        for (HeirType heirType : heirs.keySet()) {
            boolean found = false;
            for (HeirType allowedType : types) {
                if (heirType == allowedType) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}