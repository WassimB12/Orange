package com.example.orange.services;

import com.example.orange.entities.Email;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class expSer {

    public CompletableFuture<List<Email>> senderMailStatus(String mail) {
        List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();

        String[] directories = {
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01",
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02"
        };

        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            for (String directory : directories) {
                Path start = Paths.get(directory);
                Files.walk(start)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            executor.submit(() -> {
                                try {
                                    List<Email> emails = searchInFile(path, mail);
                                    synchronized (resultEmails) {
                                        resultEmails.addAll(emails);
                                    }
                                } catch (IOException e) {
                                    futureResult.completeExceptionally(e);
                                }
                            });
                        });
            }
        } catch (IOException e) {
            futureResult.completeExceptionally(e);
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                futureResult.completeExceptionally(e);
            }
        }

        futureResult.complete(resultEmails);
        return futureResult;
    }

    private static List<Email> searchInFile(Path path, String mail) throws IOException {
        List<Email> emails = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
        String regex = "QUEUE\\(\\[(.*?)\\]\\)";
        String ipv4Regex = "relayed via ((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})";

        Pattern pattern2 = Pattern.compile(regex);
        Pattern patternIP = Pattern.compile(ipv4Regex);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("from <") && line.contains(mail)) {
                    Email email = new Email();
                    email.setSender(mail);

                    Matcher matcherId = pattern2.matcher(line);

                    if (matcherId.find()) {
                        String id = matcherId.group(1);
                        email.setId(Integer.parseInt(id));
                    }

                    while ((line = reader.readLine()) != null && !line.contains("deleted")) {
                        if (line.contains("DEQUEUER")) {
                            Matcher matcher = pattern.matcher(line);
                            while (matcher.find()) {
                                email.setReceiver(matcher.group());
                            }
                        }

                        if (line.contains("relayed via")) {
                            email.setResult("Delivered");
                            Matcher matcherIP = patternIP.matcher(line);

                            if (matcherIP.find()) {
                                String ipAddress = matcherIP.group(1);
                                email.setCouloir(String.valueOf(searchIDinFiles("57117905")));
                            }
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

    private static String searchIDinFiles(String wordToSearch) {
        String directoryPath = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP02";
        String result = null;
        ExecutorService executor = Executors.newCachedThreadPool();

        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            result = paths.parallel()
                    .filter(Files::isRegularFile)
                    .map(path -> searchWordInFile(path, wordToSearch))
                    .filter(res -> res != null)
                    .findFirst()
                    .orElse("Word not found");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        return result;
    }

    private static String searchWordInFile(Path path, String wordToSearch) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(wordToSearch)) {
                    return path.getParent().getFileName().toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Word not found in this file
    }
}

