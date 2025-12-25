package com.NTG.mirathy.Entity.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HeirType {

    // ====== الأزواج ======
    HUSBAND("زوج", 1),
    WIFE("زوجة", 4),

    // ====== الأصول ======
    FATHER("أب", 1),
    MOTHER("أم", 1),
    GRANDFATHER("جد", 1),
    GRANDMOTHER_PATERNAL("جدة لأب", 1),
    GRANDMOTHER_MATERNAL("جدة لأم", 1),

    // ====== الفروع ======
    SON("ابن", null),
    DAUGHTER("بنت", null),
    SON_OF_SON("ابن الابن", null),
    DAUGHTER_OF_SON("بنت الابن", null),

    // ====== الإخوة الأشقاء ======
    FULL_BROTHER("أخ شقيق", null),
    FULL_SISTER("أخت شقيقة", null),

    // ====== الإخوة لأب ======
    PATERNAL_BROTHER("أخ لأب", null),
    PATERNAL_SISTER("أخت لأب", null),

    // ====== الإخوة لأم ======
    MATERNAL_BROTHER("أخ لأم", null),
    MATERNAL_SISTER("أخت لأم", null),

    // ====== أبناء الإخوة ======
    SON_OF_FULL_BROTHER("ابن الأخ الشقيق", null),
    SON_OF_PATERNAL_BROTHER("ابن الأخ لأب", null),

    // ====== الأعمام ======
    FULL_UNCLE("عم شقيق", null),
    PATERNAL_UNCLE("عم لأب", null),

    // ====== أبناء الأعمام ======
    SON_OF_FULL_UNCLE("ابن العم الشقيق", null),
    SON_OF_PATERNAL_UNCLE("ابن العم لأب", null);

    private final String arabicName;
    private final Integer MAX_ALLOWED;

}
