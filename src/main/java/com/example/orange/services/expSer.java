package com.example.orange.services;

import com.example.orange.entities.Email;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class expSer {

    public List<Email> senderMailStatus(String mail) {
        List<Email> resultEmails = new ArrayList<>();

        String directory = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01";
        String directory2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02";

        Path start = Paths.get(directory);
        Path start2 = Paths.get(directory2);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            Stream<Path> filesStream = Stream.concat(
                    Files.walk(start),
                    Files.walk(start2));
            filesStream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        executor.submit(() -> {
                            try {
                                List<Email> emails = searchInFile(path, mail);
                                synchronized (resultEmails) {
                                    resultEmails.addAll(emails);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return resultEmails;
    }

    private static List<Email> searchInFile(Path path, String mail) throws IOException {
        List<Email> emails = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
        String regex = "QUEUE\\(\\[(.*?)\\]\\)";
        Pattern pattern2 = Pattern.compile(regex);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("from <") && line.contains(mail)) {
                    Email email = new Email();
                    email.setSender(mail);

                    Matcher matcherId = pattern2.matcher(line);

                    if (matcherId.find()) {
                        String id = matcherId.group(1);
                        email.setId(Integer.parseInt(id));}

                    while ((line = reader.readLine()) != null && !line.contains("deleted")) {
                        if (line.contains("DEQUEUER")) {
                            Matcher matcher = pattern.matcher(line);
                            while (matcher.find()) {
                                email.setReceiver(matcher.group());
                            }
                        }

                        if (line.contains("relayed via")) {
                            email.setResult("Delivered");
                        } else if (line.contains("undelivered")) {
                            email.setResult("Undelivered");
                        } else if (line.contains("blocked")) {
                            email.setResult("Blocked");
                        }
                    }
                    emails.add(email);
                }
            }
        }

        return emails;
    }
}
