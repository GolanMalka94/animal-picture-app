package com.example.animalpictures;

import com.example.animalpictures.client.AnimalImageClient;
import com.example.animalpictures.client.FetchedImage;
import com.example.animalpictures.domain.AnimalPictureRepository;
import com.example.animalpictures.domain.AnimalType;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test through HTTP, service and the real (in-memory H2) database.
 * Only the external picture APIs are replaced, so the build never depends on third-party uptime.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnimalPictureApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnimalPictureRepository repository;

    @MockitoBean
    private AnimalImageClient imageClient;

    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(imageClient.fetchRandom(any())).thenAnswer(invocation -> {
            AnimalType type = invocation.getArgument(0);
            int n = counter.incrementAndGet();
            return new FetchedImage("https://" + type.value() + ".test/" + n, "image/jpeg", new byte[]{(byte) n});
        });
    }

    @Test
    void storesPicturesAndServesTheLatestOne() throws Exception {
        mockMvc.perform(post("/api/animals/dog/pictures").param("count", "3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(post("/api/animals/cat/pictures"))
                .andExpect(status().isCreated());

        String latestDog = mockMvc.perform(get("/api/animals/dog/pictures/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animal").value("dog"))
                .andExpect(jsonPath("$.sourceUrl").value("https://dog.test/" + (counter.get() - 1)))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/animals/dog/pictures/latest/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(new byte[]{(byte) (counter.get() - 1)}));

        String imageUrl = JsonPath.read(latestDog, "$.imageUrl");
        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));

        assertStoredCount(4);
    }

    @Test
    void latestReturns404BeforeAnythingIsStored() throws Exception {
        mockMvc.perform(get("/api/animals/bear/pictures/latest"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesUiAndHealthEndpoint() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private void assertStoredCount(long expected) {
        assertThat(repository.count()).isEqualTo(expected);
    }
}
