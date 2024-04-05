package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.entity.Comment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Comment repository.
 * @author atsuhiko Mochizuki
 */
@Repository
public interface CommentRepository extends CrudRepository<Comment,Integer> {
}
