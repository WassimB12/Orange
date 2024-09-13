package com.example.orange.repository;

import com.example.orange.entities.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParameterRespository extends JpaRepository<Parameter, Integer> {
}
