package com.example.orange.controllers;

import com.example.orange.entities.Parameter;
import com.example.orange.services.ParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parameter")
public class ParameterController {

    @Autowired
    private ParameterService parameterService;

    @PutMapping("/update/{id}")
    public Parameter updateParameter(@PathVariable Integer id, @RequestBody Parameter parameter) {
        // Implement logic to update the Parameter entity with the given id
        return parameterService.updateParameter(id, parameter);
    }

    @GetMapping("/get/{id}")
    public Parameter getParameter(@PathVariable Integer id) {
        // Implement logic to retrieve the Parameter entity with the given id
        return parameterService.getParameter(id);
    }
}