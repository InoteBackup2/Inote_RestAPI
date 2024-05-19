package fr.inote.inoteApi.dto;

public record PasswordDtoRequest(
        String email,
        String code,
        String password
) {}




