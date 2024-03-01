package com.example.orange.services;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogFileReader {

    public static List<TimeRange> getTimeRanges(String directoryPath) {
        List<TimeRange> timeRanges = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

        try (Stream<Path> paths = Files.list(Paths.get(directoryPath))) {
            List<Path> sortedFiles = paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList());

            for (int i = 0; i < sortedFiles.size() - 1; i++) {
                Path currentFile = sortedFiles.get(i);
                Path nextFile = sortedFiles.get(i + 1);

                String currentFileName = currentFile.getFileName().toString();
                String nextFileName = nextFile.getFileName().toString();

                LocalDateTime startTime = LocalDateTime.parse(currentFileName.substring(0, 16), formatter);
                LocalDateTime endTime = LocalDateTime.parse(nextFileName.substring(0, 16), formatter);

                timeRanges.add(new TimeRange(startTime, endTime));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return timeRanges;
    }

    public static class TimeRange {
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }
    }

    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01";
        List<TimeRange> timeRanges = getTimeRanges(directoryPath);

        // Print the time ranges
        for (TimeRange timeRange : timeRanges) {
            System.out.println("Start Time: " + timeRange.getStartTime() + ", End Time: " + timeRange.getEndTime());
        }
    }
}
