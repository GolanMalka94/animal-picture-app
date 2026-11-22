package com.example.animalpictures.service;

import com.example.animalpictures.client.AnimalImageClient;
import com.example.animalpictures.client.FetchedImage;
import com.example.animalpictures.domain.AnimalPicture;
import com.example.animalpictures.domain.AnimalPictureRepository;
import com.example.animalpictures.domain.AnimalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnimalPictureService {

    public static final int MAX_PICTURES_PER_REQUEST = 10;

    private static final Logger log = LoggerFactory.getLogger(AnimalPictureService.class);

    private final AnimalImageClient imageClient;
    private final AnimalPictureRepository repository;

    public AnimalPictureService(AnimalImageClient imageClient, AnimalPictureRepository repository) {
        this.imageClient = imageClient;
        this.repository = repository;
    }

    /**
     * Fetches {@code count} random pictures of the given animal and stores them.
     *
     * <p>All pictures are downloaded first and then saved together, so a failing download
     * leaves the database untouched (all-or-nothing).
     */
    public List<AnimalPicture> fetchAndSave(AnimalType animalType, int count) {
        if (count < 1 || count > MAX_PICTURES_PER_REQUEST) {
            throw new IllegalArgumentException(
                    "count must be between 1 and " + MAX_PICTURES_PER_REQUEST + " but was " + count);
        }
        List<AnimalPicture> pictures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            FetchedImage image = imageClient.fetchRandom(animalType);
            pictures.add(new AnimalPicture(animalType, image.sourceUrl(), image.contentType(), image.data()));
        }
        List<AnimalPicture> saved = repository.saveAll(pictures);
        log.info("Stored {} {} picture(s)", saved.size(), animalType.value());
        return saved;
    }

    @Transactional(readOnly = true)
    public AnimalPicture getLatest(AnimalType animalType) {
        return repository.findFirstByAnimalTypeOrderByIdDesc(animalType)
                .orElseThrow(() -> new PictureNotFoundException(
                        "No " + animalType.value() + " picture has been stored yet"));
    }

    @Transactional(readOnly = true)
    public AnimalPicture getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PictureNotFoundException("Picture " + id + " does not exist"));
    }
}
