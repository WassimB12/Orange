package com.example.orange.controllers;


import com.example.orange.entities.DomainList;
import com.example.orange.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")

@RestController
@RequestMapping("/domain")
public class DomainController {

    @Autowired
    private DomainRepository domainListRepository;

    @PostMapping("/add")
    public DomainList addDomain(@RequestBody DomainList domainList) {
        return domainListRepository.save(domainList);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDomain(@PathVariable int id) {
        domainListRepository.deleteById(id);
    }

    @PutMapping("/update")
    public DomainList updateDomain(@RequestBody DomainList updatedDomain) {
        return domainListRepository.save(updatedDomain);
    }

    @GetMapping("/get/{id}")
    public DomainList getDomainById(@PathVariable int id) {
        return domainListRepository.findById(id).orElse(null);
    }

    @GetMapping("/get-all")
    public List<DomainList> getAllDomains() {
        return domainListRepository.findAll();
    }
}