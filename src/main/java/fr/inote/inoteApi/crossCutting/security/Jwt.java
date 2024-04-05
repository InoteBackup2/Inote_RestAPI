package fr.inote.inoteApi.crossCutting.security;

import fr.inote.inoteApi.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jwt")
public class Jwt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String contentValue;
    private boolean deactivated;
    private boolean expired;

    // In this case, the cascade makes the creation or deletion of a refreshToken
    // implicit in the creation or deletion of a token.
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private RefreshToken refreshToken;

    private Date refreshTokenExpiration;

    // When the token is detached or merged from the persistence context, so are the
    // user.
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE })
    @JoinColumn(name = "user_id")
    private User user;
}