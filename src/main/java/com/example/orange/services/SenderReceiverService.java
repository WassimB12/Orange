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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class SenderReceiverService {
    static Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    static Pattern patternSender = Pattern.compile("from <(.*?)>");
    static Pattern patternFESid = Pattern.compile("got:250 (.*?) message");
    static String regex = "QUEUE\\(\\[(.*?)\\]\\)";
    static String ipv4Regex = "relayed via ((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})";
    static String ipv4Regex2 = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";

    static Pattern pattern2 = Pattern.compile(regex);
    static Pattern patternIP = Pattern.compile(ipv4Regex);
    static Pattern patternIP2 = Pattern.compile(ipv4Regex2);

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static String[] getDirectoriesInTimeRange(String startTime, String endTime, String op, String couloir) {
        List<String> filteredDirectories = new ArrayList<>();
        String[] baseDirectories = {
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01\\",
                "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02\\"
        };


        if (Objects.equals(op, "couloirSearch")) {
            baseDirectories = new String[]{
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\" + couloir + "02\\",

            };
        } else if (Objects.equals(op, "couloirSearch2")) {
            baseDirectories = new String[]{
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\" + couloir + "02\\",
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\" + couloir + "01\\",

            };


        } else if (Objects.equals(op, "receiverSearch")) {
            baseDirectories = new String[]{
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX01",
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX02",
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX03",
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX04",
            };


        } else if (Objects.equals(op, "receiverFesSearch")) {
            baseDirectories = new String[]{
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\Log\\FES01",
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\Log\\FES02",

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
                                .toList();

                        for (int i = 0; i < fileNames.size(); i++) {
                            String fileName = fileNames.get(i);
                            LocalDateTime fileDateTime;
                            if (fileName.length() < 16) {
                                fileDateTime = LocalDateTime.parse(fileName.substring(0, 10) + "_00-00", fileNameFormatter);
                            } else {
                                fileDateTime = LocalDateTime.parse(fileName.substring(0, 16), fileNameFormatter);
                            }
                            LocalDateTime nextFileDateTime = null;
                            if (!op.equals("receiverSearch")) {
                                if (i == fileNames.size() - 1) {
                                    nextFileDateTime = LocalDateTime.parse(fileName.substring(0, 10) + "_23-59", fileNameFormatter);
                                } else {
                                    String nextFileName = fileNames.get(i + 1);
                                    nextFileDateTime = LocalDateTime.parse(nextFileName.substring(0, 16), fileNameFormatter);
                                }
                            }

                            boolean isFileInTimeRange = fileDateTime.isAfter(startDateTime) && fileDateTime.isBefore(endDateTime.plusMinutes(1));
                            boolean isNextFileAfterEndTime = nextFileDateTime == null || nextFileDateTime.isAfter(endDateTime);
//*/
                            if (Objects.equals(op, "logSearch") || Objects.equals(op, "receiverFesSearch")) {
                                if ((startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime))
                                        || (endDateTime.isAfter(fileDateTime) && endDateTime.isBefore(nextFileDateTime))) {
                                    filteredDirectories.add(Paths.get(directoryPath, fileName).toString());
                                }
                            } else if (Objects.equals(op, "couloirSearch") || Objects.equals(op, "couloirSearch2")) {
                                startDateTime = startDateTime.minusMinutes(20);
                                if (startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime)) {
                                    filteredDirectories.add(Paths.get(directoryPath, fileName).toString());
                                }

                            } else if (Objects.equals(op, "receiverSearch")) {
                                if (startDateTime.isAfter(fileDateTime)) {
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

    private static List<Email> searchInFesSender(Path path, String mail, String receiver) throws IOException {
        List<Email> emails = new ArrayList<>();
        boolean accountIsFullFound = false;
        boolean exceedsSizeLimitFound = false;


        // Parse the string into LocalDateTime

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {

                if (line.contains("from <") && line.contains(mail)) {
                    Email email = new Email();
                    Matcher matcher1 = patternSender.matcher(line);
                    if (matcher1.find()) {
                        String sender = matcher1.group(1);
                        email.setSender(sender);
                    }


                    String fileName = path.getFileName().toString().substring(0, Math.min(10, path.getFileName().toString().length()));
                    // Copy 11 characters from the actual line
                    String extractedLine = line.substring(0, Math.min(8, line.length()));
                    String dateString = (fileName + "T" + extractedLine);

                    LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);

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


                    while ((line = reader.readLine()) != null && !line.contains("QUEUE([" + email.getId() + "]) deleted")) {
                        if (line.contains("DEQUEUER [" + email.getId())) {
                            Matcher matcher = pattern.matcher(line);
                            while (matcher.find()) {
                                email.setReceiver(matcher.group());
                            }
                        }
                        if (line.contains("got:250")) {
                            email.setResult("Delivered");
                            int startIndex = line.indexOf("got:250") + "got:250".length();
                            int endIndex = line.indexOf("message");
                            if (endIndex != -1) {
                                String value = line.substring(startIndex, endIndex).trim();
                                // Remove spaces from the extracted value
                                value = value.replaceAll("\\s+", "");
                                email.setCouloirID(Integer.parseInt(value));
                            }
                        } else if (line.contains("[" + email.getId() + "] rule(Disc-Kaspersky-virus)")) {
                            email.setResult("Rejected(mail considered as a virus)");
                        } else if (line.contains("[" + email.getId() + "] message body rejected, got:579 message content is not acceptable here")) {
                            email.setResult("Rejected(mail content is not acceptable)");
                        } else if (line.contains("composed message exceeds the size limit")) {
                            email.setResult("Mail or attaechement exceed size limit");
                            exceedsSizeLimitFound = true;
                        } else if (//line.contains("DEQUEUER [" + email.getId() + "]") &&
                                line.contains("failed: account is full")) {
                            email.setResult("recipient inbox is full");
                            accountIsFullFound = true;
                        } else if (line.contains("[" + email.getId() + "] message body rejected, got:579 message content is not acceptable here")) {
                            email.setResult("Rejected(mail content is not acceptable)");

                        } else if (line.contains("DEQUEUER [" + email.getId() + "]") && line.contains("message discarded without processing")
                                && !accountIsFullFound &&
                                !exceedsSizeLimitFound) {
                            email.setResult("Rejected(Wrong mail adress)");
                        } else if (line.contains("nX-KAS-Level: [XXXXXXXX") && Objects.equals(email.getResult(), "Delivered"))
                        // if line contains more than 8 X check in couloir if message is discareded or not
                        {
                            String pattern = "yyyy-MM-dd'T'HH:mm:ss";

                            DateFormat df = new SimpleDateFormat(pattern);


                            String date = df.format(email.getDate());
                            email.setResult(searchCouloirIdExecutor
                                    (String.valueOf(email.getCouloirID()), date,
                                            ipAdressConclusion(email.getIPAdress()), 2));/* to be continued*/
                        } else if (line.contains("[" + email.getId() + "] stored on ")) {
                            email.setResult("Delivered");
                            Pattern pattern = Pattern.compile("\\[(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\]");
                            Matcher matcherIP2 = pattern.matcher(line);
                            if (matcherIP2.find()) {
                                String ipAddressBack = matcherIP2.group(1);
                                if (Objects.equals(ipAddressBack, "10.46.2.51")) {
                                    email.setIPAdress("10.46.2.51");
                                } else if (Objects.equals(ipAddressBack, "10.46.2.52")) {
                                    email.setIPAdress("10.46.2.52");
                                } else if (Objects.equals(ipAddressBack, "10.46.2.53")) {
                                    email.setIPAdress("10.46.2.53");
                                } else if (Objects.equals(ipAddressBack, "10.46.2.54")) {
                                    email.setIPAdress("10.46.2.54");
                                }


                            }
                        }


                        if (line.contains("relayed via")) {
                            email.setResult("Delivered");
                            Matcher matcherIP = patternIP.matcher(line);

                            if (matcherIP.find()) {
                                String ipAddress = matcherIP.group(1);
                                email.setIPAdress(ipAddress);

                            }
                        } else if (line.contains("undelivered")) {
                            email.setResult("Undelivered");
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

    static String ipAdressConclusion(String ipAddress) {
        if (ipAddress.equals("10.46.96.20")) {
            return "VIP";

        } else if (ipAddress.equals("10.46.96.21")) {
            return ("GP");

        } else if (ipAddress.equals("10.46.96.22")) {
            return ("ML");

        } else if (ipAddress.equals("10.46.96.13")) {
            return ("VIP");
        } else if (ipAddress.equals("10.46.96.14")) {
            return ("VIP");
        } else if (ipAddress.equals("10.46.96.15")) {
            return ("GP");
        } else if (ipAddress.equals("10.46.96.16")) {
            return ("GP");
        } else if (ipAddress.equals("10.46.96.17")) {
            return ("ML");
        } else if (ipAddress.equals("10.46.96.18")) {
            return ("ML");
        }
        return ipAddress;
    }

    private static String searchCouloirIdExecutor(String wordToSearch, String date, String couloir, int option) {
        String[] logDirectories = getDirectoriesInTimeRange(date, date, "couloirSearch", couloir);

// work on this
        System.out.println("CouloirFiles: " + Arrays.toString(logDirectories));
        String result = null;
        ExecutorService executor = Executors.newCachedThreadPool();
        if (option == 1) {
            try {
                for (String logDirectory : logDirectories) {
                    try (Stream<Path> paths = Files.walk(Paths.get(logDirectory))) {
                        result = paths.parallel()
                                .filter(Files::isRegularFile)
                                .map(path -> searchCouloirID(path, wordToSearch, date, couloir))
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
        }
        if (option == 2) {
            String[] logDirectories2 = getDirectoriesInTimeRange(date, date, "couloirSearch2", couloir);
            try {
                for (String logDirectory : logDirectories2) {
                    try (Stream<Path> paths = Files.walk(Paths.get(logDirectory))) {
                        result = paths.parallel()
                                .filter(Files::isRegularFile)
                                .map(path -> searchCouloirID2(path, wordToSearch))
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
        }

        return result;
    }

    private static String searchCouloirID(Path path, String wordToSearch, String date, String couloir) {
        String result = couloir + "01";

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            Boolean wordFound = false;
            while ((line = reader.readLine()) != null && (!wordFound)) {
                if (line.contains(wordToSearch)) {
                    wordFound = true;
                    result = couloir + "02";
                    break;


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result; // Word not found in this file
    }


    private static String searchCouloirID2(Path path, String couloirID) {
        String result = "Delivered";
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            Boolean wordFound = false;
            while ((line = reader.readLine()) != null && (!wordFound)) {
                if (line.contains("[" + couloirID + "] rule(discard")) {
                    wordFound = true;
                    result = "Rejected(Spam detected)";
                    break;


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result; // Word not found in this file
    }


    //********* Receiver Functions   **********//
    public static List<Email> readLog(Path path, String receiverMail, String word2) throws IOException {
        List<Email> emails = new ArrayList<>();
        boolean accountIsFullFound = false;
        boolean exceedsSizeLimitFound = false;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {

                if (line.contains("from <")) {
                    Email email = new Email();

                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        email.setSender(matcher.group());
                    }

                    Matcher matcherId = pattern2.matcher(line);

                    if (matcherId.find()) {
                        String id = matcherId.group(1);
                        email.setId(Integer.parseInt(id));
                    }

                    while ((line = reader.readLine()) != null && !line.contains("deleted")) {

                        if (line.contains("message accepted")) {
                            Matcher matcher1 = patternFESid.matcher(line);
                            if (matcher1.find()) {
                                String fesId = matcher1.group(1);
                                email.setCouloirID(Integer.parseInt(fesId));
                            }
                        }
                        if (line.contains("relayed via")) {
                            email.setResult("Delivered");
                        } else if (line.contains("rule(Disc-Kaspersky-spam)")) {
                            email.setResult("Rejected(mail considered as a spam)");
                        } else if (line.contains("composed message exceeds the size limit")) {
                            email.setResult("Mail or attaechement exceed size limit");
                            exceedsSizeLimitFound = true;
                        } else if (//line.contains("DEQUEUER [" + email.getId() + "]") &&
                                line.contains("failed: account is full")) {
                            email.setResult("recipient inbox is full");
                            accountIsFullFound = true;
                        } else if (line.contains("[" + email.getId() + "] message body rejected, got:579 message content is not acceptable here")) {
                            email.setResult("Rejected(mail content is not acceptable)");

                        }/* else if (line.contains("DEQUEUER [" + email.getId() + "]") && line.contains("message discarded without processing")
                                && !accountIsFullFound &&
                                !exceedsSizeLimitFound) {
                            email.setResult("Rejected(Wrong mail adress)");
                        }*/
                        if (line.contains("DEQUEUER") && line.contains(receiverMail)) {
                            matcher = pattern.matcher(line);
                            while (matcher.find()) {
                                email.setReceiver(matcher.group());
                            }
                            String fileName = path.getFileName().toString().substring(0, Math.min(10, path.getFileName().toString().length()));
                            // Copy 11 characters from the actual line
                            String extractedLine = line.substring(0, Math.min(8, line.length()));
                            String dateString = (fileName + "T" + extractedLine);

                            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                            email.setDate(Date.from(dateTime.atZone(ZoneId.of("UTC+1")).toInstant()));
                            email.setFes(path.getParent().getFileName().toString());

                        }


                    }

                    if (Objects.equals(email.getReceiver(), receiverMail)) {

                        // email.setCouloir(executorFesReceiver(email.getId(), String.valueOf(email.getDate())));
                        searchFesIdMXExecutor(email, email.getDate());


                        emails.add(email);

                    }

                }
            }
        }

        return emails;


    }

    static void CheckFesForReceiverMails(Email email, Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("QUEUE([" + email.getId() + "])") && line.contains("from")) {
                    while (!line.contains("QUEUE([" + email.getId() + "]) deleted")) {
                        if (line.contains("[" + email.getId() + "] stored on ")) {
                            email.setResult("Delivered client POP");
                            Pattern pattern = Pattern.compile("\\[(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\]");
                            Matcher matcherIP2 = pattern.matcher(line);
                            if (matcherIP2.find()) {
                                String ipAddressBack = matcherIP2.group(1);
                                if (Objects.equals(ipAddressBack, "10.46.2.51")) {
                                    email.setCouloir("BE01");
                                } else if (Objects.equals(ipAddressBack, "10.46.2.52")) {
                                    email.setCouloir("BE02");
                                } else if (Objects.equals(ipAddressBack, "10.46.2.53")) {
                                    email.setCouloir("BE03");
                                } else if (Objects.equals(ipAddressBack, "10.46.2.54")) {
                                    email.setCouloir("BE04");
                                }


                            }
                        } else if (line.contains("relayed via")) {
                            email.setResult("Delivered client SMTP");
                        }


                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void searchFesIdMXExecutor(Email email, Date d1) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";

        DateFormat df = new SimpleDateFormat(pattern);


        String date = df.format(d1);

        String[] directories = getDirectoriesInTimeRange(date, date, "receiverFesSearch", "none");

// work on this
        //  System.out.println("CouloirFiles: " + Arrays.toString(logDirectories));
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            for (String logDirectory : directories) {
                try (Stream<Path> paths = Files.walk(Paths.get(logDirectory))) {
                    paths.parallel()
                            .filter(Files::isRegularFile)
                            .forEach(path -> searchFesIdMX(path, email));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

    }


    private static void searchFesIdMX(Path path, Email email) {


        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(String.valueOf(email.getCouloirID())) &&
                        line.contains("QUEUE([" + email.getCouloirID() + "])") &&
                        !line.contains("QUEUE([" + email.getCouloirID() + "]) deleted")) {
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("[" + email.getCouloirID() + "] rule(Disc-Kaspersky-virus)")) {
                            email.setResult("Rejected(mail considered as a virus)");
                        }

                        if (line.contains("[" + email.getCouloirID() + "] message body rejected, got:579 message content is not acceptable here")) {
                            email.setResult("Rejected(mail content is not acceptable)");
                        }

                        if (line.contains("DEQUEUER [" + email.getCouloirID() + "] ") && line.contains(" message discarded without processing")) {
                            email.setResult("Rejected(Wrong mail adress)");
                        }

                        if (line.contains("[" + email.getCouloirID() + "] stored on ")) {
                            email.setResult("Delivered client POP");
                            Pattern pattern = Pattern.compile("\\[(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\]");
                            Matcher matcherIP2 = pattern.matcher(line);
                            if (matcherIP2.find()) {
                                String ipAddressBack = matcherIP2.group(1);
                                switch (ipAddressBack) {
                                    case "10.46.2.51":
                                        email.setIPAdress("BE01");
                                        break;
                                    case "10.46.2.52":
                                        email.setIPAdress("BE02");
                                        break;
                                    case "10.46.2.53":
                                        email.setIPAdress("BE03");
                                        break;
                                    case "10.46.2.54":
                                        email.setIPAdress("BE04");
                                        break;
                                }
                            }
                        }
                        if (line.contains("relayed via") && line.contains("DEQUEUER [" + email.getCouloirID() + "]")) {
                            email.setResult("Delivered client SMTP");
                        }
                    }
                    email.setCouloir(path.getParent().getFileName().toString());
                    return; // Exit the method after finding and setting the couloir
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
// Word not found in this file

        // Word not found in this file
    }


    public CompletableFuture<List<Email>> senderMailStatus(String mail, String receiver, String d1, String d2) {
        List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();
        String[] directories = getDirectoriesInTimeRange(d1, d2, "logSearch", "none");
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
                                    List<Email> emails = searchInFesSender(path, mail, receiver);
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

    public CompletableFuture<List<Email>> checkReceiver(String receiverMail, String d1, String d2) {
        List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();

        String[] directories = getDirectoriesInTimeRange(d1, d2, "receiverSearch", "none"); //@@@
        System.out.println("Directories: " + Arrays.toString(directories));

        String word2 = "from <";

        ExecutorService executor = Executors.newCachedThreadPool();
        // to review this directory filtring process and compare it to the fesSearcher
        try {
            for (String directory : directories) {
                Path start = Paths.get(directory);
                Files.walk(start)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            executor.submit(() -> {
                                try {
                                    List<Email> emails = readLog(path, receiverMail, word2);
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

    public String searchCouloirs(String ipAddress, int couloirID, String date) {

        String formatedDate = date.substring(0, 16);


        if (ipAddress.equals("10.46.96.20")) {
            return (searchCouloirIdExecutor(String.valueOf(couloirID), formatedDate, "VIP", 1));

        } else if (ipAddress.equals("10.46.96.21")) {
            return (searchCouloirIdExecutor(String.valueOf(couloirID), formatedDate, "GP", 1));

        } else if (ipAddress.equals("10.46.96.22")) {
            return (searchCouloirIdExecutor(String.valueOf(couloirID), formatedDate, "ML", 1));

        } else if (ipAddress.equals("10.46.96.13")) {
            return ("VIP01");
        } else if (ipAddress.equals("10.46.96.14")) {
            return ("VIP02");
        } else if (ipAddress.equals("10.46.96.15")) {
            return ("GP01");
        } else if (ipAddress.equals("10.46.96.16")) {
            return ("GP02");
        } else if (ipAddress.equals("10.46.96.17")) {
            return ("ML01");
        } else if (ipAddress.equals("10.46.96.18")) {
            return ("ML02");
        } else if (ipAddress.equals("10.46.2.51")) {
            return ("BE01");
        } else if (ipAddress.equals("10.46.2.52")) {
            return ("BE02");
        } else if (ipAddress.equals("10.46.2.53")) {
            return ("BE03");
        } else if (ipAddress.equals("10.46.2.54")) {
            return ("BE04");
        } else {
            return "CouloirId unknown: " + ipAddress;
        }

    }
}




  /*  public static String executorFesReceiver(int id, String d1) throws IOException {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";

        DateFormat df = new SimpleDateFormat(pattern);


        String date = df.format(d1);
        //String[] directories = getDirectoriesInTimeRange(date, date, "receiverFesSearch", "none");
        String[] directories = new String[]{
                "C:\\Users\\wassi\\OneDrive\\Bureau\\Log\\FES01"};
        System.out.println("Receiver Fes Search Directories: " + Arrays.toString(directories));

        String result = null;
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            for (String Directory : directories) {
                try (Stream<Path> paths = Files.walk(Paths.get(Directory))) {
                    result = paths.parallel()
                            .filter(Files::isRegularFile)
                            .map(path -> searchWordInFesFiles(path, String.valueOf(id), date))
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

    private static String searchWordInFesFiles(Path path, String wordToSearch, String date) {
        String result = "Not found";

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            Boolean wordFound = false;
            while ((line = reader.readLine()) != null && (!wordFound)) {
                if (line.contains(wordToSearch)) {
                    wordFound = true;
                    result = path.getParent().getFileName().toString();
                    break;


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result; // Word not found in this file
    }*/



