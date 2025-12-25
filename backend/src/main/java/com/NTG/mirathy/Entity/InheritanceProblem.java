package com.NTG.mirathy.Entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inheritance_problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InheritanceProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String title;


    private Double totalValue;



    @CreationTimestamp
    private LocalDateTime createdAt;

    private Boolean isFavorite = false;

    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
