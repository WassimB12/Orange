package com.example.orange.services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {



    public static void main(String[] args) throws IOException {
           String searchTerm = "grftgyyfgtfytyggtyh@planet.tn"; // Replace with your search term
        String directory = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES01";
        String directory2 = "C:\\Users\\wassi\\OneDrive\\Bureau\\PROJECT\\PFE\\PFE-Kattem\\Log\\FES02";

        Path start = Paths.get(directory).resolve(directory2);

        // Create a ForkJoinPool with parallelism equal to available processors ***
        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        // Perform the search in parallel
        forkJoinPool.submit(() ->
                {
                    try {
                        Files.walk(start)
                                .parallel()
                                .filter(path -> Files.isRegularFile(path))
                                .forEach(path -> searchInFile(path, searchTerm));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
         ).join();
    }

    private static void searchInFile(Path path, String searchTerm) {
        try {
            String emailRegex = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b";

            Pattern pattern = Pattern.compile(emailRegex);

            List<String> lines = Files.readAllLines(path);
            for (int i = 0; i < lines.size(); i++) {
                if ( lines.get(i).contains("from") &&lines.get(i).contains(searchTerm)) {
                    int j=i;
                    System.out.println("email sender : "+searchTerm);

                    while(!lines.get(j).contains("deleted")){
                       if(lines.get(j).contains("DEQUEUER")){
                       Matcher matcher = pattern.matcher(lines.get(j));
                       while (matcher.find()) {
                           System.out.println("Email receiver: " + matcher.group());
                       }

                   }

                        if(lines.get(j).contains("relayed via")){
                        System.out.println("mail well delivred ");
                    }
                        if(lines.get(j).contains("undelivred")){
                            System.out.println("mail undelivred ");
                        }
                    j++;
                }
                    System.out.println("Found in file: " + path.toString() + ", line: " + (i + 1));
                }
            }
        } catch (IOException e) {
            // Handle exception
            e.printStackTrace();
        }
    }
}



