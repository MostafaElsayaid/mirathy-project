package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class DaughterRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.DAUGHTER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        int count = c.count(HeirType.DAUGHTER);

        if (count == 1 && !c.has(HeirType.SON)) {
            // بنت واحدة بدون ابن: نصف فرضًا
            return new InheritanceShareDto(
                    HeirType.DAUGHTER,
                    count,
                    null,
                    null,
                    ShareType.MIXED,
                    FixedShare.HALF,
                    "ترث النصف فرضًا والباقي تعصيبًا مع الأب أو الإخوة"
            );
        } else if (count >= 2 && !c.has(HeirType.SON)) {
            // بنتان أو أكثر بدون ابن: ثلثان فرضًا
            return new InheritanceShareDto(
                    HeirType.DAUGHTER,
                    count,
                    null,
                    null,
                    ShareType.MIXED,
                    FixedShare.TWO_THIRDS,
                    "ترث الثلثين فرضًا والباقي تعصيبًا مع الأب أو الإخوة"
            );
        } else {
            // مع وجود ابن: تعصيب بالغير
            return new InheritanceShareDto(
                    HeirType.DAUGHTER,
                    count,
                    null,
                    null,
                    ShareType.TAASIB,
                    null,
                    "ترث تعصيبًا مع الإخوة الذكور"
            );
        }
    }

}
