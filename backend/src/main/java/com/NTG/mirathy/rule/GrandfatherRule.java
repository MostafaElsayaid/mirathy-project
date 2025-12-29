package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class GrandfatherRule implements InheritanceRule{


    @Override
    public boolean canApply(InheritanceCase c) {

        // لو الجد مش موجود
        if (!c.has(HeirType.GRANDFATHER)) return false;

        // الأب يحجب الجد
        if (c.has(HeirType.FATHER)) return false;

        return true;
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {

        // وجود فرع وارث → السدس
        if (c.hasChildren()) {
            return new InheritanceShareDto(
                    null,
                    HeirType.GRANDFATHER,
                    ShareType.FIXED,
                    FixedShare.SIXTH,
                    "الجد يرث السدس لوجود فرع وارث"
            );
        }

        // لا يوجد فرع وارث → تعصيب
        return new InheritanceShareDto(
                null,
                HeirType.GRANDFATHER,
                ShareType.TAASIB,
                null,
                "الجد يرث تعصيبًا لعدم وجود فرع وارث"
        );
    }
}
