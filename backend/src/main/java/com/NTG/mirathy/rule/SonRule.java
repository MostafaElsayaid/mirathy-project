package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class SonRule implements InheritanceRule{

    @Override
    public boolean canApply(InheritanceCase c) {
        // القاعدة تنطبق فقط إذا هناك أبناء أو بنات
        return c.hasMaleChild() || c.countFemaleChildren() > 0;
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        int sons = c.countMaleChildren();
        int daughters = c.countFemaleChildren();

        if (sons + daughters == 0) return null;

        int totalUnits = sons * 2 + daughters;

        // هذه الطريقة ترجع الابن فقط، البنات ترجع بنفس الطريقة في السهم نفسه مع reason موحد
        // في السيرفيس الرئيسي، سيتم حساب المبالغ الفعلية بعد معرفة remaining
        return new InheritanceShareDto(
                null,  // سيتم حساب المبلغ لاحقًا
                null,  // الورثة سيملأوا في السيرفيس حسب النوع
                ShareType.TAASIB,
                null,
                "تعصيب: للذكر مثل حظ الأنثيين"
        );
    }

    // لو عايز، ممكن تضيف method تحسب كل الابناء والبنات مباشرة مع الأسهم
    public List<InheritanceShareDto> calculateShares(InheritanceCase c, int remaining) {
        List<InheritanceShareDto> shares = new ArrayList<>();
        int sons = c.countMaleChildren();
        int daughters = c.countFemaleChildren();

        if (sons + daughters == 0) return shares;

        int totalUnits = sons * 2 + daughters;

        if (sons > 0) {
            shares.add(new InheritanceShareDto(
                    null,
                    HeirType.SON,
                    ShareType.TAASIB,
                    null,
                    "تعصيب: للذكر مثل حظ الأنثيين"
            ));
        }

        if (daughters > 0) {
            shares.add(new InheritanceShareDto(
                    null,
                    HeirType.DAUGHTER,
                    ShareType.TAASIB,
                    null,
                    "تعصيب: للذكر مثل حظ الأنثيين"
            ));
        }

        return shares;
    }
}
