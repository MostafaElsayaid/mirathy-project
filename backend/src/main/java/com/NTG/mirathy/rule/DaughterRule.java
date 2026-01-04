package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class DaughterRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        // تنطبق إذا فيه بنت واحدة أو أكثر
        return c.has(HeirType.DAUGHTER);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.DAUGHTER;
        int count = c.count(heirType);
        ShareType shareType;
        FixedShare fixedShare = null;
        String reason = "";

        if (c.has(HeirType.SON)) {
            // تعصيب مع الابن
            shareType = ShareType.TAASIB;
            reason = "يرث الأبناء الذكور والإناث معا تعصيبا للذكر مثل حظ الأنثيين لقوله تعالى (يُوصِيكُمُ اللَّهُ فِي أَوْلادِكُمْ لِلذَّكَرِ مِثْلُ حَظِّ الأُنثَيَيْنِ)";
        } else {
            // بدون ابن → فرض ثابت
            shareType = ShareType.FIXED;
            if (count == 1) {
                fixedShare = FixedShare.HALF;
                reason = "ترث البنت الواحدة النصف إذا لم يكن لها أخ يعصبها. لقوله تعالى (يُوصِيكُمُ اللَّهُ فِي أَوْلادِكُمْ لِلذَّكَرِ مِثْلُ حَظِّ الأُنثَيَيْنِ فَإِنْ كُنَّ نِسَاءً فَوْقَ اثْنَتَيْنِ فَلَهُنَّ ثُلُثَا مَا تَرَكَ وَإِنْ كَانَتْ وَاحِدَةً فَلَهَا النِّصْفُ)";
            } else {
                fixedShare = FixedShare.TWO_THIRDS;
                reason = "ترث البنتين فأكثر الثلثين إذا لم يكن لهن أخ يعصبهن. لقوله تعالى (يُوصِيكُمُ اللَّهُ فِي أَوْلادِكُمْ لِلذَّكَرِ مِثْلُ حَظِّ الأُنثَيَيْنِ فَإِنْ كُنَّ نِسَاءً فَوْقَ اثْنَتَيْنِ فَلَهُنَّ ثُلُثَا مَا تَرَكَ وَإِنْ كَانَتْ وَاحِدَةً فَلَهَا النِّصْفُ) ولأنه ﷺ أعطى الثلثين لبنتى سعد بن الربيع، و الاثنتين لهما حكم فوق اثنتين لأن القرآن أيضا ذكر حُكم الأختين أن لهما الثلثين (فَإِن كَانَتَا اثْنَتَيْنِ فَلَهُمَا الثُّلُثَانِ مِمَّا تَرَكَ) فيتم حمل البنتين على الأختين ، وكذلك حمل الأكثر من أختين على قوله فى البنات (فَوْقَ اثْنَتَيْنِ).";
            }
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
}
