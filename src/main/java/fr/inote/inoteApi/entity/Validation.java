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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Instant creation;
    private Instant expiration;
    private Instant activation;
    private String code;

    //    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE})
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.DETACH})
    private User user;

}

