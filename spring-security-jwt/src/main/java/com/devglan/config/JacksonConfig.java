package com.devglan.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.devglan.controller.JacksonCustomCricketDeserializer;
import com.devglan.controller.TeamPlayerInfoDeserializer;
import com.devglan.dao.CricketDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {
	
	@Bean
    public ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.deserializerByType(CricketDataDTO.class, new JacksonCustomCricketDeserializer());
        builder.deserializerByType(Map.class, new TeamPlayerInfoDeserializer());
        return builder.build();
    }
}
