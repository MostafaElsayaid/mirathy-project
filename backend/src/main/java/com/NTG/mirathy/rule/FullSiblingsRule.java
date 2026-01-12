package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;

public class FullSiblingsRule implements InheritanceRule{
    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.FULL_BROTHER) || c.has(HeirType.FULL_SISTER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        int count = c.count(HeirType.FULL_BROTHER) + c.count(HeirType.FULL_SISTER);
        boolean hasMother = c.has(HeirType.MOTHER);
        boolean hasSpouse = c.hasSpouse();
        boolean hasMaternalSiblings = c.hasMaternalSiblings();

        // ====== المسألة الحمارية ======
        // الإخوة الأشقاء محجوبون حجب حرمان في المسألة الحمارية
        if (hasMother && hasSpouse && hasMaternalSiblings) {
            return null; // لا يرثون شيئاً
        }

        // ====== التحقق من الحجب ======
        if (c.hasDescendant() || c.has(HeirType.FATHER) || c.has(HeirType.GRANDFATHER)) {
            // محجوبون بفرع وارث أو أب أو جد
            return null;
        }

        // ====== حالة التعصيب ======
        if (!c.hasMaleDescendant() && !c.has(HeirType.FATHER) && !c.has(HeirType.GRANDFATHER)) {
            return new InheritanceShareDto(
                    HeirType.FULL_BROTHER,
                    count,
                    null,
                    null,
                    ShareType.TAASIB,
                    null,
                    "يرث الإخوة الأشقاء الباقي تعصيباً"
            );
        }

        return null;
    }
}
