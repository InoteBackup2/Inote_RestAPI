package fr.inote.inoteApi.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;

/**
 * The interface Role repository.
 * @author AtsuhikoMochizuki
 */
@Repository
public interface RoleRepository extends CrudRepository<Role,Integer>{
    /**
     * Find by name optional.
     *
     * @param libelle the libelle
     * @return the optional
     * @author AtsuhikoMochizuki
     */
    Optional<Role> findByName(RoleEnum libelle);
}
