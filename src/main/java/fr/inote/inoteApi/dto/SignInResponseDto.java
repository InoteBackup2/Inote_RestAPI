package fr.inote.inoteApi.dto;

public record SignInResponseDto(
                String bearer,
                String refresh) {}
