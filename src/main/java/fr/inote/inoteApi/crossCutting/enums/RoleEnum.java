package fr.inote.inoteApi.crossCutting.enums;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * List the various possible roles in the application,
 * specifying the associated permissions for each.
 * To retrieve the permissions for each role (GrantedAuthority),
 * use the public getAuthorities() method
 */

@AllArgsConstructor
@Getter
public enum RoleEnum {
        /*
         * In Java, we can add constructors, fields and methods as we do with other
         * classes.
         * We use here a special syntax possible withs enums in Java.
         * This is how a constructor is invoked for enum types. Although it’s illegal to
         * use the new operator for an enum, we can pass constructor arguments in the
         * declaration list.
         * When an ADMIN role is assigned to a user, it's an instance of RoleEnum,
         * containing a variable of type Set<PermissionEnum>.
         * 
         */
        // USER role permissions
        USER(Set.of(PermissionEnum.UTILISATEUR_CREATE_AVIS)), // Set.of create an immuable Set

        // ADMIN role permissions
        ADMIN(Set.of(PermissionEnum.ADMINISTRATEUR_CREATE,
                        PermissionEnum.ADMINISTRATEUR_READ,
                        PermissionEnum.ADMINISTRATEUR_UPDATE,
                        PermissionEnum.ADMINISTRATEUR_DELETE,
                        PermissionEnum.MANAGER_CREATE,
                        PermissionEnum.MANAGER_READ,
                        PermissionEnum.MANAGER_UPDATE,
                        PermissionEnum.MANAGER_DELETE_AVIS)),

        // MANAGER role permissions
        MANAGER(Set.of(PermissionEnum.MANAGER_CREATE,
                        PermissionEnum.MANAGER_READ,
                        PermissionEnum.MANAGER_UPDATE,
                        PermissionEnum.MANAGER_DELETE_AVIS)),

        // TESTER role permissions
        TESTER(Set.of(PermissionEnum.TESTER_TESTER));

        /**
         * Permissions Set for a given role
         */
        private final Set<PermissionEnum> permissions;

        /**
         * Get the Spring Security Autorities associated to the given role.
         * Nota: GrantedAuthority is a Spring Security interface representing a
         * permission or role
         * assigned to an user of application
         * 
         * @return a list of authorities for the given role
         */
        public Collection<? extends GrantedAuthority> getAuthorities() {

                List<SimpleGrantedAuthority> grantedAuthorities =
                                // Get the Set for a given role
                                this.getPermissions()
                                                // Transforming the Set into a flow to iterate over each permission
                                                .stream()
                                                // Each permission is transformed here into an effective authorization
                                                // in Spring Security
                                                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                                                // Collecting permissions in a list
                                                .collect(Collectors.toList());
                // Title this set of permissions
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));

                return grantedAuthorities;
        }
}
