package com.example.animalpictures.web;

import com.example.animalpictures.domain.AnimalPicture;
import com.example.animalpictures.domain.AnimalType;
import com.example.animalpictures.service.AnimalPictureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Animal pictures")
public class AnimalPictureController {

    private final AnimalPictureService service;

    public AnimalPictureController(AnimalPictureService service) {
        this.service = service;
    }

    @Operation(summary = "Fetch random pictures of an animal from the external API and store them")
    @PostMapping("/animals/{animal}/pictures")
    public ResponseEntity<List<PictureResponse>> fetchAndSave(
            @Parameter(description = "cat, dog or bear") @PathVariable AnimalType animal,
            @Parameter(description = "Number of pictures to fetch (1-10)")
            @RequestParam(defaultValue = "1") @Min(1) @Max(AnimalPictureService.MAX_PICTURES_PER_REQUEST) int count) {
        List<PictureResponse> saved = service.fetchAndSave(animal, count).stream()
                .map(PictureResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Metadata of the last stored picture of an animal")
    @GetMapping("/animals/{animal}/pictures/latest")
    public PictureResponse latest(@Parameter(description = "cat, dog or bear") @PathVariable AnimalType animal) {
        return PictureResponse.from(service.getLatest(animal));
    }

    @Operation(summary = "The last stored picture of an animal, as an image")
    @GetMapping("/animals/{animal}/pictures/latest/image")
    public ResponseEntity<byte[]> latestImage(
            @Parameter(description = "cat, dog or bear") @PathVariable AnimalType animal) {
        return imageResponse(service.getLatest(animal), CacheControl.noStore());
    }

    @Operation(summary = "A stored picture by id, as an image")
    @GetMapping("/pictures/{id}/image")
    public ResponseEntity<byte[]> imageById(@PathVariable long id) {
        // stored pictures never change, so they can be cached
        return imageResponse(service.getById(id), CacheControl.maxAge(Duration.ofDays(1)));
    }

    private static ResponseEntity<byte[]> imageResponse(AnimalPicture picture, CacheControl cacheControl) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.getContentType()))
                .cacheControl(cacheControl)
                .header("X-Picture-Id", String.valueOf(picture.getId()))
                .body(picture.getData());
    }
}
