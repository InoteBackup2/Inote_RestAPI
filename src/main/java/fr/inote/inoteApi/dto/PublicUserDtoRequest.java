package fr.inote.inoteApi.dto;

public record PublicUserDtoRequest(
        String pseudo,
        String username,
        String avatar,
        boolean actif,
        String role) {
}
