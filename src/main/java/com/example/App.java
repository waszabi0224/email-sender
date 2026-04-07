package com.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.*;
import jakarta.mail.UIDFolder.FetchProfileItem;
import jakarta.mail.internet.*;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;

public class App {

    private static String EMAIL = "tothszabi0224@gmail.com";
    private static String USER_NAME = "tothszabi0224@gmail.com";
    private static String PASSWORD = "pzwu foxl tvhh cqrd"; //alkalmazásjelszó

    public static void main( String[] args ) {

        send(read("emails.txt"));
        readGmail();
    }

    //txt beolvasása
    public static ArrayList<String> read(String fileName) {
        File file = new File(fileName);

        ArrayList<String> emails = new ArrayList<>();
        
        try(Scanner reader = new Scanner(file)) {
            while(reader.hasNextLine()) {
                emails.add(reader.nextLine());
            }
        } catch(FileNotFoundException e) {
            System.out.println("Hiba történt.");
            e.printStackTrace();
        }

        return emails;
    }

    //levél küldése
    public static void send(ArrayList<String> emails) {
        
        String from = EMAIL;
        final String username = USER_NAME;
        final String password = PASSWORD;
        String host = "smtp.gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
            new jakarta.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

        for (String email : emails) {
            String to = email;

            //email validálás
            if(is_valid(email)) {
                WriteToFile("Email cím valid -> " + to + " | ");
            }
            else {
                WriteToFile("- Email cím NEM valid -> " + to + "\n");
                continue;
            }
            
            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject("Pályakezdő programozó gyakornok");

                String htmlText = Files.readString(Path.of("email.html"));

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlText, "text/html; charset=UTF-8");

                MimeBodyPart imagePath = new MimeBodyPart();
                imagePath.attachFile(new File("logo.png"));
                imagePath.setHeader("Content-ID","<logo0224>");
                imagePath.setDisposition(MimeBodyPart.INLINE);

                MimeBodyPart attachmenPart = new MimeBodyPart();
                attachmenPart.attachFile(new File("cv.pdf"));
                attachmenPart.setFileName("cv.pdf");

                MimeMultipart multipart = new MimeMultipart();
                multipart.addBodyPart(htmlPart);
                multipart.addBodyPart(imagePath);
                multipart.addBodyPart(attachmenPart);

                message.setContent(multipart);

                //SMTP elfogadta-e
                try {
                    Transport.send(message);
                    WriteToFile("sikeresen elküldve\n");
                } catch(SendFailedException e) {
                    WriteToFile("Érvénytelen vagy hibás email cím\n");
                } catch(MessagingException e) {
                    WriteToFile("Sikertelen küldés\n");
                    WriteToFile(e.getMessage());
                }
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //email cím valid-e
    public static boolean is_valid(String email) {
        boolean is_valid = false;
        try {
            InternetAddress address = new InternetAddress(email);
            address.validate();
            is_valid = true;
        } catch(AddressException e) {
            return is_valid;
        }
        return is_valid;
    }

    //küldés eredményeinek írása txt fájlba
    public static void WriteToFile(String info) {
        try {
            FileWriter writer = new FileWriter("results.txt", true);
            writer.write(info);
            writer.close();
        } catch(IOException e) {
            System.out.println("Hiba történt.");
            e.printStackTrace();
        }
    }

    public static void readGmail() {
        Folder folder = null;
        Store store = null;

        try {
            Thread.sleep(10000);
            store = getImapStore();
            folder = getFolderFromStore(store, "INBOX");

            Message[] messages = folder.search(getMessagesSearchTerm());
            folder.fetch(messages, getFetchProfile());

            for(Message message : messages) {
                isBounceMessage(message);
            }

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            closeFolder(folder);
            closeStore(store);
        }
    }

    //props tulajdonságok megadása (kapcsolat tulajdonságok)
    public static Properties getImapProperties() {
        Properties props = new Properties();
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.ssl.trust", "imap.gmail.com");
        props.put("mail.imaps.port", 993);
        props.put("mail.imaps.starttls.enable", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");
        return props;
    }

    //session és az alapján store létrehozása (kapcsolat)
    public static Store getImapStore() throws Exception {
        Session session = Session.getInstance(getImapProperties());
        Store store = session.getStore("imaps");
        store.connect("imap.gmail.com", EMAIL, PASSWORD);
        return store;
    }

    //mappa megnyitása store-al
    public static Folder getFolderFromStore(Store store, String folderName) throws MessagingException {
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_ONLY);
        return folder;
    }

    //idő alapján szűrés (utolsó 24 óra))
    public static SearchTerm getMessagesSearchTerm() {
        Date yesterdayDate = new Date(new Date().getTime() - (1000*60*60*24));
        return new ReceivedDateTerm(ComparisonTerm.GT, yesterdayDate);
    }

    //fetch-el lehet megadni, milyen adatokat kérjen le előre a levélből (optimalizálás)
    public static FetchProfile getFetchProfile() {
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfileItem.ENVELOPE); //fejléc
        fetchProfile.add(FetchProfileItem.CONTENT_INFO); //törzs
        return fetchProfile;
    }

    //ellenőrzi a levél tartalmát (feladó, tárgy, tartalom alapján)
    public static boolean isBounceMessage(Message message) {
        boolean is_bounce;

        try {
            Address[] from = message.getFrom();
            String fromString = "";
            if(from != null && from.length > 0) {
                fromString = from[0].toString();
            }

            String subjectString = "";
            if(message.getSubject() != null && message.getSubject().length() > 0) {
                subjectString = message.getSubject();
            }

            if(fromString.contains("mailer-daemon") || fromString.contains("postmaster") || fromString.contains("googlemail.com") || fromString.contains("Mail Delivery Subsystem") || subjectString.contains("Delivery Status Notification") || subjectString.contains("Failure")) {
                is_bounce = true;
            } else {
                is_bounce = false;
                return is_bounce;
            }

            StringBuilder textCollector = new StringBuilder();
            collectTextFromMessage(textCollector, message);
            String bodyString = textCollector.toString();

            if(bodyString.contains("A cím nem található") || bodyString.contains("Üzenetét nem sikerült kézbesíteni") || bodyString.contains("cím nem található, vagy nem tud leveleket fogadni.") || bodyString.contains("does not exist") || bodyString.contains("NoSuchUser") || bodyString.contains("couldn't be delivered") || bodyString.contains("wasn't found at")) {
                String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9._%+-]+\\.[a-zA-Z]{2,}";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(bodyString);
                if(matcher.find()) {
                    String notExistEmail = matcher.group();
                    WriteToFile("* Nem létező email cím: " + notExistEmail + "\n");
                }

                return is_bounce;
            } else {
                is_bounce = false;
                return is_bounce;
            }

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //levél vizsgálata
    /*public static void printMessage(Message message) throws MessagingException, IOException {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Kézbesítés dátuma: ").append(message.getReceivedDate()).append("\n");

        Address[] addresesFrom = message.getFrom();
        String from = addresesFrom != null ? ((InternetAddress) addresesFrom[0]).getAddress() : null;
        messageBuilder.append("Feladó: ").append(from).append("\n");
        
        messageBuilder.append("Tárgy: ").append(message.getSubject()).append("\n");

        StringBuilder textCollector = new StringBuilder();
        collectTextFromMessage(textCollector, message);
        messageBuilder.append("Tartalom: ").append(textCollector.toString()).append("\n");

        System.out.println(messageBuilder.toString());
    }*/

    //levél típusának meghatározása (text/plan, text/html, multipart/alternative, multipart/mixed, multipart/related, multipart/*)
    public static void collectTextFromMessage(StringBuilder textCollector, Part part) throws MessagingException, IOException {
        if(part.isMimeType("text/plain")) {
            textCollector.append((String) part.getContent());
        } else if(part.isMimeType("text/html")) {
            textCollector.append((String) part.getContent());
        } else if(part.isMimeType("multipart/*") && part.getContent() instanceof Multipart) {
            Multipart multiPart = (Multipart) part.getContent();
            for(int i = 0; i < multiPart.getCount(); i++) {
                collectTextFromMessage(textCollector, multiPart.getBodyPart(i));
            }
        }  
    }

    //folder lezárása
    public static void closeFolder(Folder folder) {
        if(folder != null && folder.isOpen()) {
            try {
                folder.close(true);
            } catch(MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    //store lezárása
    private static void closeStore(Store store) {
		if (store != null && store.isConnected()) {
			try {
				store.close();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
	}

}
