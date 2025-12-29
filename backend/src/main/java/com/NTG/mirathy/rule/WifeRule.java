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
        BigDecimal netEstate = c.getNetEstate();
        int wifeCount = c.count(HeirType.WIFE);
        Double amount;
        FixedShare fixedShare;
        String reason;

        if (c.hasChildren() || c.hasDescendant()) {
            // الزوجة/الزوجات لهن الثمن عند وجود الفرع الوارث
            amount = netEstate.multiply(BigDecimal.valueOf(1.0/8.0)).doubleValue();
            fixedShare = FixedShare.EIGHTH;
            reason = wifeCount > 1 ?
                    "للزوجات الثمن لوجود الفرع الوارث" :
                    "للزوجة الثمن لوجود الفرع الوارث";
        } else {
            // الزوجة/الزوجات لهن الربع عند عدم وجود الفرع الوارث
            amount = netEstate.multiply(BigDecimal.valueOf(1.0/4.0)).doubleValue();
            fixedShare = FixedShare.QUARTER;
            reason = wifeCount > 1 ?
                    "للزوجات الربع لعدم وجود الفرع الوارث" :
                    "للزوجة الربع لعدم وجود الفرع الوارث";
        }

        // تقسيم المبلغ على عدد الزوجات إذا كن أكثر من واحدة
        if (wifeCount > 1) {
            amount = amount / wifeCount;
        }

        return new InheritanceShareDto(
                amount,
                HeirType.WIFE,
                ShareType.FIXED,
                fixedShare,
                reason
        );
    }
}