package fr.inote.inoteApi.entity;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    /* Id & strategy generation <=> primary key */
    /* ============================================================ */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* Private attributes <=> columns */
    /* ============================================================ */
    @NonNull
    @Enumerated(EnumType.STRING) // @Enumerated is an annotation that indicates how an enumerated type should be
                                 // persisted in the database.
    private RoleEnum name;
}
