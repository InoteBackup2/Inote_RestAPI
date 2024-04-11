package fr.inote.inoteApi.entity;

import java.util.Set;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @NonNull
    @Enumerated(EnumType.STRING)    // @Enumerated is an annotation that indicates how an enumerated type should be persisted in the database.
    private RoleEnum name;

    // @OneToMany(mappedBy = "role")
    // private Set<User> users; // Use a Set collection to avoid duplication (doublons)

}
