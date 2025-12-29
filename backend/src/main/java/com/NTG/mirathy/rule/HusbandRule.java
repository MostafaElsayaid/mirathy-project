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
        BigDecimal netEstate = c.getNetEstate();
        double amount;
        FixedShare fixedShare;
        String reason;

        if (c.hasChildren() || c.hasDescendant()) {
            // الزوج له الربع عند وجود الفرع الوارث
            amount = netEstate.multiply(BigDecimal.valueOf(1.0/4.0)).doubleValue();
            fixedShare = FixedShare.QUARTER;
            reason = "للزوج الربع لوجود الفرع الوارث";
        } else {
            // الزوج له النصف عند عدم وجود الفرع الوارث
            amount = netEstate.multiply(BigDecimal.valueOf(1.0/2.0)).doubleValue();
            fixedShare = FixedShare.HALF;
            reason = "للزوج النصف لعدم وجود الفرع الوارث";
        }

        return new  InheritanceShareDto(
                amount,
                HeirType.HUSBAND,
                ShareType.FIXED,
                fixedShare,
                reason
        );
    }
}