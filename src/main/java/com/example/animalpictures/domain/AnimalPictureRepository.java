package com.example.animalpictures.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnimalPictureRepository extends JpaRepository<AnimalPicture, Long> {

    /** The most recently stored picture of the given animal (ids are monotonically increasing). */
    Optional<AnimalPicture> findFirstByAnimalTypeOrderByIdDesc(AnimalType animalType);
}
