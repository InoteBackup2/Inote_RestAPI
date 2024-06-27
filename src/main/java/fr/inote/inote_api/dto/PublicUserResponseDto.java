package fr.inote.inote_api.dto;

import fr.inote.inote_api.entity.Role;

public record PublicUserResponseDto(
        String username,
        String pseudonyme,
        String avatar,
        boolean actif,
        Role role) {}
