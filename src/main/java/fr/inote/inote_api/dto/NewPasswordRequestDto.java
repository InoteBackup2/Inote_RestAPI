package fr.inote.inote_api.dto;

public record NewPasswordRequestDto(
        String email,
        String code,
        String password
) {}




