package com.banfftech.services;

import org.apache.commons.io.FileUtils;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

public class UtilEmail {
    public static final String EMAIL_ANNEX_EXCEL_TYPE = "application/msexcel";
    // 发件人电子邮箱
    public static final String SEND_EMAIL_URL = "try028@163.com";
    // 发件人电子邮箱授权码
    public static final String SEND_EMAIL_AUTHORIZE = "RTDDNJEMYVYRPYIX";
    // SMTP服务器(这里用的163 SMTP服务器)
    public static final String EMAIL_163_SMTP_HOST = "smtp.163.com";
    // 端口号,这个是163使用到的;QQ的应该是465或者875
    public static final String SMTP_163_PORT = "465";


    public static void sendEmail(String emailUrl, String titleName, String content) throws MessagingException, UnsupportedEncodingException {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", EMAIL_163_SMTP_HOST);
        p.setProperty("mail.smtp.port", SMTP_163_PORT);
        p.setProperty("mail.smtp.socketFactory.port", SMTP_163_PORT);
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.smtp.socketFactory.class", "SSL_FACTORY");
        p.put("mail.smtp.ssl.enable", "true");// 设置是否使用ssl安全连接 ---一般都使用
        Session session = Session.getInstance(p, new Authenticator() {
            // 设置认证账户信息
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SEND_EMAIL_URL, SEND_EMAIL_AUTHORIZE);
            }
        });
        session.setDebug(true);
        Debug.log("创建邮件");
        MimeMessage message = new MimeMessage(session);
        // 发件人
        InternetAddress internetAddress = new InternetAddress(SEND_EMAIL_URL);
        internetAddress.setPersonal("Procurement System");
        message.setFrom(internetAddress);
        // 收件人和抄送人
        message.setRecipients(Message.RecipientType.TO, emailUrl);
//		message.setRecipients(Message.RecipientType.CC, MY_EMAIL_ACCOUNT);

        message.setSubject(titleName);
        message.setContent(content, "text/html;charset=UTF-8");
        message.setSentDate(new Date());
        message.saveChanges();
        Debug.log("准备发送到: " + emailUrl);
        Transport.send(message);

    }

    public static void sendAttachmentEmail(String emailUrl, String titleName, String content, String attachmentUrl) throws MessagingException, IOException {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", EMAIL_163_SMTP_HOST);
        p.setProperty("mail.smtp.port", SMTP_163_PORT);
        p.setProperty("mail.smtp.socketFactory.port", SMTP_163_PORT);
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.smtp.socketFactory.class", "SSL_FACTORY");
        p.put("mail.smtp.ssl.enable", "true");// 设置是否使用ssl安全连接 ---一般都使用
        Session session = Session.getInstance(p, new Authenticator() {
            // 设置认证账户信息
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SEND_EMAIL_URL, SEND_EMAIL_AUTHORIZE);
            }
        });
        session.setDebug(true);
        Debug.log("创建邮件");
        // 发件人
        InternetAddress internetAddress = new InternetAddress(SEND_EMAIL_URL);
        internetAddress.setPersonal("Procurement System");
        //主消息
        MimeMessage message = new MimeMessage(session);
        // 收件人和抄送人
        message.setFrom(internetAddress);
        message.setRecipients(Message.RecipientType.TO, emailUrl);
//		message.setRecipients(Message.RecipientType.CC, MY_EMAIL_ACCOUNT);
        Multipart multiPart = new MimeMultipart();
        //文本部分
        BodyPart textPart = new MimeBodyPart();
        textPart.setContent(content, "text/html;charset=UTF-8");
        //附件部分
        BodyPart imageBodyPart = new MimeBodyPart();
        String fileUrl = FlexibleLocation.resolveLocation(attachmentUrl).getFile();
        fileUrl = fileUrl.replaceAll("%20", " ");
        File file = new File(fileUrl);
        //获取fileUrl这个路径的文件转为byte
        imageBodyPart.setFileName(file.getName());
        imageBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(FileUtils.readFileToByteArray(file), "application/octet-stream")));
        //放入multiPart
        multiPart.addBodyPart(imageBodyPart);
        multiPart.addBodyPart(textPart);

        message.setSubject(titleName);
        message.setContent(multiPart);
        message.setSentDate(new Date());
        message.saveChanges();
        Debug.log("准备发送到: " + emailUrl);
        Transport.send(message);

    }

    public static String getVendorOnBoardingTemp() throws IOException {
        String tempPath = "component://officeauto/documents/VendorOnBoarding_temp.html";
        String fileUrl = FlexibleLocation.resolveLocation(tempPath).getFile();
        return FileUtils.readFileToString(new File(fileUrl), "utf-8");
    }

    public static String getVendorOnBoardingVendorTemp() throws IOException {
        String tempPath = "component://officeauto/documents/VendorOnBoarding_vendor_temp.html";
        String fileUrl = FlexibleLocation.resolveLocation(tempPath).getFile();
        return FileUtils.readFileToString(new File(fileUrl), "utf-8");
    }

}
