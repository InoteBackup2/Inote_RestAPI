package fr.inote.inoteApi.dto;

import fr.inote.inoteApi.entity.Role;

public record PublicUserResponseDto(
        String username,
        String pseudonyme,
        String avatar,
        boolean actif,
        Role role) {}
