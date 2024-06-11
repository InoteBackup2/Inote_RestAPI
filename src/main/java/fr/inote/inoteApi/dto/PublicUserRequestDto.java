package fr.inote.inoteApi.dto;

public record PublicUserRequestDto(
        String pseudo,
        String username,
        String avatar,
        boolean actif,
        String role) {
}