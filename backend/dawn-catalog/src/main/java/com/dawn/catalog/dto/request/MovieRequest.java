package com.dawn.catalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MovieRequest {
    @NotBlank(message = "Title is not mandatory")
    @Size(min = 1, max = 100, message = "Required characters")
    private String title;

    private String originalTitle;

    @NotBlank(message = "Poster is not mandatory")
    private String poster;

    private String backdrop;

    @NotBlank(message = "Overview is not mandatory")
    private String overview;

    @NotNull(message = "Duration is not mandatory")
    @Min(value = 0, message = "Duration required longer than 0")
    private Integer duration;

    @NotEmpty(message = "Genres is not mandatory")
    private Set<String> genres = new HashSet<>();

    @NotNull(message = "Release Date is not mandatory")
    @Past(message = "Release Date must be in the past")
    private LocalDate releaseDate;

    private String language;

    private String country;

    @NotBlank(message = "IMDB ID is not mandatory")
    private String imdbId;

    @NotBlank(message = "Film ID is not mandatory")
    private String filmId;

}
