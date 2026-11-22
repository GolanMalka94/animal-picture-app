package com.example.animalpictures.web;

import com.example.animalpictures.domain.AnimalType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Lets path variables use lower-case animal names, e.g. {@code /api/animals/cat/pictures}. */
@Component
class AnimalTypeConverter implements Converter<String, AnimalType> {

    @Override
    public AnimalType convert(String source) {
        return AnimalType.fromValue(source);
    }
}
