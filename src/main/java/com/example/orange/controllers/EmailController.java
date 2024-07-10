package com.example.orange.controllers;

import com.example.orange.entities.Email;
import com.example.orange.services.LogDetails;
import com.example.orange.services.SenderReceiverService;
import com.example.orange.services.expSer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
                                                       String mail, @PathVariable("id2")
                                                       String mail2, @PathVariable("d1") String d1, @PathVariable("d2") String d2) {

        return senderReceiverService.senderMailStatus(mail, mail2, d1, d2);
    }

    @GetMapping("/test/{id}/{id2}/{d1}/{d2}")
    public CompletableFuture<List<Email>> test(@PathVariable("id")
                                               String mail, @PathVariable("id2")
                                               String mail2, @PathVariable("d1") String d1, @PathVariable("d2") String d2) {

        return expSer.senderMailStatus(mail, mail2, d1, d2);
    }

    @GetMapping("/log/{id}/{op}/{ipAd}/{date}")

    public String logD(@PathVariable("id") String id, @PathVariable("op") int op,
                       @PathVariable("ipAd") String ipAdress, @PathVariable("date") String date) throws IOException, ExecutionException, InterruptedException {

        return logDetails.searchLogInFiles(id, op, ipAdress, date);
    }


    @GetMapping("/receiver/{id}/{d1}/{d2}")

    public CompletableFuture<List<Email>> receiverStatus(@PathVariable("id")
                                                         String mail, @PathVariable("d1") String d1, @PathVariable("d2") String d2) {

        return senderReceiverService.checkReceiver(mail, d1, d2);
    }


}
