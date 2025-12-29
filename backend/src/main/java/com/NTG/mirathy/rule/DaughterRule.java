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
        return c.has(HeirType.DAUGHTER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        BigDecimal netEstate = c.getNetEstate();
        int daughterCount = c.count(HeirType.DAUGHTER);
        int sonCount = c.count(HeirType.SON);
        Double amount;
        FixedShare fixedShare;
        String reason;

        if (sonCount == 0) {
            // بنات فقط
            if (daughterCount == 1) {
                // بنت واحدة: النصف
                amount = netEstate.multiply(BigDecimal.valueOf(1.0/2.0)).doubleValue();
                fixedShare = FixedShare.HALF;
                reason = "للأبنة النصف لانفرادها";
            } else {
                // بنتان فأكثر: الثلثين
                amount = netEstate.multiply(BigDecimal.valueOf(2.0/3.0)).doubleValue();
                fixedShare = FixedShare.TWO_THIRDS;
                reason = "للأبناء الثلثين لتعددهن";
                // تقسيم المبلغ على عدد البنات
                amount = amount / daughterCount;
            }
        } else {
            // مع أبناء ذكور: تأخذ نصف نصيب الذكر (يتم حسابها مع التعصيب)
            amount = 0.0;
            fixedShare = null;
            reason = "للأبنة نصف نصيب الذكر مع الإخوة الذكور";
        }

        return new  InheritanceShareDto(
                amount,
                HeirType.DAUGHTER,
                ShareType.FIXED,
                fixedShare,
                reason
        );
    }
}