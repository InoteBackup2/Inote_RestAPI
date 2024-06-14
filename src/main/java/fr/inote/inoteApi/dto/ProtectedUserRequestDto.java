package fr.inote.inoteApi.dto;

public record ProtectedUserRequestDto(
    String name,
    String email,
    boolean active,
    String pseudonyme,
    String avatar,
    String roleName
) {

}
