package ru.danilavak.zizu.binaryapi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BinaryApiProperties.class)
public class BinaryApiConfiguration {
}
