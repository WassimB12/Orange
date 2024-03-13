package com.example.orange.controllers;

import com.example.orange.entities.Email;
import com.example.orange.services.LogDetails;
import com.example.orange.services.SenderReceiverService;
import com.example.orange.services.expSer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@CrossOrigin(origins = "*")


public class EmailController {
    @Autowired
    SenderReceiverService senderReceiverService;
    @Autowired
    LogDetails logDetails;
    @Autowired
    expSer expSer;

    // Read operation
      @GetMapping("/send/{id}/{id2}/{d1}/{d2}")

    @Cacheable("emails")

    public CompletableFuture<List<Email>> senderStatus(@PathVariable("id")
             String mail,@PathVariable("id2")
      String mail2,@PathVariable("d1") String d1,@PathVariable("d2")String d2)
    {

        return senderReceiverService.senderMailStatus(mail,mail2,d1,d2);
    }
    @GetMapping("/test/{id}/{id2}/{d1}/{d2}")
    public CompletableFuture<List<Email>> test(@PathVariable("id")
                                                   String mail,@PathVariable("id2")
                                                   String mail2,@PathVariable("d1") String d1,@PathVariable("d2")String d2)
    {

        return expSer.senderMailStatus(mail,mail2,d1,d2);
    }

    @GetMapping("/log/{id}")

    public String log(@PathVariable("id") String id) throws IOException, ExecutionException, InterruptedException {

        return logDetails.searchLogInFiles(id);
    }

}
