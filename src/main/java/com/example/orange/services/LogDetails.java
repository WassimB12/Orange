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


    private static void searchInDirectory(String directoryPath, String wordToSearch, ExecutorService executor, StringBuilder resultBuilder) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            List<Future<String>> futures = new ArrayList<>();
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        Future<String> future = executor.submit(() -> {
                            String result = searchLogInFile(path, wordToSearch);
                            return result;
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
                    copyLines = true;
                }
                if (copyLines) {
                    stringBuilder.append(line).append(System.lineSeparator());
                }
                if (copyLines && line.contains("QUEUE([" + wordToSearch + "]) deleted")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public String searchLogInFiles(String wordToSearch) throws IOException, InterruptedException, ExecutionException {
        StringBuilder resultBuilder = new StringBuilder();
        String directoryPath1 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01";
        String directoryPath2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02";
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            searchInDirectory(directoryPath1, wordToSearch, executor, resultBuilder);
            searchInDirectory(directoryPath2, wordToSearch, executor, resultBuilder);
        } finally {
            executor.shutdown();
        }

        return resultBuilder.toString();
    }
}