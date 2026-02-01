package ru.mephi.ozerov.shortlinks.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLinkResponse {

    private LinkResponse link;
    /**
     * UUID пользователя. При первом запросе генерируется и возвращается здесь; далее передавать в заголовке X-User-Id.
     */
    private UUID userId;
}
