package com.insurance.dashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "policy_holders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @Column(unique = true)
    private String email;

    private String phone;

    private LocalDate dateOfBirth;

    private String address;

    @OneToMany(mappedBy = "policyHolder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Policy> policies;
}
