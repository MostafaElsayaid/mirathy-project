package com.NTG.mirathy.DTOs;

import com.NTG.mirathy.Entity.Enum.FixedShare;
import com.NTG.mirathy.Entity.Enum.HeirType;

public record InheritanceShareDto(
        HeirType heirType,
        FixedShare fixedShare,
        String reason
) {
}
