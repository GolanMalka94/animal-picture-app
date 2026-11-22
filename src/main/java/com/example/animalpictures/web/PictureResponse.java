package com.example.animalpictures.web;

import com.example.animalpictures.domain.AnimalPicture;

import java.time.Instant;

/** Picture metadata returned by the REST API. The image itself is served from {@code imageUrl}. */
public record PictureResponse(
        Long id,
        String animal,
        String contentType,
        int sizeBytes,
        String sourceUrl,
        Instant createdAt,
        String imageUrl) {

    static PictureResponse from(AnimalPicture picture) {
        return new PictureResponse(
                picture.getId(),
                picture.getAnimalType().value(),
                picture.getContentType(),
                picture.getSizeBytes(),
                picture.getSourceUrl(),
                picture.getCreatedAt(),
                "/api/pictures/" + picture.getId() + "/image");
    }
}
