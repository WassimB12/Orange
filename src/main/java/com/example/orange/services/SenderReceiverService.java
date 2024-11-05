package com.example.orange.services;

import com.example.orange.entities.Email;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SenderReceiverService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    static Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    static Pattern patternSender = Pattern.compile("from <(.*?)>");
    static Pattern patternFESid = Pattern.compile("got:250 (.*?) message");
    static String regex = "QUEUE\\(\\[(.*?)\\]\\)";
    static Pattern patternID = Pattern.compile("DEQUEUER \\[(\\d+)\\]");
    static int batchSize = 1000;
    static String ipv4Regex = "relayed via ((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})";
    static String ipv4Regex2 = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
    static Pattern pattern2 = Pattern.compile(regex);
    static Pattern patternIP = Pattern.compile(ipv4Regex);
    static Pattern patternIP2 = Pattern.compile(ipv4Regex2);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String[] getDirectoriesInTimeRange
            (String startTime, String endTime, String op, String couloir) {
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
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01",
                    "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02",
            };
        }

        try {
            // Parse start and end times
            LocalDateTime startDateTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endDateTime = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime nextFileDateTime = null;
            DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

            // Iterate through the date range
            for (String baseDirectory : baseDirectories) {
                try (Stream<Path> paths = Files.list(Paths.get(baseDirectory))) {
                    List<String> fileNames = paths.filter(Files::isRegularFile)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .sorted() // Sort the file names to ensure they are in chronological order
                            .toList();
                    Map<String, LocalDateTime> firstFileDateTimeMap = new HashMap<>();
                    Map<String, LocalDateTime> lastFileDateTimeMap = new HashMap<>();

// Pass 1: Identify the first and last file for each date
                    for (String fileName : fileNames) {
                        LocalDateTime fileDateTime = parseFileDateTime(fileName);
                        String fileDate = fileName.substring(0, 10); // Extract the date part (yyyy-MM-dd)

                        if (!firstFileDateTimeMap.containsKey(fileDate) || fileDateTime.isBefore(firstFileDateTimeMap.get(fileDate))) {
                            firstFileDateTimeMap.put(fileDate, fileDateTime);
                        }
                        if (!lastFileDateTimeMap.containsKey(fileDate) || fileDateTime.isAfter(lastFileDateTimeMap.get(fileDate))) {
                            lastFileDateTimeMap.put(fileDate, fileDateTime);
                        }
                    }

// Pass 2: Process each file and adjust the first and last file of the day
                    for (int i = 0; i < fileNames.size(); i++) {
                        String fileName = fileNames.get(i);
                        LocalDateTime fileDateTime = parseFileDateTime(fileName);
                        String fileDate = fileName.substring(0, 10); // Extract the date part (yyyy-MM-dd)

                        if (fileDateTime.equals(firstFileDateTimeMap.get(fileDate))) {
                            // First file of the day
                            nextFileDateTime = LocalDateTime.parse(fileDate + "_00-00", fileNameFormatter);
                        } else if (fileDateTime.equals(lastFileDateTimeMap.get(fileDate))) {
                            // Last file of the day
                            nextFileDateTime = LocalDateTime.parse(fileDate + "_23-59", fileNameFormatter);
                        } else {
                            // Average file
                            String nextFileName = fileNames.get(i + 1);
                            if (nextFileName.length() >= 16) {
                                nextFileDateTime = LocalDateTime.parse(nextFileName.substring(0, 16), fileNameFormatter);
                            } else {
                                throw new IllegalArgumentException("Invalid nextFileName format: " + nextFileName);
                            }
                        }

                        boolean isFileInTimeRange = (fileDateTime != null)
                                && ((fileDateTime.isAfter(startDateTime) && fileDateTime.isBefore(endDateTime.plusMinutes(1)))
                                || (fileDateTime.isEqual(startDateTime) || fileDateTime.isEqual(endDateTime))
                                || (startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime))
                                || (endDateTime.isAfter(fileDateTime) && endDateTime.isBefore(nextFileDateTime)));

                        Path path = Paths.get(baseDirectory, fileName);
                        if (Objects.equals(op, "logSearch")) {
                            if (isFileInTimeRange) {
                                filteredDirectories.add(path.toString());
                            }

                        } else {
                            if (!Objects.equals(op, "receiverSearch")) {
                                if (i == fileNames.size() - 1) {
                                    nextFileDateTime = LocalDateTime.parse(fileName.substring(0, 10) + "_23-59", DATE_TIME_FORMAT);
                                } else {
                                    String nextFileName = fileNames.get(i + 1);
                                    nextFileDateTime = parseFileDateTime(nextFileName);
                                }
                            }

                            if (Objects.equals(op, "receiverFesSearch")) {
                                if ((startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime))
                                        || (endDateTime.isAfter(fileDateTime) && endDateTime.isBefore(nextFileDateTime))) {
                                    filteredDirectories.add(path.toString());
                                }
                            } else if (Objects.equals(op, "couloirSearch2") || Objects.equals(op, "couloirSearch")) {
                                /* startDateTime = startDateTime.minusMinutes(20);*/
                                if ((startDateTime.isAfter(fileDateTime) && startDateTime.isBefore(nextFileDateTime))
                                        || (endDateTime.isAfter(fileDateTime) && endDateTime.isBefore(nextFileDateTime))) { // TO REVIEW
                                    filteredDirectories.add(path.toString());
                                }
                            } else if (Objects.equals(op, "receiverSearch")) {
                                if (startDateTime.isAfter(fileDateTime)) {
                                    filteredDirectories.add(path.toString());
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filteredDirectories.toArray(new String[0]);
    }

    private static LocalDateTime parseFileDateTime(String fileName) {
        try {
            if (fileName.length() >= 16) {
                return LocalDateTime.parse(fileName.substring(0, 16), DATE_TIME_FORMAT);
            } else if (fileName.length() >= 10) {
                return LocalDate.parse(fileName.substring(0, 10), DATE_FORMAT).atStartOfDay();
            } else {
                return null; // Invalid format
            }
        } catch (Exception e) {
            return null; // Parsing failed, return null
        }
    }


    public static List<Email> searchInFesSender(Path path, String mail, String receiver) throws IOException {
        List<Email> emails = new ArrayList<>();
        List<Email> emailsToProcess = new ArrayList<>();

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
                    String extractedLine = line.substring(0, Math.min(8, line.length()));
                    String dateString = fileName + "T" + extractedLine;
                    LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                    email.setFes(path.getParent().getFileName().toString());
                    email.setDate(Date.from(dateTime.atZone(ZoneId.of("UTC+1")).toInstant()));

                    Matcher matcherId = pattern2.matcher(line);
                    if (matcherId.find()) {
                        String id = matcherId.group(1);
                        email.setId(Integer.parseInt(id));
                    }

                    boolean processEmail = false;
                    while ((line = reader.readLine()) != null && !line.contains("QUEUE([" + email.getId() + "]) deleted")) {
                        if (line.contains("DEQUEUER [" + email.getId())) {
                            Matcher matcher = pattern.matcher(line);
                            Set<String> uniqueEmails = new HashSet<>(); // Create a set to store unique emails

                            while (matcher.find()) {
                                String foundEmail = matcher.group(); // Get the matched email
                                if (email.getReceiver() != null && !email.getReceiver().isEmpty()) {
                                    // Check if the email is not already in the set
                                    if (uniqueEmails.add(foundEmail)) {
                                        // If added successfully, append to the receiver
                                        email.setReceiver(email.getReceiver() + "  //  " + foundEmail);
                                    }
                                } else {
                                    // If receiver is empty, initialize it with the found email
                                    email.setReceiver(foundEmail);
                                    uniqueEmails.add(foundEmail); // Add the first email to the set
                                }
                            }

                        }
                        if (line.contains("got:250") && line.contains(String.valueOf(email.getId()))) {
                            int startIndex = line.indexOf("got:250") + "got:250".length();
                            int endIndex = line.indexOf("message");
                            if (endIndex != -1) {
                                String value = line.substring(startIndex, endIndex).trim();
                                value = value.replaceAll("\\s+", "");
                                email.setCouloirID(Integer.parseInt(value));
                            }
                        }
                        if (line.contains("relayed via") && line.contains(String.valueOf(email.getId()))) {
                            Pattern pattern = Pattern.compile("relayed via \\[(.*?)\\]");
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                String ipAddress = matcher.group(1);
                                email.setIPAdress(ipAddress);
                                email.setResult("Relay SMTP @" + ipAddress);
                                processEmail = false;
                            } // Set flag to process this email
                            else {
                                Matcher matcherIP = patternIP.matcher(line);
                                if (matcherIP.find()) {
                                    String ipAddress = matcherIP.group(1);
                                    email.setIPAdress(ipAddress);
                                    processEmail = true;  // Set flag to process this email
                                }
                            }
                        } else if (line.contains("[" + email.getId() + "] rule(Disc-Kaspersky-virus) discarded the message")) {
                            email.setResult("Rejected(mail considered as a virus)");
                        } else if (line.contains("[" + email.getId() + "] message body rejected, got:579 message content is not acceptable here")) {
                            email.setResult("Rejected(mail content is not acceptable)");
                        } else if (line.contains("composed message exceeds the size limit")) {
                            email.setResult("Mail or attachment exceeds size limit");
                        } else if (line.contains(String.valueOf(email.getId())) && line.contains("failed: account is full")) {
                            email.setResult("recipient inbox is full");
                        } else if (line.contains("DEQUEUER [" + email.getId() + "]") && line.contains("message discarded without processing")) {
                            email.setResult("Rejected(Wrong mail address)");
                        } else if (line.contains(String.valueOf(email.getId())) && line.contains(" failed: cancelled with suppressed NDNs")) {
                            email.setResult("cancelled with suppressed NDNs");
                        } else if (line.contains(String.valueOf(email.getId())) && (
                                (line.contains("message discarded without processing")) ||
                                        (line.contains("NoSuchUser")) ||
                                        (line.contains("Recipient address rejected: User unknown")) ||
                                        (line.contains("host name is unknown ")) ||
                                        (line.contains("mailbox unavailable ")) ||
                                        (line.contains("no mailbox here by that name ")) ||
                                        (line.contains("mailbox not found")))) {
                            email.setResult("Rejected(Wrong mail address)");
                        }
                        if (line.contains(String.valueOf(email.getId())) && (
                                (line.contains("This mailbox is disabled")) ||
                                        (line.contains("failed: : DNS A-record is empty")) ||
                                        (line.contains("discarded by Rules")) ||

                                        (line.contains("batch delayed ")) ||
                                        (line.contains("550 authentication required")) ||
                                        (line.contains(" Relay access denied")) ||
                                        (line.contains("Session encryption is required")))) {
                            email.setResult("Not delivered");

                        } else if (line.contains("[" + email.getId() + "] stored on ") || (
                                line.contains("[" + email.getId()) && line.contains(email.getReceiver() + "relayed: relayed via"))) {
                            email.setResult("Delivered");
                            processEmail = false;
                            Matcher matcherIP2 = Pattern.compile("\\[(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\]").matcher(line);
                            if (matcherIP2.find()) {
                                String ipAddressBack = matcherIP2.group(1);
                                email.setIPAdress(ipAddressBack);
                                email.setCouloir(ipAdressConclusion(ipAddressBack));
                            }
                        }
                    }

                    if (processEmail && (receiver.equals("all") || email.getReceiver().contains(receiver))) {
                        emailsToProcess.add(email); // Add email to process if the flag is set
                    }

                    if (receiver.equals("all") || email.getReceiver().contains(receiver)) {
                        emails.add(email);
                    }
                }
            }
        }

        processEmailsInParallel(emailsToProcess); // Process emails in parallel

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
        } else if (ipAddress.equals("10.46.2.51")) {
            return ("BE01");
        } else if (ipAddress.equals("10.46.2.52")) {
            return ("BE02");
        } else if (ipAddress.equals("10.46.2.53")) {
            return ("BE03");
        } else if (ipAddress.equals("10.46.2.54")) {
            return ("BE04");
        }
        return ipAddress;
    }

    private static boolean searchCouloir(Path path, Email email) {
        String couloirID = String.valueOf(email.getCouloirID());

        try (BufferedReader reader = new BufferedReader
                (new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8), 8192)) {
            String line;
            boolean couloirIDFound = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains(" QUEUE([" + couloirID + "]) ")) {
                    couloirIDFound = true;
                    email.setCouloir(path.getParent().getFileName().toString());
                    break;
                }
            }

            if (couloirIDFound) {
                while ((line = reader.readLine()) != null && !line.contains("QUEUE([" + couloirID + "]) deleted")) {
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains("relayed via")) {
                        email.setResult("Delivered");//delivered couloir verified
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains("rule(Disc-Kaspersky-virus) discarded the message")) {
                        email.setResult("Rejected(mail considered as a virus)");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains("message body rejected, got:579 message content is not acceptable here")) {
                        email.setResult("Rejected(mail content is not acceptable)");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains("composed message exceeds the size limit")) {
                        email.setResult("Mail or attachment exceeds size limit");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains("failed: account is full")) {
                        email.setResult("Recipient inbox is full");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains(" failed: cancelled with suppressed NDNs")) {
                        email.setResult("cancelled with suppressed NDNs");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && (
                            (line.contains("message discarded without processing")) ||
                                    (line.contains("NoSuchUser")) ||
                                    (line.contains("Recipient address rejected: User unknown")) ||
                                    (line.contains("host name is unknown ")) ||
                                    (line.contains("mailbox unavailable ")) ||
                                    (line.contains("no mailbox here by that name ")) ||
                                    (line.contains("mailbox not found")))) {
                        email.setResult("Rejected(Wrong mail address)");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && (
                            (line.contains("This mailbox is disabled")) ||
                                    (line.contains("failed: : DNS A-record is empty")) ||
                                    (line.contains("discarded by Rules")) ||

                                    (line.contains("batch delayed ")) ||
                                    (line.contains("550 authentication required")) ||
                                    (line.contains(" Relay access denied")) ||
                                    (line.contains("Session encryption is required")))) {
                        email.setResult("Not delivered");
                        return true;
                    }
                    if ((line.contains("rule(Disc-Kaspersky-spam) rejected the message") ||
                            line.contains("rule(Disc-Kaspersky-spam) discarded the message") ||
                            (line.contains("failed: Message detected as SPAM")))
                            && line.contains("" + email.getCouloirID())) {
                        email.setResult("Rejected(mail considered as a spam)");
                        return true;
                    }
                    if (line.contains(String.valueOf(email.getCouloirID())) && line.contains("rule(discard_from_MX) discarded the message")) {
                        email.setResult("discard from MX");
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    // TO DELETE THIS FUNCTION


    //********* Receiver Functions   **********//
    public static List<Email> readLog(Path path, String receiverMail, String sender) throws IOException {
        List<Email> emails = new ArrayList<>();
        boolean accountIsFullFound = false;
        boolean exceedsSizeLimitFound = false;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            List<String> linesBuffer = new ArrayList<>();
            int dequeuerLineIndex = -1;

            while ((line = reader.readLine()) != null) {
                linesBuffer.add(line);

                if (line.contains("DEQUEUER") && line.contains(receiverMail)) {
                    Email email = new Email();
                    email.setReceiver(receiverMail);

                    // Extract ID from DEQUEUER line
                    Matcher matcherId = patternID.matcher(line);
                    if (matcherId.find()) {
                        String id = matcherId.group(1);
                        email.setId(Integer.parseInt(id));
                    }

                    // Read the block to find the email result
                    while ((line = reader.readLine()) != null && !line.contains("QUEUE([" + email.getId() + "]) deleted")) {
                        linesBuffer.add(line);


                        // Extract date and FES
                        String fileName = path.getFileName().toString().substring(0, Math.min(10, path.getFileName().toString().length()));
                        String extractedLine = line.substring(0, Math.min(8, line.length()));
                        String dateString = fileName + "T" + extractedLine;

                        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                        email.setDate(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
                        email.setFes(path.getParent().getFileName().toString());
                    }

                    // Go back in linesBuffer to find the sender
                    for (int i = linesBuffer.size() - 1; i >= 0; i--) {
                        String bufferedLine = linesBuffer.get(i);
                        if (bufferedLine.contains("from <") && bufferedLine.contains(String.valueOf(email.getId()))) {
                            Matcher matcherSender = patternSender.matcher(bufferedLine);//continue here
                            if (matcherSender.find()) {
                                email.setSender(matcherSender.group(1));
                                break;
                            }
                        }
                        if (bufferedLine.contains("message accepted") && bufferedLine.contains("" + email.getId())) {
                            Matcher matcher1 = patternFESid.matcher(bufferedLine);
                            if (matcher1.find()) {
                                String fesId = matcher1.group(1);
                                email.setCouloirID(Integer.parseInt(fesId));
                            }
                        }
                        if (bufferedLine.contains("relayed via") && bufferedLine.contains("" + email.getId())) {
                            email.setResult("Delivered");
                        } else if ((bufferedLine.contains("rule(Disc-Kaspersky-spam) discarded the message") ||
                                (bufferedLine.contains("failed: Message detected as SPAM")))
                                && bufferedLine.contains("" + email.getId())) {
                            email.setResult("Rejected(mail considered as a spam)");
                        } else if (bufferedLine.contains("composed message exceeds the size limit") && bufferedLine.contains("" + email.getId())) {
                            email.setResult("Mail or attachment exceeds size limit");
                            exceedsSizeLimitFound = true;
                        } else if (bufferedLine.contains("failed: account is full") && bufferedLine.contains("" + email.getId())) {
                            email.setResult("recipient inbox is full");
                            accountIsFullFound = true;
                        } else if (bufferedLine.contains("[" + email.getId() + "] message body rejected, got:579 message content is not acceptable here")) {
                            email.setResult("Rejected(mail content is not acceptable)");
                        } else if (bufferedLine.contains("DEQUEUER [" + email.getId() + "]") && line.contains("message discarded without processing")
                                && !accountIsFullFound && !exceedsSizeLimitFound) {
                            email.setResult("Rejected(Wrong mail address)");
                        }
                    }
                    if (Objects.equals(sender, email.getSender()) || Objects.equals(sender, "all")) {
                        searchFesIdMXExecutor(email, email.getDate());

                        emails.add(email);
                        linesBuffer.clear();
                    }
                }
            }
        }

        return emails;
    }

    public static void searchFesIdMXExecutor(Email email, Date d1) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";

        DateFormat df = new SimpleDateFormat(pattern);


        String date = df.format(d1);

        String[] directories = getDirectoriesInTimeRange(date, date, "receiverFesSearch", "none");

// work on this
        //  System.out.println("CouloirFiles: " + Arrays.toString(logDirectories));
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


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
                        if (line.contains("[" + email.getCouloirID() + "] rule(Disc-Kaspersky-virus) discarded the message")) {
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

    public static void processEmailsInParallel(List<Email> emails) {
        ExecutorService executorService = Executors.newCachedThreadPool();

        List<CompletableFuture<Void>> futures = emails.stream()
                .map(email -> CompletableFuture.runAsync(() -> couloirIdExecutor(email, executorService), executorService))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        shutdownExecutorService(executorService);
    }

    private static void shutdownExecutorService(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            executorService.shutdownNow();
        }
    }

    private static void couloirIdExecutor(Email email, ExecutorService executorService) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(TimeZone.getDefault());
        String date = df.format(email.getDate());

        String[] Directories = getDirectoriesInTimeRange(date, date, "couloirSearch2", ipAdressConclusion(email.getIPAdress()));
        System.out.println(email.getCouloirID() + "  " + ipAdressConclusion(email.getIPAdress()) + " " + email.getDate().toInstant().atZone(ZoneId.of("UTC+1")));
        System.out.println("CouloirFiles: " + Arrays.toString(Directories));
        List<CompletableFuture<Boolean>> futures = Arrays.stream(Directories)
                .flatMap(Directory -> searchCouloirInDirectory(Paths.get(Directory), email, executorService).stream())
                .collect(Collectors.toList());

        boolean found = futures.stream().anyMatch(CompletableFuture::join);

        if (!found) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(email.getDate()); // set the current date to the calendar
            calendar.add(Calendar.MINUTE, 10); // add 30 minutes

            // Get the updated date
            Date newDate = calendar.getTime();


            String dateCheck = df.format(newDate);
            String[] Directories2 = getDirectoriesInTimeRange(dateCheck, dateCheck, "couloirSearch2", ipAdressConclusion(email.getIPAdress()));
            System.out.println("2nd check CouloirFiles: " + Arrays.toString(Directories));
            List<CompletableFuture<Boolean>> futures2 = Arrays.stream(Directories2)
                    .flatMap(Directory2 -> searchCouloirInDirectory(Paths.get(Directory2), email, executorService).stream())
                    .collect(Collectors.toList());
            found = futures2.stream().anyMatch(CompletableFuture::join);

            // a supprimé
        }
    }


    private static List<CompletableFuture<Boolean>> searchCouloirInDirectory(Path directory, Email email, ExecutorService executorService) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> futures.add(CompletableFuture.supplyAsync(() -> searchCouloir(path, email), executorService)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return futures;
    }


    public CompletableFuture<List<Email>> senderMailStatus
            (String mail, String receiver, String d1, String d2) {
        List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();
        String[] directories = getDirectoriesInTimeRange(d1, d2, "logSearch", "none");

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

    public CompletableFuture<List<Email>> checkReceiver(String receiverMail, String sender, String d1, String d2) {
        List<Email> resultEmails = new ArrayList<>();
        CompletableFuture<List<Email>> futureResult = new CompletableFuture<>();
        String[] directories = getDirectoriesInTimeRange(d1, d2, "receiverSearch", "none");

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            for (String directory : directories) {
                Path start = Paths.get(directory);
                Files.walk(start)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            executor.submit(() -> {
                                try {
                                    List<Email> emails = readLog(path, receiverMail, sender);
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


























