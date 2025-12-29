package com.NTG.mirathy.DTOs;

import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.Enum.ShareType;

public record InheritanceShareDto(
        Double amount,
        HeirType heirType,
        ShareType shareType,
        FixedShare fixedShare,
        String reason
) {

    public InheritanceShareDto withAmount(Double newAmount) {
        return new InheritanceShareDto(
                newAmount,
                heirType,
                shareType,
                fixedShare,
                reason
        );
    }

    public InheritanceShareDto withReason(String newReason) {
        return new InheritanceShareDto(
                amount,
                heirType,
                shareType,
                fixedShare,
                newReason
        );
    }
}
