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
        ShareType shareType;
        FixedShare fixedShare = null;
        String reason;

        boolean hasMaleDescendant = c.hasMaleDescendant();
        boolean hasDescendant = c.hasDescendant();

        if (hasDescendant) {
            if (hasMaleDescendant) {
                shareType = ShareType.FIXED;
                fixedShare = FixedShare.SIXTH;
                reason = "الجد يرث السدس فقط لوجود فرع وارث ذكر (المسألة الحجرية)";
            } else {
                shareType = ShareType.MIXED;
                fixedShare = FixedShare.SIXTH;
                reason = "الجد يرث السدس فرضًا والباقي تعصيبًا لوجود فرع وارث أنثى";
            }
        } else {
            shareType = ShareType.TAASIB;
            reason = "الجد يرث الباقي تعصيبًا لعدم وجود فرع وارث";
        }

        return new InheritanceShareDto(
                heirType, count, null, null, shareType, fixedShare, reason
        );
    }
}