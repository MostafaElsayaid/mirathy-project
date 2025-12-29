package com.NTG.mirathy.configuration;


import com.NTG.mirathy.rule.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RuleConfiguration {

    @Bean
    public List<InheritanceRule> inheritanceRules(
            HusbandRule husband,
            DaughterRule daughter,
            MotherRule mother,
            WifeRule wifeRule,
            FatherRule father,
            SonRule son,
            GrandfatherRule grandfatherRule

    ) {
        return List.of(
                husband,
                daughter,
                mother,
                wifeRule,
                father,
                son,
                grandfatherRule
        );
    }
}
