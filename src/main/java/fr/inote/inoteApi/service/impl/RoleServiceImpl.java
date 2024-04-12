package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The Service RoleServiceImpl
 * @author Atsuhiko Mochizuki
 * @date   11/04/2024
 */
@Service
public class RoleServiceImpl implements RoleService {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    private RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /* PUBLIC METHODS */
    /* ============================================================ */
    
    /**
     * Populates the role table from the dedicated enumeration
     *
     * @return persisted roles in database
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    @Override
    public List<Role> insertRolesInDb() {
        List<Role> rolesInApp = new ArrayList<>();
        for (RoleEnum role : RoleEnum.values()) {
            rolesInApp.add(this.roleRepository.save(
                    Role.builder().name(role).build()));
        }
        return rolesInApp;
    }

    /**
     * Load admin role
     *
     * @return Singleton of asked admin Role
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    @Override
    public Role loadAdminRole() throws InoteUserException {
        return this.roleRepository.findByName(RoleEnum.ADMIN).orElseThrow(() -> new InoteUserException());
    }

    /**
     * Load Manager role
     *
     * @return Singleton of asked manager Role
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    @Override
    public Role loadManagerRole() throws InoteUserException {
        return this.roleRepository.findByName(RoleEnum.MANAGER).orElseThrow(() -> new InoteUserException());
    }

    /**
     * Load User role
     *
     * @return Singleton of asked user Role
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    @Override
    public Role loadUserRole() throws InoteUserException {
        return this.roleRepository.findByName(RoleEnum.USER).orElseThrow(() -> new InoteUserException());
    }
}
