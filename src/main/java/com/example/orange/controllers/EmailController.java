package com.example.orange.controllers;

import com.example.orange.entities.Email;
import com.example.orange.services.LogService;
import com.example.orange.services.expSer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")


public class EmailController {
    @Autowired
    LogService logService;
    @Autowired
    expSer expSer;

    // Read operation
    @GetMapping("/senderstauts/{id}")
    @Cacheable("emails")

    public List<Email> senderStatus(@PathVariable("id") String mail)
    {

        return logService.senderMailStatus(mail);
    }
    @GetMapping("/test/{id}")

    public List<Email> test(@PathVariable("id") String mail)
    {

        return expSer.senderMailStatus(mail);
    }


}
