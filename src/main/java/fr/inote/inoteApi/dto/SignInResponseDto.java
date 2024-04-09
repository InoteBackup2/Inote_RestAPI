package fr.inote.inoteApi.dto;

import org.springframework.http.HttpStatus;

public record SignInResponseDto(
        String bearer,
        String refreshToken

) {
}
