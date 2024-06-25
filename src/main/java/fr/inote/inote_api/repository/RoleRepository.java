package fr.inote.inote_api.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.inote.inote_api.cross_cutting.enums.RoleEnum;
import fr.inote.inote_api.entity.Role;

@Repository
public interface RoleRepository extends CrudRepository<Role, Integer> {

    Optional<Role> findByName(RoleEnum libelle);
}
