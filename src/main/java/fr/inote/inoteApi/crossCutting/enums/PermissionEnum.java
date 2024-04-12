package fr.inote.inoteApi.crossCutting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * List all permissions associated with a Role
 */

@RequiredArgsConstructor
@Getter
public enum PermissionEnum {

    // ADMIN
    ADMINISTRATEUR_CREATE,
    ADMINISTRATEUR_READ,
    ADMINISTRATEUR_UPDATE,
    ADMINISTRATEUR_DELETE,

    // MANAGER
    MANAGER_CREATE,
    MANAGER_READ,
    MANAGER_UPDATE,
    MANAGER_DELETE_AVIS,

    // USER
    UTILISATEUR_CREATE_AVIS,

    //TESTER
    TESTER_TESTER;

    private String libelle;
}
