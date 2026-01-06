package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class FatherRule implements InheritanceRule {


    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.FATHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {

        int count = c.count(HeirType.FATHER);

        // وجود فرع وارث
        if (c.hasDescendant()) {

            // فرع وارث ذكر → سدس فقط
            if (c.hasMaleChild()) {
                return new InheritanceShareDto(
                        HeirType.FATHER,
                        count,
                        null,
                        null,
                        ShareType.FIXED,
                        FixedShare.SIXTH,
                        "يرث الأب السدس لوجود فرع وارث ذكر"
                );
            }

            // فرع وارث أنثى فقط → سدس + تعصيب
            return new InheritanceShareDto(
                    HeirType.FATHER,
                    count,
                    null,
                    null,
                    ShareType.MIXED,
                    FixedShare.SIXTH,
                    "يرث الأب السدس فرضًا والباقي تعصيبًا لوجود فرع وارث أنثى"
            );
        }

        // لا يوجد فرع وارث
        return new InheritanceShareDto(
                HeirType.FATHER,
                count,
                null,
                null,
                ShareType.TAASIB,
                null,
                "يرث الأب الباقي تعصيبًا لعدم وجود فرع وارث"
        );
    }
    }
