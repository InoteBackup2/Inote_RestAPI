package fr.inote.inote_api.dto;

public record CommentResponseDto(
        Integer id,
        String message,
        Integer UserId
) {}
