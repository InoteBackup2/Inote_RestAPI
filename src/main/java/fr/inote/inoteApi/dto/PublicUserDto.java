package fr.inote.inoteApi.dto;

public record PublicUserDto(
        String pseudo,
        String username,
        String avatar,
        boolean actif,
        String role) {
}
