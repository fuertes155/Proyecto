package com.cooperativa.fintech.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fintech.compliance")
public class FintechComplianceProperties {

    private String entityCode = "COOP001";
    private String storagePath = "./data/regulatory-reports";
}
