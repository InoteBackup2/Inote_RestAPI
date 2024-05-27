package fr.inote.inoteApi.dto;

public record CommentResponseDto(
        Integer id,
        String message,
        Integer UserId
) {}
