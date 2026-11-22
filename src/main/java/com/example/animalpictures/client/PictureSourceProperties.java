package com.example.animalpictures.client;

import com.example.animalpictures.domain.AnimalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration of the external picture APIs.
 *
 * <p>Each URL is a template that may contain {@code {width}} and {@code {height}} placeholders.
 * A random size between {@code minSize} and {@code maxSize} is used per request, which makes the
 * placeholder services return a different picture each time.
 */
@Validated
@ConfigurationProperties(prefix = "app.picture-sources")
public record PictureSourceProperties(
        @NotEmpty Map<AnimalType, String> urls,
        @DefaultValue("200") @Min(50) int minSize,
        @DefaultValue("500") @Min(50) int maxSize,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("15s") Duration readTimeout) {

    public PictureSourceProperties {
        if (maxSize < minSize) {
            throw new IllegalArgumentException("app.picture-sources.max-size must be >= min-size");
        }
        urls = Map.copyOf(urls);
    }
}
