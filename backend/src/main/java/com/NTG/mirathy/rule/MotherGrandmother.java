package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class MotherGrandmother implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.MOTHER)
                || c.has(HeirType.GRANDMOTHER_MATERNAL)
                || c.has(HeirType.GRANDMOTHER_PATERNAL);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {

        /* =========================
           أولًا: الأم
         ========================= */
        if (c.has(HeirType.MOTHER)) {

            boolean hasChildren = c.hasChildren();
            boolean hasMultipleSiblings = c.countSiblings() >= 2;

            FixedShare share;
            String reason;

            if (hasChildren || hasMultipleSiblings) {
                share = FixedShare.SIXTH;
                reason = "الأم ترث السدس لوجود فرع وارث أو جمع من الإخوة";
            } else {
                share = FixedShare.THIRD;
                reason = "الأم ترث الثلث لعدم وجود فرع وارث ولا جمع من الإخوة";
            }

            return new InheritanceShareDto(
                    HeirType.MOTHER,
                    1,
                    null,
                    null,
                    ShareType.FIXED,
                    share,
                    reason
            );
        }

        /* =========================
           ثانيًا: الجدة
         ========================= */

        // الجدة محجوبة بالأم
        if (c.has(HeirType.MOTHER)) {
            return null;
        }

        // الجدة لأب محجوبة بالأب
        if (c.has(HeirType.GRANDMOTHER_PATERNAL) && c.has(HeirType.FATHER)) {
            return null;
        }

        int grandmotherCount =
                c.count(HeirType.GRANDMOTHER_MATERNAL)
                        + c.count(HeirType.GRANDMOTHER_PATERNAL);

        if (grandmotherCount == 0) {
            return null;
        }

        return new InheritanceShareDto(
                HeirType.GRANDMOTHER_MATERNAL,
                grandmotherCount,
                null,
                null,
                ShareType.FIXED,
                FixedShare.SIXTH,
                "الجدة الصحيحة ترث السدس وتشترك فيه إن تعددت"
        );
    }
}
