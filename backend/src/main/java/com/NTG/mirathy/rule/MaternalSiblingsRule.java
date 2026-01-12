package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;

public class MaternalSiblingsRule implements InheritanceRule{
    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.MATERNAL_BROTHER) || c.has(HeirType.MATERNAL_SISTER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        int count = c.count(HeirType.MATERNAL_BROTHER) + c.count(HeirType.MATERNAL_SISTER);
        boolean hasMother = c.has(HeirType.MOTHER);
        boolean hasSpouse = c.hasSpouse();
        boolean hasFullSiblings = c.hasFullSiblings();

        // ====== المسألة الحمارية ======
        if (hasMother && hasSpouse && hasFullSiblings) {
            // في المسألة الحمارية: الإخوة لأم يشاركون الأم في ثلث الباقي
            return new InheritanceShareDto(
                    HeirType.MATERNAL_BROTHER, // نستخدم نوعاً يمثل الإخوة لأم
                    count,
                    null,
                    null,
                    ShareType.FIXED,
                    FixedShare.THIRD_REMAINDER_SHARED,
                    "الإخوة لأم يشاركون الأم في ثلث الباقي بعد نصيب الزوج/الزوجة (المسألة الحمارية)"
            );
        }

        // ====== الحالة العادية ======
        // إخوة لأم فقط مع زوج/زوجة
        if (hasMother && hasSpouse && !hasFullSiblings) {
            return new InheritanceShareDto(
                    HeirType.MATERNAL_BROTHER,
                    count,
                    null,
                    null,
                    ShareType.FIXED,
                    FixedShare.THIRD,
                    "يرث الإخوة لأم الثلث مع الأم"
            );
        }

        // ====== إخوة لأم فقط (بدون أم ولا زوج) ======
        if (!hasMother && !hasSpouse) {
            return new InheritanceShareDto(
                    HeirType.MATERNAL_BROTHER,
                    count,
                    null,
                    null,
                    ShareType.FIXED,
                    FixedShare.THIRD,
                    "يرث الإخوة لأم الثلث للذكر مثل حظ الأنثيين"
            );
        }

        // ====== حالات أخرى ======
        return null; // محجوبون في حالات أخرى
    }
}
