package com.NTG.mirathy.Entity.Enum;

import com.NTG.mirathy.util.InheritanceCase;
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

    public int getAsabaUnit() {
        return switch (this) {
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

            // الأب والجد: 1 عندما يكونان عاصبين
            case FATHER, GRANDFATHER
                    -> 1;

            default -> 0;
        };
    }

    public AsabaType getAsabaType() {
        return switch (this) {
            // عصبة بالنفس
            case SON, SON_OF_SON,
                 FATHER, GRANDFATHER,
                 FULL_BROTHER, PATERNAL_BROTHER,
                 FULL_UNCLE, PATERNAL_UNCLE
                    -> AsabaType.BY_SELF;

            // عصبة بالغير
            case DAUGHTER, DAUGHTER_OF_SON,
                 FULL_SISTER, PATERNAL_SISTER
                    -> AsabaType.WITH_OTHER;

            // عصبة مع الغير
            case SON_OF_FULL_BROTHER, SON_OF_PATERNAL_BROTHER,
                 SON_OF_FULL_UNCLE, SON_OF_PATERNAL_UNCLE
                    -> AsabaType.WITH_GHERR;

            default -> AsabaType.NONE;
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

    public boolean canBeAsaba(InheritanceCase context) {
        if (this == FATHER || this == GRANDFATHER) {
            boolean hasMaleDescendant = context.has(HeirType.SON) ||
                    (context.has(HeirType.SON_OF_SON) && !context.has(HeirType.SON));
            return !hasMaleDescendant;
        }
        return this.getAsabaUnit() > 0;
    }
}
