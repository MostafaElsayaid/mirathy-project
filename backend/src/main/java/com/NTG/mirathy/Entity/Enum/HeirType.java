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
    Full_SIBLINGS("إخوة أشقاء" , null),

    // ====== الإخوة لأب ======
    PATERNAL_BROTHER("أخ لأب", null),
    PATERNAL_SISTER("أخت لأب", null),

    // ====== الإخوة لأم ======
    MATERNAL_BROTHER("أخ لأم", null),
    MATERNAL_SISTER("أخت لأم", null),
    MATERNAL_SIBLINGS("إخوة لأم" , null),

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

        //منطق التعصيب

//    public boolean isTaasib() {
//        return switch (this) {
//
//            case SON, DAUGHTER,
//                 SON_OF_SON, DAUGHTER_OF_SON,
//
//
//                 FULL_BROTHER, FULL_SISTER,
//                 PATERNAL_BROTHER, PATERNAL_SISTER,
//
//                 SON_OF_FULL_BROTHER,
//                 SON_OF_PATERNAL_BROTHER,
//                 FULL_UNCLE,
//                 PATERNAL_UNCLE,
//                 SON_OF_FULL_UNCLE,
//                 SON_OF_PATERNAL_UNCLE
//                    -> true;
//
//            default -> false;
//        };
//    }

    public int getAsabaUnit(HeirType type) {
        return switch (type) {

            // الذكور = 2
            case SON,
                 SON_OF_SON,
                 FULL_BROTHER,
                 PATERNAL_BROTHER,
                 SON_OF_FULL_BROTHER,
                 SON_OF_PATERNAL_BROTHER,
                 FULL_UNCLE,
                 PATERNAL_UNCLE,
                 SON_OF_FULL_UNCLE,
                 SON_OF_PATERNAL_UNCLE
                    -> 2;

            // الإناث = 1
            case DAUGHTER,
                 DAUGHTER_OF_SON,
                 FULL_SISTER,
                 PATERNAL_SISTER
                    -> 1;

            // عصبة بالنفس بوحدة واحدة
            case FATHER,
                 GRANDFATHER
                    -> 1;

            default -> 0;
        };
    }



    public int getUnit() {
        return switch (this) {

            case SON,
                 SON_OF_SON,
                 FULL_BROTHER,
                 PATERNAL_BROTHER,
                 SON_OF_FULL_BROTHER,
                 SON_OF_PATERNAL_BROTHER,
                 FULL_UNCLE,
                 PATERNAL_UNCLE,
                 SON_OF_FULL_UNCLE,
                 SON_OF_PATERNAL_UNCLE
                    -> 2;

            case DAUGHTER,
                 DAUGHTER_OF_SON,
                 FULL_SISTER,
                 PATERNAL_SISTER
                    -> 1;

            default -> 0;
        };
    }
    public boolean isSpouse() {
        return this == HUSBAND || this == WIFE;
    }
}
