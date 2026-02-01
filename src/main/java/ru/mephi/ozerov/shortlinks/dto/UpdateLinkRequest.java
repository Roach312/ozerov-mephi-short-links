package ru.mephi.ozerov.shortlinks.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateLinkRequest {

    @URL(message = "Некорректный URL")
    private String originalUrl;

    @Positive(message = "Лимит переходов должен быть положительным")
    private Integer clickLimit;
}
