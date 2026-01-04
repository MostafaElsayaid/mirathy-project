package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class GrandfatherRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.GRANDFATHER) && !c.has(HeirType.FATHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.GRANDFATHER;
        int count = c.count(heirType);
        ShareType shareType = null;
        FixedShare fixedShare = null;
        String reason = "";

        boolean hasChildren = c.hasChildren();
        boolean hasSiblings = c.has(HeirType.FULL_BROTHER) ||
                c.has(HeirType.FULL_SISTER) ||
                c.has(HeirType.PATERNAL_BROTHER) ||
                c.has(HeirType.PATERNAL_SISTER);

        if (hasChildren) {
            // مع الفرع الوارث: سدس فقط
            shareType = ShareType.FIXED;
            fixedShare = FixedShare.SIXTH;
            reason = "الجد يرث السدس لوجود فرع وارث";
        } else if (hasSiblings) {
            // مع الإخوة: قد يرث معهم أو يحجبهم حسب المسألة
            shareType = ShareType.TAASIB;
            reason = "الجد يرث تعصيبًا مع الإخوة في بعض الحالات";
        } else {
            // بدون فرع ولا إخوة: تعصيب كامل
            shareType = ShareType.TAASIB;
            reason = "الجد يرث تعصيبًا لعدم وجود فرع وارث";
        }

        return new InheritanceShareDto(
                heirType, count, null, null, shareType, fixedShare, reason
        );
    }
}