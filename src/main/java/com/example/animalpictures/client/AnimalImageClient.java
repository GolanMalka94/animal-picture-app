package com.example.animalpictures.client;

import com.example.animalpictures.domain.AnimalPicture;
import com.example.animalpictures.domain.AnimalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Downloads a random picture of an animal from the configured external API. */
@Component
public class AnimalImageClient {

    private static final Logger log = LoggerFactory.getLogger(AnimalImageClient.class);

    private final RestClient restClient;
    private final PictureSourceProperties properties;

    public AnimalImageClient(RestClient pictureSourceRestClient, PictureSourceProperties properties) {
        this.restClient = pictureSourceRestClient;
        this.properties = properties;
    }

    public FetchedImage fetchRandom(AnimalType animalType) {
        String url = resolveUrl(animalType);
        log.debug("Fetching {} picture from {}", animalType.value(), url);

        ResponseEntity<byte[]> response;
        try {
            response = restClient.get()
                    .uri(url)
                    .accept(MediaType.ALL)
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (RestClientException e) {
            throw new ImageSourceException("Could not fetch a " + animalType.value() + " picture from " + url, e);
        }

        MediaType contentType = response.getHeaders().getContentType();
        byte[] body = response.getBody();
        if (contentType == null || !"image".equalsIgnoreCase(contentType.getType())) {
            throw new ImageSourceException("Picture source " + url + " did not return an image (content type: "
                    + contentType + ")");
        }
        if (body == null || body.length == 0) {
            throw new ImageSourceException("Picture source " + url + " returned an empty image");
        }
        if (body.length > AnimalPicture.MAX_IMAGE_BYTES) {
            throw new ImageSourceException("Picture from " + url + " exceeds the maximum size of "
                    + AnimalPicture.MAX_IMAGE_BYTES + " bytes");
        }
        return new FetchedImage(url, contentType.getType() + "/" + contentType.getSubtype(), body);
    }

    String resolveUrl(AnimalType animalType) {
        String template = properties.urls().get(animalType);
        if (template == null) {
            throw new ImageSourceException("No picture source configured for animal type " + animalType.value());
        }
        int width = randomSize();
        int height = randomSize();
        return UriComponentsBuilder.fromUriString(template)
                .buildAndExpand(Map.of("width", width, "height", height))
                .toUriString();
    }

    private int randomSize() {
        return ThreadLocalRandom.current().nextInt(properties.minSize(), properties.maxSize() + 1);
    }
}
