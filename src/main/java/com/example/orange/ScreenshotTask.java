package com.example.orange;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;

public class ScreenshotTask {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private static final String EMAIL_TO = "becheikh.wassim@esprit.tn";
    private static final String EMAIL_FROM = "wassim.becheikh@gmail.com";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_USER = "wassim.becheikh@gmail.com";
    private static final String SMTP_PASSWORD = "mlgm hxcs txlx iusw";

    public static void main(String[] args) {
        // Schedule the task
        // This could be done using a scheduling library like Quartz, or a simple loop with a sleep in a real application

        // For simplicity, we'll just run the task once here
        runTask();
    }

    public static void runTask() {
        try {

            // Click the button
            File screenshot = takeScreenshot("http://localhost:4200/charts");
            sendEmailWithAttachment(screenshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File takeScreenshot(String url) throws IOException, InterruptedException {
        // Set up the WebDriver (use the correct path to your WebDriver executable)
        System.setProperty("webdriver.chrome.driver", "src/main/resources/static/chromedriver.exe");
        WebDriver driver = new ChromeDriver();
        driver.get(url);
        WebElement emailInput = driver.findElement(By.id("email"));

        emailInput.clear();

        emailInput.sendKeys("@aziza.tn");
        WebElement button = driver.findElement(By.id("submit"));

        // Click the button
        button.click();
        Thread.sleep(3000);
        // Go to the website

        // Take a screenshot
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH_mm_ss");
        String formattedDate = dateFormat.format(currentDate);

// Generate a file name with the current date
        String fileNameWithDate = "screenshot_" + formattedDate + ".png";

// Create a new File with the file name including the current date
        File destFile = new File(fileNameWithDate);
        Files.copy(screenshot.toPath(), destFile.toPath());

        driver.quit();
        return destFile;
    }

    public static void sendEmailWithAttachment(File attachment) throws MessagingException, IOException {
        // Set up the email properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", "587");

        // Authenticate with the SMTP server
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        // Create the email
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
        message.setSubject("Scheduled Screenshot");

        // Add the screenshot as an attachment
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Attached is the screenshot.");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.attachFile(attachment);
        multipart.addBodyPart(attachmentBodyPart);

        message.setContent(multipart);

        // Send the email
        Transport.send(message);
    }
}
