package com.dawn.ai.service;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentTools {

    private final ChatModel chatModel;
    private final CatalogModuleApi catalogApi;

    @Tool("Rewrite a more appealing movie description from catalog data (title, genres, overview, duration).")
    public String generateMovieDescription(Long movieId) {
        try {
            MovieResponse movie = catalogApi.findMovieById(movieId);
            String prompt = "Rewrite a short appealing Vietnamese cinema description (max 3 sentences) from: "
                    + "title=" + movie.getTitle()
                    + ", genres=" + movie.getGenres()
                    + ", overview=" + movie.getOverview()
                    + ", duration=" + movie.getDuration() + " minutes.";
            return chatModel.chat(prompt);
        } catch (RuntimeException e) {
            log.warn("Movie description generation failed for {}", movieId, e);
            return "Could not generate description for movie " + movieId + ".";
        }
    }

    @Tool("Draft an article (news/promotion) from a topic. Returns title, summary, content and tags.")
    public String generateArticleDraft(String topic, String type) {
        try {
            String prompt = "Draft a Vietnamese cinema article as title, summary, content and tags. "
                    + "Topic: " + topic + ". Type: " + (type == null ? "news" : type) + ".";
            return chatModel.chat(prompt);
        } catch (RuntimeException e) {
            log.warn("Article draft generation failed for topic {}", topic, e);
            return "Could not generate article draft for: " + topic;
        }
    }
}
