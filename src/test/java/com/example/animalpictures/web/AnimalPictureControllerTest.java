package com.example.animalpictures.web;

import com.example.animalpictures.client.ImageSourceException;
import com.example.animalpictures.domain.AnimalPicture;
import com.example.animalpictures.domain.AnimalType;
import com.example.animalpictures.service.AnimalPictureService;
import com.example.animalpictures.service.PictureNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalPictureController.class)
class AnimalPictureControllerTest {

    private static final byte[] IMAGE = {1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalPictureService service;

    @Test
    void fetchAndSaveReturnsCreatedPictures() throws Exception {
        when(service.fetchAndSave(AnimalType.CAT, 2)).thenReturn(List.of(picture(1L), picture(2L)));

        mockMvc.perform(post("/api/animals/cat/pictures").param("count", "2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].animal").value("cat"))
                .andExpect(jsonPath("$[0].sizeBytes").value(3))
                .andExpect(jsonPath("$[1].imageUrl").value("/api/pictures/2/image"));
    }

    @Test
    void countDefaultsToOne() throws Exception {
        when(service.fetchAndSave(AnimalType.DOG, 1)).thenReturn(List.of(picture(1L)));

        mockMvc.perform(post("/api/animals/DOG/pictures"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsCountAboveLimit() throws Exception {
        mockMvc.perform(post("/api/animals/cat/pictures").param("count", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("count")));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnknownAnimal() throws Exception {
        mockMvc.perform(post("/api/animals/unicorn/pictures"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("cat, dog, bear")));
    }

    @Test
    void returns502WhenSourceIsDown() throws Exception {
        when(service.fetchAndSave(AnimalType.BEAR, 1)).thenThrow(new ImageSourceException("bears are sleeping"));

        mockMvc.perform(post("/api/animals/bear/pictures"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("bears are sleeping"));
    }

    @Test
    void latestReturnsMetadata() throws Exception {
        when(service.getLatest(AnimalType.DOG)).thenReturn(picture(7L));

        mockMvc.perform(get("/api/animals/dog/pictures/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    void latestImageReturnsImageBytes() throws Exception {
        when(service.getLatest(AnimalType.DOG)).thenReturn(picture(7L));

        mockMvc.perform(get("/api/animals/dog/pictures/latest/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("X-Picture-Id", "7"))
                .andExpect(content().bytes(IMAGE));
    }

    @Test
    void latestReturns404WhenNothingStored() throws Exception {
        when(service.getLatest(AnimalType.CAT)).thenThrow(new PictureNotFoundException("No cat picture"));

        mockMvc.perform(get("/api/animals/cat/pictures/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Picture not found"));
    }

    @Test
    void imageByIdReturns404ForUnknownId() throws Exception {
        when(service.getById(anyLong())).thenThrow(new PictureNotFoundException("Picture 99 does not exist"));

        mockMvc.perform(get("/api/pictures/99/image")).andExpect(status().isNotFound());
    }

    private static AnimalPicture picture(Long id) {
        AnimalPicture picture = new AnimalPicture(AnimalType.CAT, "https://source.test/" + id, "image/jpeg", IMAGE);
        ReflectionTestUtils.setField(picture, "id", id);
        return picture;
    }
}
