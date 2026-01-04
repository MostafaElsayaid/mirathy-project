package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class DaughterOfSonRule implements InheritanceRule {


    @Override
    public boolean canApply(InheritanceCase c) {
        // لا تنطبق إذا يوجد ابن صلب
        if (c.has(HeirType.SON)) return false;

        // لا تنطبق إذا يوجد ابنتان صليبيتان فأكثر يستوفين الثلثين
        int daughterCount = c.count(HeirType.DAUGHTER);
        if (daughterCount >= 2) {
            // تحقق: هل الابنتان يأخذان الثلثين بالكامل؟
            // إذا كان معهما بنت ابن، قد تكمل الثلثين
            return true; // نترك التفاصيل للدالة calculate
        }

        return c.has(HeirType.DAUGHTER_OF_SON);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.DAUGHTER_OF_SON;
        int count = c.count(heirType);
        ShareType shareType = null;
        FixedShare fixedShare = null;
        String reason = "";

        int daughterCount = c.count(HeirType.DAUGHTER);

        // 1. مع ابن الابن: تعصيب
        if (c.has(HeirType.SON_OF_SON)) {
            shareType = ShareType.TAASIB;
            reason = "بنت الابن ترث تعصيبًا مع ابن الابن للذكر مثل حظ الأنثيين لقوله تعالى (يُوصِيكُمُ اللَّهُ فِي أَوْلادِكُمْ لِلذَّكَرِ مِثْلُ حَظِّ الأُنثَيَيْنِ) ،ولا فرق بين أن يكون ابن الإبن اخ لبنت الإبن أوابن عمها";
        }
        // 2. مع بنت واحدة: تكملة الثلثين
        else if (daughterCount == 1 && count >= 1) {
            shareType = ShareType.FIXED;
            fixedShare = FixedShare.SIXTH;
            reason = "بنت الإبن مع البنت الصلبيه الواحده فى عدم الإبن الصلبى وابن الإبن الذى فى درجتها - يعصبها - وابن الإبن الأعلى منها درجة - يحجبها - ترث السدس تكملة للثلثين تنفرد به الواحده وتشترك فيه الأكثر من واحده لقول ابن مسعود - رضى الله عنه - فى مسألة بنت وبنت ابن وأخت شقيقة (أقضى فيها بقضاء رسول الله للبنت النصف،ولبنت الإبن السدس تكملة للثلثين ،وللأخت الشقية الباقى )";
        }
        // 3. بدون بنت صلبية
        else if (daughterCount == 0) {
            if (count == 1) {
                shareType = ShareType.FIXED;
                fixedShare = FixedShare.HALF;
                reason = "بنت الابن ترث النصف إذا انفردت";
            } else if (count >= 2) {
                shareType = ShareType.FIXED;
                fixedShare = FixedShare.TWO_THIRDS;
                reason = "لبنات الابن الثلثان لاشتراكهن";
            }
        }
        // 4. مع ابنتين صليبيتين: تحجب إذا استوفتا الثلثين
        else if (daughterCount >= 2) {
            // هنا قد تحجب أو تأخذ البقية
            shareType = ShareType.TAASIB;
            reason = "بنت الابن ترث بالتعصيب مع البنات الصليبيات";
        }

        if (shareType == null) {
            return null;
        }

        return new InheritanceShareDto(
                heirType, count, null, null, shareType, fixedShare, reason
        );
    }
}