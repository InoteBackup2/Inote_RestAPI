package fr.inote.inote_api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.inote.inote_api.entity.Comment;

@Repository
public interface CommentRepository extends CrudRepository<Comment, Integer> {
}
