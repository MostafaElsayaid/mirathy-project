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

        int totalBrothersSisters = c.countSiblings();
        boolean hasMaternalSiblings = c.hasMaternalSiblings();
        boolean hasFullSiblings = c.hasFullSiblings();
        boolean hasSpouse = c.hasSpouse();

        // ====== المسألة الحمارية ======
        // زوج/زوجة + أم + إخوة لأم + إخوة أشقاء
        if (c.isHimariyyaCase()) {
            fixedShare = FixedShare.THIRD_REMAINDER_SHARED; // ثلث الباقي مشترك مع الإخوة لأم
            reason = "ترث الأم ثلث الباقي بعد نصيب الزوج/الزوجة وتتقاسمه مع الإخوة لأم (المسألة الحمارية)";
        }
        // ====== المسألة العمرية البسيطة ======
        else if (c.isSimpleUmariyya()) {
            fixedShare = FixedShare.THIRD_OF_REMAINDER;
            reason = "ترث الأم ثلث الباقي بعد نصيب الزوج/الزوجة (المسألة العمرية)";
        }
        // ====== حالة الأم مع إخوة لأم فقط (بدون إخوة أشقاء) ======
        else if (hasSpouse && hasMaternalSiblings && !hasFullSiblings) {
            fixedShare = FixedShare.THIRD; // تأخذ الثلث كاملاً
            reason = "ترث الأم الثلث كاملاً مع الإخوة لأم يشاركونها فيه";
        }
        // ====== السدس ======
        else if (c.hasDescendant() || totalBrothersSisters >= 2) {
            fixedShare = FixedShare.SIXTH;
            reason = "ترث الأم السدس عند وجود الفرع الوارث أو عند وجود أكثر من أخ";
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
}