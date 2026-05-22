package ru.danilavak.zizu.signature;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SignatureModuleProperties.class)
public class SignatureModuleConfiguration {
}
