package com.example.orange.controllers;

import com.example.orange.services.ScreenshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ScreenshotController {
    @Autowired
    ScreenshotService screenshotService;

    @GetMapping("/screen/{mail}/{receiver}/{op}")


    public ResponseEntity<?> sendMail(@PathVariable("mail")
                                      String mail, @PathVariable("receiver") String receiver, @PathVariable int op) {

        try {
            screenshotService.runTask(mail, receiver, op);
            return ResponseEntity.ok("mail delivered succesfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}