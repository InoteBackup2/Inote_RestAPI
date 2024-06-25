package fr.inote.inote_api.dto;

public record PublicUserRequestDto(
        String pseudo,
        String username,
        String avatar,
        boolean actif,
        String role) {
}