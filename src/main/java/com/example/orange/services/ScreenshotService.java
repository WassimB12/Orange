package com.example.orange.services;

import com.example.orange.entities.DomainList;
import com.example.orange.repository.DomainRepository;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ScreenshotService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private static final String EMAIL_FROM = "wassim.becheikh@gmail.com";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_USER = "wassim.becheikh@gmail.com";
    private static final String SMTP_PASSWORD = "mlgm hxcs txlx iusw";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    DomainRepository domainRepository;




    public static File takeScreenshotAndSendEmail(String url, String mail, String receiver, int op)
            throws IOException, InterruptedException, MessagingException {
        // Set up the WebDriver (use the correct path to your WebDriver executable)
        System.setProperty("webdriver.chrome.driver", "src/main/resources/static/chromedriver.exe");
        WebDriver driver = new ChromeDriver();
        driver.get(url);
        WebElement emailInput = driver.findElement(By.id("email"));

        emailInput.clear();
        emailInput.sendKeys(mail);

        WebElement button = driver.findElement(By.id("submit"));
        button.click();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofHours(2));
        WebElement chart = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("chart")));

        Thread.sleep(700);

        // Retrieve the content of the <p> element with id "rapport"
        WebElement rapportElement = driver.findElement(By.id("rapport"));
        String rapportContent = rapportElement.getText();

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

        // Make the rapport element visible
        ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block';", rapportElement);

        // Retrieve the content of the <p> element again after making it visible
        rapportContent = rapportElement.getText();

        driver.quit();

        // Send email with the updated content of the <p> element and the screenshot as attachment
        sendEmailWithAttachment(destFile, rapportContent, receiver, op);

        return destFile;
    }

    public static void sendEmailWithAttachment(File attachment, String content, String receiver, int op) throws MessagingException, IOException {
        // Set up the email properties
        // Include your email setup here as before
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
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver));
        if (Objects.equals(op, 2)) {
            message.setSubject("Mail transmission problem to review");
        } else {
            message.setSubject("Mail transmission details");
        }

        // Add the content of the <p> element to the email body
        MimeBodyPart contentBodyPart = new MimeBodyPart();
        contentBodyPart.setText(content);

        // Add the screenshot as an attachment
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.attachFile(attachment);

        // Create the multipart to include both content and attachment
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(contentBodyPart);
        multipart.addBodyPart(attachmentBodyPart);

        message.setContent(multipart);

        // Send the email
        Transport.send(message);
    }

    public void runTask(String mail, String receiver, int op) {
        try {

            // Click the button
            File screenshot = takeScreenshotAndSendEmail("http://localhost:4200/charts", mail, receiver, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startScheduling(String hours, String minutes) {
        // Parse the hours and minutes
        int hour = Integer.parseInt(hours);
        int minute = Integer.parseInt(minutes);

        // Calculate initial delay and period (in seconds)
        long initialDelay = calculateInitialDelay(hour, minute);
        long period = TimeUnit.DAYS.toSeconds(1); // Run daily

        // Schedule the task
        scheduler.scheduleAtFixedRate(this::scheduleTask, initialDelay, period, TimeUnit.SECONDS);
    }

    private long calculateInitialDelay(int hour, int minute) {
        LocalTime now = LocalTime.now();
        LocalTime targetTime = LocalTime.of(hour, minute);

        Duration duration = Duration.between(now, targetTime);
        if (duration.isNegative()) {
            // If the target time is earlier than the current time, schedule for the next day
            duration = duration.plusDays(1);
        }

        return duration.toMillis();
    }


    public void stopScheduling() {
        scheduler.shutdown();
    }

    public void scheduleTask() {
        List<DomainList> domainList = domainRepository.findAll();

        for (DomainList domain : domainList) {
            System.out.println(domain.getName() + domain.getRespEmail());
            runTask(domain.getName(), domain.getRespEmail(), 1);
        }
    }


}

