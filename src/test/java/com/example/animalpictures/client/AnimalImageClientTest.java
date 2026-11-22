package com.example.animalpictures.client;

import com.example.animalpictures.domain.AnimalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class AnimalImageClientTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    private MockRestServiceServer server;
    private AnimalImageClient client;

    @BeforeEach
    void setUp() {
        // min == max makes the generated URL deterministic
        PictureSourceProperties properties = new PictureSourceProperties(
                Map.of(AnimalType.DOG, "https://dogs.test/{width}/{height}",
                        AnimalType.CAT, "https://cats.test/cat?width={width}&height={height}"),
                300, 300, Duration.ofSeconds(1), Duration.ofSeconds(1));
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnimalImageClient(builder.build(), properties);
    }

    @Test
    void fetchesImageFromConfiguredSource() {
        server.expect(requestTo("https://dogs.test/300/300")).andExpect(method(GET))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

        FetchedImage image = client.fetchRandom(AnimalType.DOG);

        assertThat(image.sourceUrl()).isEqualTo("https://dogs.test/300/300");
        assertThat(image.contentType()).isEqualTo("image/jpeg");
        assertThat(image.data()).isEqualTo(JPEG);
        server.verify();
    }

    @Test
    void expandsQueryParameterPlaceholders() {
        server.expect(requestTo("https://cats.test/cat?width=300&height=300"))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_PNG));

        assertThat(client.fetchRandom(AnimalType.CAT).contentType()).isEqualTo("image/png");
    }

    @Test
    void randomSizeStaysWithinConfiguredBounds() {
        PictureSourceProperties properties = new PictureSourceProperties(
                Map.of(AnimalType.BEAR, "https://bears.test/{width}/{height}"),
                200, 250, Duration.ofSeconds(1), Duration.ofSeconds(1));
        AnimalImageClient randomClient = new AnimalImageClient(RestClient.create(), properties);

        for (int i = 0; i < 50; i++) {
            String[] parts = randomClient.resolveUrl(AnimalType.BEAR).replace("https://bears.test/", "").split("/");
            assertThat(Integer.parseInt(parts[0])).isBetween(200, 250);
            assertThat(Integer.parseInt(parts[1])).isBetween(200, 250);
        }
    }

    @Test
    void failsWhenSourceReturnsError() {
        server.expect(requestTo("https://dogs.test/300/300")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.fetchRandom(AnimalType.DOG))
                .isInstanceOf(ImageSourceException.class)
                .hasMessageContaining("dogs.test");
    }

    @Test
    void failsWhenSourceDoesNotReturnAnImage() {
        server.expect(requestTo("https://dogs.test/300/300"))
                .andRespond(withSuccess("<html>oops</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.fetchRandom(AnimalType.DOG))
                .isInstanceOf(ImageSourceException.class)
                .hasMessageContaining("did not return an image");
    }

    @Test
    void failsWhenNoSourceIsConfigured() {
        assertThatThrownBy(() -> client.fetchRandom(AnimalType.BEAR))
                .isInstanceOf(ImageSourceException.class)
                .hasMessageContaining("No picture source configured");
    }
}
