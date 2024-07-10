package com.example.orange.services;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Service
public class LogDetails {


    private static void searchInDirectory(String directoryPath, String wordToSearch,
                                          ExecutorService executor, StringBuilder resultBuilder, String date) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            List<Future<String>> futures = new ArrayList<>();

            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(date))
                    .forEach(path -> {
                        Future<String> future = executor.submit(() -> {
                            return searchLogInFile(path, wordToSearch);
                        });
                        futures.add(future);
                    });

            for (Future<String> future : futures) {
                String result = future.get();
                if (result != null) {
                    resultBuilder.append(result).append(System.lineSeparator());
                }
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String searchLogInFile(Path filePath, String wordToSearch) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean copyLines = false;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(wordToSearch)) {

                    stringBuilder.append(line).append(System.lineSeparator());
                }
                if (line.contains("QUEUE([" + wordToSearch + "]) deleted")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public String searchLogInFiles(String wordToSearch, Integer option, String ipAdress, String date) throws IOException, InterruptedException, ExecutionException {
        StringBuilder resultBuilder = new StringBuilder();
        String directoryPath1 = "";

        String directoryPath2 = "";
        String directoryPath3 = "";
        String directoryPath4 = "";


        if (option == 1) {
            directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01";
            directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02";
        } else if (option == 3) {
            directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX01";
            directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX02";
            directoryPath3 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX03";
            directoryPath4 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\MX\\MX04";

        } else {
            switch (ipAdress) {
                case "10.46.96.20" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP01";
                    directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP02";


                }
                case "10.46.96.21" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\GP01";
                    directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\GP02";

                }
                case "10.46.96.22" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\ML01";
                    directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\ML02";

                }
                case "BE01", "BE02", "BE03", "BE04" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01";
                    directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02";
                }
                case "10.46.96.13" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP01";
                    directoryPath2 = null;

                }
                case "10.46.96.14" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\VIP02";
                    directoryPath2 = null;

                }
                case "10.46.96.15" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\GP01";
                    directoryPath2 = null;

                }
                case "10.46.96.16" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\GP02";
                    directoryPath2 = null;

                }
                case "10.46.96.17" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\ML01";
                    directoryPath2 = null;

                }
                case "10.46.96.18" -> {
                    directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\ML02";
                    directoryPath2 = null;

                }
                default -> {
                    return "CouloirId unknown: " + wordToSearch;

                }
            }
        }
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            if (directoryPath1 != null && !directoryPath1.isEmpty()) {
                searchInDirectory(directoryPath1, wordToSearch, executor, resultBuilder, date);
            }
            if (directoryPath2 != null && !directoryPath2.isEmpty()) {
                searchInDirectory(directoryPath2, wordToSearch, executor, resultBuilder, date);
            }
            if (directoryPath3 != null && !directoryPath3.isEmpty()) {
                searchInDirectory(directoryPath3, wordToSearch, executor, resultBuilder, date);
            }
            if (directoryPath4 != null && !directoryPath4.isEmpty()) {
                searchInDirectory(directoryPath4, wordToSearch, executor, resultBuilder, date);
            }
        } finally {
            executor.shutdown();
        }

        return resultBuilder.toString();
    }
}