package com.NTG.mirathy.DTOs;

import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;

public record InheritanceShareDto(
        HeirType heirType,
        Integer count,
        Double amountPerPerson,
        Double totalAmount,
        ShareType shareType,
        FixedShare fixedShare,
        String reason

) {


    public InheritanceShareDto withAmounts(Double amountPerPerson, Double totalAmount) {
        return new InheritanceShareDto(
                heirType,
                count,
                amountPerPerson,
                totalAmount,
                shareType,
                fixedShare,
                reason

        );
    }

    public InheritanceShareDto withCount(Integer count) {
        return new InheritanceShareDto(
                heirType,
                count,
                amountPerPerson,
                totalAmount,
                shareType,
                fixedShare,
                reason

        );
    }
}