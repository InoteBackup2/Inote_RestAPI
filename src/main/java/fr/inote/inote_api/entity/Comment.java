package fr.inote.inote_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comment")
public class Comment {

    /* Id & strategy generation <=> primary key */
    /* ============================================================ */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    /* Private attributes <=> columns */
    /* ============================================================ */
    private Integer id;
    private String message;
    private String status;

    /* Private attributes in relations with others entities <=> foreign key */
    /* ============================================================ */
    @ManyToOne
    private User user;
}
