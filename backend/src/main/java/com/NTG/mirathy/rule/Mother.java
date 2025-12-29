package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
public class Mother implements InheritanceRule {
    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.MOTHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        BigDecimal netState = c.getNetEstate();
        double amount ;
        FixedShare share;
        String reason;

        if (c.hasDescendant() || hasMoreThanOneFullBrotherOrSister(c)
        ) {
            amount = netState.doubleValue() / 6;
            share = FixedShare.SIXTH;
            reason = "الأم ترث السدس ً" +
                    "عند وجود فرع وارث للمتوفى (ولد  أو ابن الابن) أو عند وجود جمع من الإخوة (اثنين أو أكثر)" +
                    "قوله تعالى: \"وَلِأَبَوَيْهِ لِكُلِّ وَاحِدٍ مِّنْهُمَا السُّدُسُ مِمَّا تَرَكَ إِن كَانَ لَهُ وَلَدٌ\" (النساء: 11)";
        } else if (c.has(HeirType.FATHER) && !c.hasDescendant()) {
            amount = netState.doubleValue() / 6;
            share = FixedShare.SIXTH;
            reason = "الأم ترث السدس مع الأب" +
                    "عند وجود الأب مع عدم وجود فرع وارث، حيث يأخذ الأب الباقي تعصيباً بعد فرض الأم" +
                    "قوله تعالى: \"فَإِن لَّمْ يَكُن لَّهُ وَلَدٌ وَوَرِثَهُ أَبَوَاهُ فَلِأُمِّهِ الثُّلُثُ فَإِن كَانَ لَهُ إِخْوَةٌ فَلِأُمِّهِ السُّدُسُ\" (النساء: 11)";
        } else {
            amount = netState.doubleValue() / 3;
            share = FixedShare.THIRD;
            reason = "الأم ترث الثلث ً" +
                    "عند عدم وجود فرع وارث للمتوفى (ولد أو ابن الابن) وعدم وجود جمع من الإخوة (أقل من اثنين)" +
                    "قوله تعالى: \"فَإِن لَّمْ يَكُن لَّهُ وَلَدٌ وَوَرِثَهُ أَبَوَاهُ فَلِأُمِّهِ الثُّلُثُ\" (النساء: 11)";

        }
        return new InheritanceShareDto(
                amount,
                HeirType.MOTHER,
                ShareType.FIXED,
                share,
                reason
               );
    }
    private boolean hasMoreThanOneFullBrotherOrSister(InheritanceCase c){
        int fullSiblingsCount = 0;
        fullSiblingsCount += c.count(HeirType.FULL_BROTHER);
        fullSiblingsCount += c.count(HeirType.FULL_SISTER);
        return fullSiblingsCount >= 2;
    }
}
