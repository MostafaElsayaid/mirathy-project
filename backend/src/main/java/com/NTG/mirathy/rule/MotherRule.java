package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
public class MotherRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.MOTHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {

        if (c.hasDescendant() || c.hasBrothersOrSisters()) {
            return new InheritanceShareDto(
                    null,
                    HeirType.MOTHER,
                    ShareType.FIXED,
                    FixedShare.SIXTH,
                    "الأم ترث السدس لوجود فرع وارث أو جمع من الإخوة"
            );
        }

        return new InheritanceShareDto(
                null,
                HeirType.MOTHER,
                ShareType.FIXED,
                FixedShare.THIRD,
                "الأم ترث الثلث لعدم وجود فرع وارث ولا إخوة"
        );
    }
}

