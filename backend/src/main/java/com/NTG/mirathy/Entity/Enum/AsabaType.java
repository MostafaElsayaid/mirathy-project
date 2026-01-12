package com.NTG.mirathy.Entity.Enum;

public enum AsabaType {
    BY_SELF("عصبة بالنفس"),
    WITH_OTHER("عصبة بالغير"),
    WITH_GHERR("عصبة مع الغير"),
    NONE("ليس بعاصب");

    private final String arabicName;

    AsabaType(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
