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

        boolean hasMaleDescendant = c.hasMaleDescendant();

        if (c.hasDescendant()) {
            if (hasMaleDescendant) {
                return new InheritanceShareDto(
                        HeirType.FATHER,
                        count,
                        null,
                        null,
                        ShareType.FIXED,
                        FixedShare.SIXTH,
                        "يرث الأب السدس فقط لوجود فرع وارث ذكر (المسألة الحجرية)"
                );
            } else if (c.hasFemaleDescendantOnly()) {
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
        }

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

