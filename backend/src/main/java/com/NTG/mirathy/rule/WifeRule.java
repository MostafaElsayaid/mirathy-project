package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class WifeRule implements InheritanceRule {

    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.WIFE);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        HeirType heirType = HeirType.WIFE;
        int count = c.count(heirType);
        ShareType shareType = ShareType.FIXED;
        FixedShare fixedShare = c.hasDescendant() ? FixedShare.EIGHTH : FixedShare.QUARTER;
        String reason = "";

        if (c.hasDescendant()) {
            reason = "ترث الزوجة من زوجها الثمن إن كان له فرع وارث سواء كان منها أو من غيرها، والفرع الوارث هم: (( الأولاد بنون أو بنات، وأولاد الأبناء وإن نزلوا )) أما أولاد البنات فهم فروع غير وارثين. قال الله تعالى: ( فَإِنْ كَانَ لَكُمْ وَلَدٌ فَلَهُنَّ الثُّمُنُ مِمَّا تَرَكْتُمْ ..) .";
        } else {
            reason = "ترث الزوجة من زوجها الربع إن لم يكن له فرع وارث سواء كان منها أو من غيرها، والفرع الوارث هم: (( الأولاد بنون أو بنات، وأولاد الأبناء وإن نزلوا )) أما أولاد البنات فهم فروع غير وارثين. قال الله تعالى: (وَلَهُنَّ الرُّبُعُ مِمَّا تَرَكْتُمْ إِنْ لَمْ يَكُنْ لَكُمْ وَلَدٌ ..) .";
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