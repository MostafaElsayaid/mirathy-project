package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class HusbandRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.HUSBAND);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.HUSBAND;
        int count = c.count(heirType);
        ShareType shareType = ShareType.FIXED;
        FixedShare fixedShare = c.hasDescendant() ? FixedShare.QUARTER : FixedShare.HALF;
        String reason = "";

        if (c.hasDescendant()) {
            reason = "يرث الزوج من زوجته الربع إن كان لها فرع وارث سواء كان منه أو من غيره، والفرع الوارث هم: (( الأولاد بنون أو بنات، وأولاد الأبناء وإن نزلوا )) أما أولاد البنات فهم فروع غير وارثين. قال الله تعالى: (وَلَكُمْ نِصْفُ مَا تَرَكَ أَزْوَاجُكُمْ إِنْ لَمْ يَكُنْ لَهُنَّ وَلَدٌ فَإِنْ كَانَ لَهُنَّ وَلَدٌ فَلَكُمْ الرُّبُعُ مِمَّا تَرَكْنَ ..) .";
        } else {
            reason = "للزوج النصف لعدم وجود فرع وارث";
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