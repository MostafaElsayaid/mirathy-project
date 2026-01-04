package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.*;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Component;

@Component
public class FatherRule implements InheritanceRule {


        @Override
        public boolean canApply(InheritanceCase c) {
            return c.has(HeirType.FATHER);
        }

        @Override
        public InheritanceShareDto calculate(InheritanceCase c) {
            HeirType heirType = HeirType.FATHER;
            int count = c.count(heirType);
            ShareType shareType = null;
            FixedShare fixedShare = null;
            String reason = "";

            if (c.hasDescendant()) {
                shareType = ShareType.FIXED;
                fixedShare = FixedShare.SIXTH;
                reason = "يرث الأب السدس لوجود فرع وارث";
            }
            else {
                shareType = ShareType.TAASIB;
                reason = "يرث الأب الباقى تعصيباً فى حالة عدم الفرع الوارث المذكر والمؤنث . قال ﷺ ( ألحقوا الفرائض بأهلها فما بقى فهو لأولى رجل ذكر.)";
            }
            System.out.print(shareType + ""+ fixedShare + reason);
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
