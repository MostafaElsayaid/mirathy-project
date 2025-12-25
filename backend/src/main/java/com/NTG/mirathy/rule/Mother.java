package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;

public class Mother implements InheritanceRule {
    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.MOTHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.MOTHER;
        FixedShare fixedShare = null;
        String reason = "";
        ShareType shareType = null;

        if (c.hasDescendant() || hasMoreThanOneFullBrotherOrSister(c)
        ) {
            fixedShare = FixedShare.SIXTH;
            shareType = ShareType.FIXED;
            reason = "الأم ترث السدس ً" +
                    "عند وجود فرع وارث للمتوفى (ولد  أو ابن الابن) أو عند وجود جمع من الإخوة (اثنين أو أكثر)" +
                    "قوله تعالى: \"وَلِأَبَوَيْهِ لِكُلِّ وَاحِدٍ مِّنْهُمَا السُّدُسُ مِمَّا تَرَكَ إِن كَانَ لَهُ وَلَدٌ\" (النساء: 11)";
        } else if (c.has(HeirType.FATHER) && !c.hasDescendant()) {
            fixedShare = FixedShare.SIXTH;
            shareType = ShareType.FIXED;
            reason = "الأم ترث السدس مع الأب" +
                    "عند وجود الأب مع عدم وجود فرع وارث، حيث يأخذ الأب الباقي تعصيباً بعد فرض الأم" +
                    "قوله تعالى: \"فَإِن لَّمْ يَكُن لَّهُ وَلَدٌ وَوَرِثَهُ أَبَوَاهُ فَلِأُمِّهِ الثُّلُثُ فَإِن كَانَ لَهُ إِخْوَةٌ فَلِأُمِّهِ السُّدُسُ\" (النساء: 11)";
        } else {
            fixedShare = FixedShare.THIRD;
            shareType = ShareType.FIXED;
            reason = "الأم ترث الثلث ً" +
                    "عند عدم وجود فرع وارث للمتوفى (ولد أو ابن الابن) وعدم وجود جمع من الإخوة (أقل من اثنين)" +
                    "قوله تعالى: \"فَإِن لَّمْ يَكُن لَّهُ وَلَدٌ وَوَرِثَهُ أَبَوَاهُ فَلِأُمِّهِ الثُّلُثُ\" (النساء: 11)";

        }
        return new InheritanceShareDto(heirType,shareType,fixedShare,reason);
    }
    private boolean hasMoreThanOneFullBrotherOrSister(InheritanceCase c){
        int fullSiblingsCount = 0;
        fullSiblingsCount += c.count(HeirType.FULL_BROTHER);
        fullSiblingsCount += c.count(HeirType.FULL_SISTER);
        return fullSiblingsCount >= 2;
    }
}
