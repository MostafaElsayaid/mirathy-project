package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class GrandmotherMaternalRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        if (!c.has(HeirType.GRANDMOTHER_MATERNAL)) return false;
        return !c.has(HeirType.MOTHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.GRANDMOTHER_MATERNAL;
        int count = c.count(heirType);
        ShareType shareType = ShareType.FIXED;
        FixedShare fixedShare = FixedShare.SIXTH;
        String reason = "";

        if (c.has(HeirType.GRANDMOTHER_PATERNAL)) {
            reason = "الجدة للأم تشترك مع الجدة للأب في السدس";
        } else {
            reason = "الجدة للأم ترث السدس لعدم وجود الأم";
        }

        return new InheritanceShareDto(
                heirType,
                count,
                null,
                null,
                shareType,
                fixedShare,
                reason
        );
    }
}