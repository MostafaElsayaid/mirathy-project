package com.NTG.mirathy.Entity.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShareType {
    FIXED("فرضًا"),
    TAASIB("تعصيبًا"),
    RADD("ردًا"),
    Mahgub("محجوب"),
    MALE_DOUBLE_FEMALE("للذكر مثل حظ الأنثيين");

    private final String arabicName;

}
