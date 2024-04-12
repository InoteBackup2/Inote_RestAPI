package fr.inote.inoteApi.dto;

public record CommentDtoResponse(
        Integer id,
        String message,
        Integer UserId
) {}
