package com.NTG.mirathy.rule;

import com.NTG.mirathy.DTOs.InheritanceShareDto;
import com.NTG.mirathy.Entity.InheritanceMember;
import com.NTG.mirathy.util.InheritanceCase;

public interface   InheritanceRule {

      boolean canApply(InheritanceCase c);

      InheritanceShareDto calculate(InheritanceCase c);

}
