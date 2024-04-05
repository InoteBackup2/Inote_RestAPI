package fr.inote.inoteApi.service;

import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.entity.Role;

import java.util.List;

public interface RoleService {

    /**
     * Populates the role table from the dedicated enumeration
     *
     * @return list of saved roles
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    List<Role> insertRolesInDb();

    /**
     * Load admin role
     *
     * @return Singleton of asked admin Role
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    Role loadAdminRole() throws InoteUserException;

    /**
     * Load Manager role
     *
     * @return Singleton of asked manager Role
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    Role loadManagerRole() throws InoteUserException;

    /**
     * Load User role
     *
     * @return Singleton of asked user Role
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    Role loadUserRole() throws InoteUserException;
}

