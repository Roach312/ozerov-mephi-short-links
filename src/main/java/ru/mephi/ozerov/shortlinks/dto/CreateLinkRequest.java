package ru.mephi.ozerov.shortlinks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class CreateLinkRequest {

    @NotBlank(message = "URL обязателен")
    @URL(message = "Некорректный URL")
    private String originalUrl;

    /**
     * Максимальное количество переходов. null = без лимита.
     */
    @Positive(message = "Лимит переходов должен быть положительным")
    private Integer clickLimit;
}
