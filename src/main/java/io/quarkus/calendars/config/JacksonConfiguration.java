package io.quarkus.calendars.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class JacksonConfiguration {

    @Produces
    @Singleton
    public YAMLMapper yamlMapper() {
        return YAMLMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
}
