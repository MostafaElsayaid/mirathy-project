package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.request.InheritanceCalculationRequest;
import com.NTG.mirathy.Entity.Enum.HeirType;
import com.NTG.mirathy.util.InheritanceCase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ArabicInheritanceTextService {

    public String generateText(InheritanceCalculationRequest request) {
        StringBuilder text = new StringBuilder();

        text.append("توفي شخص ");

        if (request.totalEstate()!=null && request.totalEstate().signum()>0){
            text.append("وترك تركة قدرها ")
                    .append(request.totalEstate())
                    .append(" جنيها ");
        }


        if (request.debts() != null && request.debts().signum() > 0) {
            text.append("وعليه دين قدره ")
                    .append(request.debts())
                    .append(" جنيهًا. ");
        }
        if (request.will() != null && request.will().signum() > 0) {
            text.append("وله وصية بمبلغ ")
                    .append(request.will())
                    .append(" جنيهًا. ");
        }

        text.append("وترك من الورثة: ");

        for (Map.Entry<HeirType,Integer> mp:request.heirs().entrySet()){
            text.append(mp.getKey().getArabicName());
            if (mp.getValue()>1){
                text.append("(").append(mp.getValue()).append(")");
            }
            text.append(" و ");
        }
        text.delete(text.length()-2, text.length());

        return text.toString();
    }


}
