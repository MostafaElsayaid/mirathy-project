package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class GrandmotherPaternalRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        if (!c.has(HeirType.GRANDMOTHER_PATERNAL)) return false;
        return !c.has(HeirType.FATHER) && !c.has(HeirType.GRANDFATHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.GRANDMOTHER_PATERNAL;
        int count = c.count(heirType);
        ShareType shareType = ShareType.FIXED;
        FixedShare fixedShare = FixedShare.SIXTH;
        String reason = "";

        if (c.has(HeirType.GRANDMOTHER_MATERNAL)) {
            reason = "الجدة للأب تشترك مع الجدة للأم في السدس";
        } else {
            reason = "الجدة للأب ترث السدس لعدم وجود الأب أو الجد";
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