package com.banfftech.services;

import org.apache.ofbiz.base.util.Debug;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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

    public static void sendEmail(String emailUrl, String titleName, String content) throws MessagingException {
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
        message.setFrom(new InternetAddress(SEND_EMAIL_URL));
        // 收件人和抄送人
        message.setRecipients(Message.RecipientType.TO, emailUrl);
//		message.setRecipients(Message.RecipientType.CC, MY_EMAIL_ACCOUNT);

        // 内容(这个内容还不能乱写,有可能会被SMTP拒绝掉;多试几次吧)
        message.setSubject(titleName);
        message.setContent(content, "text/html;charset=UTF-8");
        message.setSentDate(new Date());
        message.saveChanges();
        Debug.log("准备发送到: " + emailUrl);
        Transport.send(message);
    }

    public static final String vendorOnBoardingTemp = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Email with Button</title>\n" +
            "</head>\n" +
            "<body style=\"font-family: Arial, sans-serif; text-align: center;\">\n" +
            "\n" +
            "    <img src=\"https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/demo_min.jpg\" alt=\"Logo\" style=\"max-width: 100%; height: auto; margin-bottom: 20px;\">\n" +
            "<h1>OfficeAuto Demo</h1>" +
            "\n" +
            "    <div style=\"text-align: left;\">\n" +
            "        <hr style=\"margin: 30px 0; border: none; border-top: 2px solid #ddd;\">\n" +
            "        <h1>{{title}}</h1>\n" +
            "        <p>{{content}}.</p>\n" +
            "\n" +
            "        <a href=\"{{url}}\" style=\"text-decoration: none;\">\n" +
            "            <button style=\"display: inline-block; padding: 10px 20px; font-size: 16px; font-weight: bold; text-align: center; text-decoration: none; background-color: #4CAF50; color: white; border-radius: 5px; cursor: pointer;\">\n" +
            "                Click Here\n" +
            "            </button>\n" +
            "        </a>\n" +
            "\n" +
            "        <hr style=\"margin: 30px 0; border: none; border-top: 2px solid #ddd;\">\n" +
            "    </div>\n" +
            "\n" +
            "</body>\n" +
            "</html>\n";
}
