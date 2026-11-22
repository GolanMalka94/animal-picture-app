package com.example.animalpictures.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
class PictureSourceClientConfig {

    /** HTTP client for the external picture APIs: follows redirects and applies timeouts. */
    @Bean
    RestClient pictureSourceRestClient(RestClient.Builder builder, PictureSourceProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "animal-picture-app/1.0")
                .build();
    }
}
