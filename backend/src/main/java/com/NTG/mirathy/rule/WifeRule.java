package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WifeRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.WIFE);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        FixedShare share = c.hasDescendant()
                ? FixedShare.EIGHTH
                : FixedShare.QUARTER;

        return new InheritanceShareDto(
                null,
                HeirType.WIFE,
                ShareType.FIXED,
                share,
                c.hasDescendant()
                        ? "للزوجات الثمن لوجود فرع وارث"
                        : "للزوجات الربع لعدم وجود فرع وارث"
        );
    }
}
