package fr.inote.inoteApi.dto;

public record NewPasswordRequestDto(
        String email,
        String code,
        String password
) {}




