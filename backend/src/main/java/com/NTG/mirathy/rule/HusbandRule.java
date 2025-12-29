package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;

import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HusbandRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.HUSBAND);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        FixedShare share = c.hasDescendant()
                ? FixedShare.QUARTER
                : FixedShare.HALF;

        return new InheritanceShareDto(
                null,
                HeirType.HUSBAND,
                ShareType.FIXED,
                share,
                c.hasDescendant()
                        ? "للزوج الربع لوجود فرع وارث"
                        : "للزوج النصف لعدم وجود فرع وارث"
        );
    }
}
