package fr.inote.inoteApi.entity;

import java.util.Collection;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.constraints.Email;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "userTable") // Without @Table name=, problems appears during tests, because user is used by
                           // framework
/*
 * The UserDetailsService interface is a core component in Spring Security used
 * for loading user-specific data. It is responsible for retrieving user
 * information from a backend data source (such as a database or an external
 * service) and returning an instance of the UserDetails interface.
 */
public class User implements UserDetails {

    /* Id & strategy generation <=> primary key */
    /* ============================================================ */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* Private attributes <=> columns */
    /* ============================================================ */
    @NonNull
    private String password;
    @NonNull
    private String name;
    @Email
    private String email;
    @Builder.Default // Set default value when using Lombok @Builder annotation
    @Getter
    private boolean actif = false;

    /* Private attributes in relations with others entities <=> foreign key*/
    /* ============================================================ */
    @ManyToOne
    private Role role;
    
    /* UserDetails interface implementations */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.role.getName().getAuthorities();
    }

    @Override
    public @NonNull String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.actif;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.actif;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.actif;
    }

    @Override
    public boolean isEnabled() {
        return this.actif;
    }
}
