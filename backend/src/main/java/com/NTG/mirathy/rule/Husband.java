package com.NTG.mirathy.rule;


import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.Entity.InheritanceMember;
import com.NTG.mirathy.util.InheritanceCase;

public class Husband implements InheritanceRule {
    @Override
    public boolean canApply(InheritanceCase c) {
        return c.has(HeirType.HUSBAND);
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        return null;
    }
}
