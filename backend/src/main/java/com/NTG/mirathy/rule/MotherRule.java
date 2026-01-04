package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class MotherRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.MOTHER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {

        HeirType heirType = HeirType.MOTHER;
        int count = c.count(heirType);
        ShareType shareType = ShareType.FIXED;
        FixedShare fixedShare;
        String reason;

        int totalBrothersSisters =
                c.count(HeirType.FULL_BROTHER) +
                        c.count(HeirType.FULL_SISTER) +
                        c.count(HeirType.PATERNAL_BROTHER) +
                        c.count(HeirType.PATERNAL_SISTER);

        // ====== العمريّة ======
        if (isUmariyya(c)) {
            fixedShare = FixedShare.THIRD_OF_REMAINDER;
            reason = "ترث الأم ثلث الباقي في المسألة العمرية (الغراوية)";
        }

        // ====== السدس ======
        else if (c.hasDescendant() || totalBrothersSisters >= 2) {
            fixedShare = FixedShare.SIXTH;
            reason = "ترث الأم السدس عند وجود الفرع الوارث مذكراً كان أو مؤنثاً، أو عند وجود اكثر من أخ. قال الله تعالى: (...وَلأَبَوَيْهِ لِكُلِّ وَاحِدٍ مِنْهُمَا السُّدُسُ مِمَّا تَرَكَ إِنْ كَانَ لَهُ وَلَدٌ فَإِنْ لَمْ يَكُنْ لَهُ وَلَدٌ وَوَرِثَهُ أَبَوَاهُ فَلأُمِّهِ الثُّلُثُ فَإِنْ كَانَ لَهُ إِخْوَةٌ فَلأُمِّهِ السُّدُسُ ..)";
        }

        // ====== الثلث ======
        else {
            fixedShare = FixedShare.THIRD;
            reason = "ترث الأم الثلث لعدم وجود فرع وارث ولا إخوة";
        }

        return new InheritanceShareDto(
                heirType,
                count,
                null,
                null,
                shareType,
                fixedShare,
                reason
        );
    }

    private boolean isUmariyya(InheritanceCase c) {
        return c.has(HeirType.FATHER)
                && c.has(HeirType.MOTHER)
                && (c.has(HeirType.WIFE) || c.has(HeirType.HUSBAND))
               && c.mapSize()==3;

    }
}