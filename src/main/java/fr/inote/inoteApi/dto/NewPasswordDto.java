package fr.inote.inoteApi.dto;

public record NewPasswordDto(
        String email,
        String code,
        String password
) {}




