package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.util.InheritanceCase;

public class Test implements InheritanceRule{
    @Override
    public boolean canApply(InheritanceCase c) {
        return false;
    }

    @Override
    public InheritanceShareDto calculate(InheritanceCase c) {
        return null;
    }
}
