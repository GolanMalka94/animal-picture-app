package com.example.animalpictures.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AnimalPictureRepositoryTest {

    @Autowired
    private AnimalPictureRepository repository;

    @Test
    void findsLatestPictureOfTheRequestedAnimalOnly() {
        repository.save(new AnimalPicture(AnimalType.DOG, "https://dogs.test/1", "image/jpeg", new byte[]{1}));
        AnimalPicture latestDog = repository.save(
                new AnimalPicture(AnimalType.DOG, "https://dogs.test/2", "image/jpeg", new byte[]{2}));
        repository.save(new AnimalPicture(AnimalType.CAT, "https://cats.test/1", "image/png", new byte[]{3}));

        assertThat(repository.findFirstByAnimalTypeOrderByIdDesc(AnimalType.DOG))
                .get()
                .extracting(AnimalPicture::getId, AnimalPicture::getSourceUrl)
                .containsExactly(latestDog.getId(), "https://dogs.test/2");
        assertThat(repository.findFirstByAnimalTypeOrderByIdDesc(AnimalType.BEAR)).isEmpty();
    }

    @Test
    void storesImageBytesAndCreationTime() {
        byte[] largeImage = new byte[512 * 1024];
        largeImage[largeImage.length - 1] = 42;

        AnimalPicture saved = repository.saveAndFlush(
                new AnimalPicture(AnimalType.BEAR, "https://bears.test/1", "image/jpeg", largeImage));

        AnimalPicture reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getData()).isEqualTo(largeImage);
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
}
