package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DaughterRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.DAUGHTER) && !c.has(HeirType.SON);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {

        int daughters = c.count(HeirType.DAUGHTER);

        if (daughters == 1) {
            return new InheritanceShareDto(
                    null,
                    HeirType.DAUGHTER,
                    ShareType.FIXED,
                    FixedShare.HALF,
                    "للبنت النصف لانفرادها"
            );
        }

        return new InheritanceShareDto(
                null,
                HeirType.DAUGHTER,
                ShareType.FIXED,
                FixedShare.TWO_THIRDS,
                "للبنات الثلثان لاشتراكهن"
        );
    }
}
