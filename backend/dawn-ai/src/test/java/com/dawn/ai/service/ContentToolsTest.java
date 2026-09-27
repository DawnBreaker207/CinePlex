package com.dawn.ai.service;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentTools")
class ContentToolsTest {

    @Mock
    ChatModel chatModel;
    @Mock
    CatalogModuleApi catalogApi;

    @InjectMocks
    ContentTools tools;

    @Nested
    @DisplayName("generateMovieDescription")
    class MovieDescription {

        @Test
        @DisplayName("passes catalog data to the chat model")
        void usesCatalogData() {
            MovieResponse movie = MovieResponse.builder()
                    .id(3L).title("M").overview("O").duration(120).genres(Set.of("Action"))
                    .build();
            when(catalogApi.findMovieById(3L)).thenReturn(movie);
            when(chatModel.chat(anyString())).thenReturn("Draft text");

            assertThat(tools.generateMovieDescription(3L)).isEqualTo("Draft text");
            verify(chatModel).chat(anyString());
        }

        @Test
        @DisplayName("model failure → fallback message")
        void failure_fallback() {
            when(catalogApi.findMovieById(3L)).thenThrow(new RuntimeException("down"));

            assertThat(tools.generateMovieDescription(3L)).contains("Could not generate");
        }
    }

    @Nested
    @DisplayName("generateArticleDraft")
    class ArticleDraft {

        @Test
        @DisplayName("passes topic to the chat model")
        void usesTopic() {
            when(chatModel.chat(anyString())).thenReturn("Article draft");

            assertThat(tools.generateArticleDraft("Premiere", "news")).isEqualTo("Article draft");
        }
    }
}
