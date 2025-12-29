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
            Mother mother,
            WifeRule wifeRule,
            FatherRule father

    ) {
        return List.of(
                husband,
                daughter,
                mother,
                wifeRule,
                father
        );
    }
}
