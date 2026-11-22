package com.example.animalpictures.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimalTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"cat", "CAT", "Cat", " cat "})
    void parsesCaseInsensitively(String value) {
        assertThat(AnimalType.fromValue(value)).isEqualTo(AnimalType.CAT);
    }

    @Test
    void rejectsUnknownAnimalWithHelpfulMessage() {
        assertThatThrownBy(() -> AnimalType.fromValue("unicorn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unicorn")
                .hasMessageContaining("cat, dog, bear");
    }

    @Test
    void exposesLowerCaseValue() {
        assertThat(AnimalType.BEAR.value()).isEqualTo("bear");
    }
}
