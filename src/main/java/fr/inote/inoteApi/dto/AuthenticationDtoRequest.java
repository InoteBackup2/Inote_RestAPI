package fr.inote.inoteApi.dto;

public record AuthenticationDtoRequest(
        String username,
        String password) {}
