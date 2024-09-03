package com.example.orange.services;

import com.example.orange.entities.Parameter;
import com.example.orange.repository.ParameterRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParameterService {

    @Autowired
    private ParameterRespository parameterRepository;

    public Parameter updateParameter(Long id, Parameter updatedParameter) {
        Parameter existingParameter = parameterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parameter not found with id: " + id));

        // Update the existing parameter with the new values
        existingParameter.setTime(updatedParameter.getTime());
        existingParameter.setEmail(updatedParameter.getEmail());
        existingParameter.setPc(updatedParameter.getPc());

        return parameterRepository.save(existingParameter);
    }

    public Parameter getParameter(Long id) {
        return parameterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parameter not found with id: " + id));
    }
}