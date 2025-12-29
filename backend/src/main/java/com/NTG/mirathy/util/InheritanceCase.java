package com.NTG.mirathy.util;

import com.NTG.mirathy.Entity.Enum.HeirType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class InheritanceCase {
    private final BigDecimal totalEstate;
    private final BigDecimal debts;
    private final BigDecimal will;
    private final Map<HeirType, Integer> heirs;

    public InheritanceCase(
            BigDecimal totalEstate,
            BigDecimal debts,
            BigDecimal will,
            Map<HeirType, Integer> heirs
    ) {
        this.totalEstate = totalEstate != null ? totalEstate : BigDecimal.ZERO;
        this.debts = debts != null ? debts : BigDecimal.ZERO;
        this.will = will != null ? will : BigDecimal.ZERO;
        this.heirs = heirs != null ? new HashMap<>(heirs) : new HashMap<>();
    }




    public int count(HeirType type) {
        return heirs.getOrDefault(type, 0);
    }

    public boolean has(HeirType type) {
        return count(type) > 0;
    }

    public boolean hasChildren() {
        return has(HeirType.SON) || has(HeirType.DAUGHTER);
    }


    public boolean hasMaleChild() {
        return has(HeirType.SON);
    }

    public boolean hasDescendant() {
        return has(HeirType.SON) || has(HeirType.DAUGHTER)
                || has(HeirType.DAUGHTER_OF_SON)
                || has(HeirType.SON_OF_SON);
    }

    public boolean hasSpouse() {
        return has(HeirType.HUSBAND) || has(HeirType.WIFE);
    }

    public int countMaleChildren() {
        return count(HeirType.SON);
    }
    public int countFemaleChildren() {
        return count(HeirType.DAUGHTER);
    }
    public int countTotalChildren() {
        return countMaleChildren() + countFemaleChildren();
    }
    public boolean hasBrothersOrSisters() {
        return has(HeirType.FULL_BROTHER) || has(HeirType.FULL_SISTER)
                || has(HeirType.PATERNAL_BROTHER) || has(HeirType.PATERNAL_SISTER);
    }

    public BigDecimal getNetEstate() {
        return totalEstate.subtract(debts).subtract(will).max(BigDecimal.ZERO);
    }

    public Map<HeirType, Integer> getHeirs() {
        return new HashMap<>(heirs);
    }


}
