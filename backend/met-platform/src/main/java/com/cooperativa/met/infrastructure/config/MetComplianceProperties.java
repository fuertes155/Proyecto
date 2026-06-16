package com.cooperativa.met.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "met.compliance")
public class MetComplianceProperties {

    private String entityCode = "COOP001";
    private String storagePath = "./data/regulatory-reports";
}
