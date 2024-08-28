package com.example.orange.controllers;

import com.example.orange.services.ScreenshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ScreenshotController {
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotController.class);
    @Autowired
    ScreenshotService screenshotService;

    @GetMapping("/screen/{mail}/{receiver}/{op}")
    public ResponseEntity<?> sendMail(@PathVariable("mail") String mail,
                                      @PathVariable("receiver") String receiver,
                                      @PathVariable int op) {
        try {
            screenshotService.runTask(mail, receiver, op);
            return ResponseEntity.ok("Mail delivered successfully");
        } catch (Exception e) {
            logger.error("Error delivering mail", e);
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }
}
