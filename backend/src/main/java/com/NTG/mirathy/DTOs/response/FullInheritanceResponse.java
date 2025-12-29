package com.NTG.mirathy.DTOs.response;

import com.NTG.mirathy.DTOs.InheritanceShareDto;

import java.util.List;

public record FullInheritanceResponse(
        String title,
        Double totalEstate,
        Double netEstate,
        List<InheritanceShareDto> shares,
        Double remainingEstate
) {
    public Double getTotalDistributed() {
        return shares.stream()
                .mapToDouble(InheritanceShareDto::amount)
                .sum();
    }
}
