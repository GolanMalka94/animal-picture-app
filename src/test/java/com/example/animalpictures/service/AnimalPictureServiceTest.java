package com.example.animalpictures.service;

import com.example.animalpictures.client.AnimalImageClient;
import com.example.animalpictures.client.FetchedImage;
import com.example.animalpictures.client.ImageSourceException;
import com.example.animalpictures.domain.AnimalPicture;
import com.example.animalpictures.domain.AnimalPictureRepository;
import com.example.animalpictures.domain.AnimalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalPictureServiceTest {

    @Mock
    private AnimalImageClient imageClient;

    @Mock
    private AnimalPictureRepository repository;

    @InjectMocks
    private AnimalPictureService service;

    @Test
    @SuppressWarnings("unchecked")
    void fetchesRequestedNumberOfPicturesAndSavesThem() {
        when(imageClient.fetchRandom(AnimalType.DOG))
                .thenReturn(new FetchedImage("https://dogs.test/1", "image/jpeg", new byte[]{1}));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AnimalPicture> saved = service.fetchAndSave(AnimalType.DOG, 3);

        assertThat(saved).hasSize(3).allSatisfy(picture -> {
            assertThat(picture.getAnimalType()).isEqualTo(AnimalType.DOG);
            assertThat(picture.getContentType()).isEqualTo("image/jpeg");
            assertThat(picture.getSourceUrl()).isEqualTo("https://dogs.test/1");
        });
        verify(imageClient, times(3)).fetchRandom(AnimalType.DOG);
        ArgumentCaptor<List<AnimalPicture>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 11})
    void rejectsInvalidCount(int count) {
        assertThatThrownBy(() -> service.fetchAndSave(AnimalType.CAT, count))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(imageClient, repository);
    }

    @Test
    void savesNothingWhenAFetchFails() {
        when(imageClient.fetchRandom(AnimalType.BEAR))
                .thenReturn(new FetchedImage("https://bears.test/1", "image/jpeg", new byte[]{1}))
                .thenThrow(new ImageSourceException("down"));

        assertThatThrownBy(() -> service.fetchAndSave(AnimalType.BEAR, 2)).isInstanceOf(ImageSourceException.class);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void returnsLatestPicture() {
        AnimalPicture picture = new AnimalPicture(AnimalType.CAT, "https://cats.test", "image/png", new byte[]{1});
        when(repository.findFirstByAnimalTypeOrderByIdDesc(AnimalType.CAT)).thenReturn(Optional.of(picture));

        assertThat(service.getLatest(AnimalType.CAT)).isSameAs(picture);
    }

    @Test
    void failsWhenNoPictureStoredYet() {
        when(repository.findFirstByAnimalTypeOrderByIdDesc(AnimalType.CAT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(AnimalType.CAT))
                .isInstanceOf(PictureNotFoundException.class)
                .hasMessageContaining("cat");
    }
}
