package fr.inote.inoteApi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "validation")
public class Validation {

    /* Id & strategy generation <=> primary key */
    /* ============================================================ */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* Private attributes <=> columns */
    /* ============================================================ */
    private Instant creation;
    private Instant expiration;
    private Instant activation;
    private String code;

    /* Private attributes in relations with others entities <=> foreign key */
    /* ============================================================ */
    @ManyToOne
    private User user;

}
