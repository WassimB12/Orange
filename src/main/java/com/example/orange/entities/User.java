package com.example.orange.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter

@Getter

@AllArgsConstructor
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 30, message = "Last name must be between 2 and 30 characters")
    private String lastName;

    @NotBlank(message = "Email is mandatory")
    private String email;
    @NotBlank(message = "employeeId is mandatory")

    private String employeId;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
    private String role;


    public User() {

    }
}
