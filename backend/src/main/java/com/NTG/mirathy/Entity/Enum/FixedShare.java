package com.NTG.mirathy.Entity.Enum;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Getter
public enum FixedShare {
    HALF(1, 2),
    QUARTER(1, 4),
    EIGHTH(1, 8),
    TWO_THIRDS(2, 3),
    THIRD(1, 3),
    SIXTH(1, 6),
    THIRD_OF_REMAINDER(1, 3),
    THIRD_REMAINDER_SHARED( 1, 3);

    private final int numerator;
    private final int denominator;

    FixedShare(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }
}
