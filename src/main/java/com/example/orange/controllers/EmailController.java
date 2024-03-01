package com.example.orange.controllers;

import com.example.orange.entities.Email;
import com.example.orange.services.LogService;
import com.example.orange.services.expSer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = "*")


public class EmailController {
    @Autowired
    LogService logService;
    @Autowired
    expSer expSer;

    // Read operation
    @GetMapping("/senderstauts/{id}")  //    @GetMapping("/senderstauts/{id}/{d1}/{d2}")

    @Cacheable("emails")

    public CompletableFuture<List<Email>> senderStatus(@PathVariable("id")
             String mail/*,@PathVariable("d1") String d1,@PathVariable("d2")String d2*/)
    {

        return logService.senderMailStatus(mail);//,d1,d2);
    }
    @GetMapping("/test/{id}")

    public CompletableFuture<List<Email>> test(@PathVariable("id") String mail)
    {

        return expSer.senderMailStatus(mail);
    }


}
