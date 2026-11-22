package com.example.animalpictures.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** A picture fetched from an external API, stored together with its binary content. */
@Entity
@Table(name = "animal_picture", indexes = @Index(name = "idx_animal_picture_type", columnList = "animal_type"))
public class AnimalPicture {

    /** Upper bound for a stored image (10 MB). */
    public static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "animal_type", nullable = false, length = 16)
    private AnimalType animalType;

    @Column(name = "source_url", nullable = false, length = 1024)
    private String sourceUrl;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "image_data", nullable = false, length = MAX_IMAGE_BYTES)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AnimalPicture() {
        // required by JPA
    }

    public AnimalPicture(AnimalType animalType, String sourceUrl, String contentType, byte[] data) {
        this.animalType = animalType;
        this.sourceUrl = sourceUrl;
        this.contentType = contentType;
        this.data = data;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public AnimalType getAnimalType() {
        return animalType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public int getSizeBytes() {
        return data == null ? 0 : data.length;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
