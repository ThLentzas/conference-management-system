package com.example.conference_management_system.role;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class RoleTypeDeserializer extends JsonDeserializer<RoleType> {

    @Override
    public RoleType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String value = jsonParser.getValueAsString()
                .trim()
                .toUpperCase();

        try {
            value = "ROLE_" + value;
            return RoleType.valueOf(value);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid user role: " + value);
        }
    }
}

