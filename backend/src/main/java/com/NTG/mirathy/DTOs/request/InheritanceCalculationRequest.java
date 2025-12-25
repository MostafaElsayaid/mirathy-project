package com.NTG.mirathy.DTOs.request;

import com.NTG.mirathy.Entity.Enum.HeirType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Map;

public record InheritanceCalculationRequest(
        @Positive(message = "Total estate must be greater than zero")
        BigDecimal totalEstate,

        @PositiveOrZero(message = "Debts must be zero or positive")
        BigDecimal debts,

        @PositiveOrZero(message = "Will must be zero or positive")
        BigDecimal will,

        @NotNull(message = "Heirs map must not be null")
        @NotEmpty(message = "Heirs map must not be empty")
        Map<
                @NotNull(message = "Heir type must not be null")
                        HeirType,
                @NotNull(message = "Heir count must not be null")
                        Integer> heirs
) {
}
