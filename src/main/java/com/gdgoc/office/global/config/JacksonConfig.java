package com.gdgoc.office.global.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /** LocalDateTime 을 타임존 없이 2026-10-05T10:00:00 형식으로 직렬화한다. */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
                .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
