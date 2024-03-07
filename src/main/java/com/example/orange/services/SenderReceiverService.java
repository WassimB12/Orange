package com.example.orange.services;

import com.example.orange.entities.Email;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SenderReceiverService {

    public  CompletableFuture<List<Email>> senderMailStatus(String mail, String receiver,String d1, String d2)
    { List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();
        String[] directories = getDirectoriesInTimeRange(d1, d2);
        System.out.println("Directories: " + Arrays.toString(directories));
       /* String[] directories = {
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01",
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02"
        };*/
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            for (String directory : directories) {
                Path start = Paths.get(directory);
                Files.walk(start)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            executor.submit(() -> {
                                try {
                                    List<Email> emails = searchInFile(path, mail,receiver);
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

    private static String[] getDirectoriesInTimeRange(String startTime, String endTime) {
        List<String> filteredDirectories = new ArrayList<>();
        String[] baseDirectories = {
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01\\",
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02\\"
        };

        try {
            // Parse start and end times
            LocalDateTime startDateTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endDateTime = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Iterate through the date range
            for (String baseDirectory : baseDirectories) {
                LocalDate currentDate = startDateTime.toLocalDate();
                DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
                while (!currentDate.isAfter(endDateTime.toLocalDate())) {
                    String dateDirectoryName = currentDate.toString();
                    String directoryPath = baseDirectory;

                    try (Stream<Path> paths = Files.list(Paths.get(directoryPath))) {
                        List<String> fileNames = paths.filter(Files::isRegularFile)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .sorted() // Sort the file names to ensure they are in chronological order
                                .collect(Collectors.toList());

                        for (int i = 0; i < fileNames.size(); i++) {
                            String fileName = fileNames.get(i);
                            LocalDateTime fileDateTime;
                            if (fileName.length() < 16) {
                                fileDateTime = LocalDateTime.parse(fileName.substring(0, 10) + "_00-00", fileNameFormatter);
                            } else {
                                fileDateTime = LocalDateTime.parse(fileName.substring(0, 16), fileNameFormatter);
                            }
                            LocalDateTime nextFileDateTime = null;

                            if (i == fileNames.size() - 1) {
                                nextFileDateTime = LocalDateTime.parse(fileName.substring(0, 10) + "_23-59", fileNameFormatter);
                            } else {
                                String nextFileName = fileNames.get(i + 1);
                                nextFileDateTime = LocalDateTime.parse(nextFileName.substring(0, 16), fileNameFormatter);
                            }

                            boolean isFileInTimeRange = fileDateTime.isAfter(startDateTime) && fileDateTime.isBefore(endDateTime.plusMinutes(1));
                            boolean isNextFileAfterEndTime = nextFileDateTime == null || nextFileDateTime.isAfter(endDateTime);

                            if ((startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime))
                                    || (endDateTime.isAfter(fileDateTime) && endDateTime.isBefore(nextFileDateTime))) {
                                filteredDirectories.add(Paths.get(directoryPath, fileName).toString());
                            }
                        }
                    } catch (NoSuchFileException e) {
                        // Handle case where file does not exist
                        System.err.println("File does not exist: " + e.getFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        // Close any resources here if necessary
                    }
                    currentDate = currentDate.plusDays(1); // Move to the next day
                }
            }
        } catch (DateTimeParseException e) {
            // Handle parsing or I/O exception
            e.printStackTrace();
        }
        return filteredDirectories.toArray(new String[0]);
    }


    private static List<Email> searchInFile(Path path, String mail,String receiver) throws IOException {
        List<Email> emails = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
        String regex = "QUEUE\\(\\[(.*?)\\]\\)";
        String ipv4Regex = "relayed via ((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})";

        Pattern pattern2 = Pattern.compile(regex);
        Pattern patternIP = Pattern.compile(ipv4Regex);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // Parse the string into LocalDateTime

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {

                if (line.contains("from <") && line.contains(mail) ) {
                    String fileName = path.getFileName().toString().substring(0, Math.min(10, path.getFileName().toString().length()));
                    // Copy 11 characters from the actual line
                    String extractedLine = line.substring(0, Math.min(8, line.length()));
                    String dateString=(fileName+"T"+extractedLine);

                    LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                    Email email = new Email();
                    email.setSender(mail);
                    email.setFes(path.getParent().getFileName().toString());

                  /*
                    Instant instant = dateTime.toInstant(ZoneOffset.UTC);
                    email.setDate(Date.from(instant));*/

                    email.setDate(Date.from(dateTime.atZone(ZoneId.of("UTC+1")).toInstant()));
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
                        if (line.contains("got:250")) {
                            int startIndex = line.indexOf("got:250") + "got:250".length();
                            int endIndex = line.indexOf("message");
                            if (endIndex != -1) {
                                String value = line.substring(startIndex, endIndex).trim();
                                // Remove spaces from the extracted value
                                value = value.replaceAll("\\s+", "");
                                email.setCouloirID(Integer.parseInt(value));
                            }
                        }


                        if (line.contains("relayed via")) {
                            email.setResult("Delivered");
                            Matcher matcherIP = patternIP.matcher(line);

                            if (matcherIP.find()) {
                                String ipAddress = matcherIP.group(1);
                                email.setCouloir(ipAddress);
                                //String.valueOf(searchIDinFiles("57117905")));
                            }
                        } else if (line.contains("undelivered")) {
                            email.setResult("Undelivered");
                        } else if (line.contains("blocked")) {
                            email.setResult("Blocked");
                        }
                    } if(receiver.equals("none")) {emails.add(email);}
                        else if (Objects.equals(email.getReceiver(), receiver)){
                            emails.add(email);}
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
                    .orElse("id not found");
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






