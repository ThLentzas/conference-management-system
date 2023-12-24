package com.example.conference_management_system.config;

import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.role.RoleTypeDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeserializerConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        ParameterNamesModule module = new ParameterNamesModule();

        module.addDeserializer(RoleType.class, new RoleTypeDeserializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}
