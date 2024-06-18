package com.example.orange.services;

import com.example.orange.entities.Email;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class expSer {

    private static String[] getDirectoriesInTimeRange(String startTime, String endTime, String op) {
        List<String> filteredDirectories = new ArrayList<>();
        String[] baseDirectories = {
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01\\",
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02\\"
        };


        if (Objects.equals(op, "couloirSearch")) {
            baseDirectories = new String[]{
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP02\\",

            };


        }


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
//*/
                            if (Objects.equals(op, "logSearch")) {
                                if ((startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime))
                                        || (endDateTime.isAfter(fileDateTime) && endDateTime.isBefore(nextFileDateTime))) {
                                    filteredDirectories.add(Paths.get(directoryPath, fileName).toString());
                                }
                            } else if (Objects.equals(op, "couloirSearch")) {
                                if (startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime)) {
                                    filteredDirectories.add(Paths.get(directoryPath, fileName).toString());


                                }

                            }
                        }
                    } catch (NoSuchFileException e) {
                        // Handle case where file does not exist
                        System.err.println("File does not exist: " + e.getFile());
                    } catch (IOException e) {
                        e.printStackTrace();
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

    private static List<Email> searchInFile(Path path, String mail, String receiver) throws IOException {
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

                if (line.contains("from <") && line.contains(mail)) {
                    String fileName = path.getFileName().toString().substring(0, Math.min(10, path.getFileName().toString().length()));
                    // Copy 11 characters from the actual line
                    String extractedLine = line.substring(0, Math.min(8, line.length()));
                    String dateString = (fileName + "T" + extractedLine);

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
                                SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

                                String formatedDate = formatter2.format(email.getDate());
                                email.setCouloir(findCouloir(String.valueOf(email.getCouloirID())));
                                //email.setCouloir(searchIDinFiles(String.valueOf(email.getCouloirID()), formatedDate));
                            }
                        } else if (line.contains("undelivered")) {
                            email.setResult("Undelivered");
                        } else if (line.contains("blocked")) {
                            email.setResult("Blocked");
                        }
                    }
                    if (receiver.equals("all")) {
                        emails.add(email);
                    } else if (Objects.equals(email.getReceiver(), receiver)) {
                        emails.add(email);
                    }
                }
            }
        }

        return emails;
    }

    private static String searchIDinFiles(String wordToSearch, String date) {
        String[] logDirectories = getDirectoriesInTimeRange(date, date, "couloirSearch");


        System.out.println("CouloirFiles: " + Arrays.toString(logDirectories));
        String result = null;
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            for (String logDirectory : logDirectories) {
                try (Stream<Path> paths = Files.walk(Paths.get(logDirectory))) {
                    result = paths.parallel()
                            .filter(Files::isRegularFile)
                            .map(path -> searchWordInFile(path, wordToSearch, date))
                            .filter(res -> res != null)
                            .findFirst()
                            .orElse("id not found");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        return result;
    }

    private static String searchWordInFile(Path path, String wordToSearch, String date) {
        String result = "VIP01";

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            Boolean wordFound = false;
            while ((line = reader.readLine()) != null && (!wordFound)) {
                if (line.contains(wordToSearch)) {
                    wordFound = true;
                    result = "VIP02";
                    break;


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result; // Word not found in this file
    }

    private static String findCouloir(String wordToSearch) {
        Path directory = Path.of("C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP02\\");
        String couloirName;

        // Create an executor service with a single thread
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // Start searching for the word in files
            Future<Path> result = findWordInFiles(executor, directory, wordToSearch);

            // Wait for the result
            Path foundFilePath = result.get();

            // If the result is not null, print the file path where the word was found
            if (foundFilePath != null) {
                System.out.println("Word '" + wordToSearch + "' found in file: " + foundFilePath);
                couloirName = "VIP02";
            } else {
                System.out.println("Word '" + wordToSearch + "' not found in any file.");
                couloirName = "VIP01";
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // Shutdown the executor service
            executor.shutdown();
        }
        return couloirName;
    }

    public static CompletableFuture<Path> findWordInFiles(ExecutorService executor, Path directory, String wordToSearch) {
        // Create a list to hold the futures for each file search task
        List<CompletableFuture<Path>> futures = new ArrayList<>();

        // Iterate over the files in the directory
        try {
            Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        // Submit a task to search for the word in each file
                        CompletableFuture<Path> future = CompletableFuture.supplyAsync(() -> searchWordInFile(file, wordToSearch), executor);
                        futures.add(future);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Combine all the futures into a single future that completes when any of the futures complete
        CompletableFuture<Path>[] futureArray = futures.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(futureArray);

        // Return a future that is completed when any of the file search tasks is completed
        return allOfFuture.thenApply(ignoredVoid -> {
            for (CompletableFuture<Path> future : futureArray) {
                if (!future.isCompletedExceptionally() && future.getNow(null) != null) {
                    return future.getNow(null);
                }
            }
            return null;
        });
    }

    public static Path searchWordInFile(Path file, String wordToSearch) {
        // Search for the word in the file
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            Boolean res = false;
            while ((line = reader.readLine()) != null && !res) {
                if (line.contains(wordToSearch)) {
                    res = true;
                    // If the word is found, return the file path
                    return file;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If the word is not found, return null
        return null;
    }

    public CompletableFuture<List<Email>> senderMailStatus(String mail, String receiver, String d1, String d2) {
        List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();
        String[] directories = getDirectoriesInTimeRange(d1, d2, "logSearch");
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
                                    List<Email> emails = searchInFile(path, mail, receiver);
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
}






