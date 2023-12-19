package com.banfftech.worker;

import org.apache.commons.io.FileUtils;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

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

    public static String getVendorOnBoardingTemp() throws IOException {
        String tempPath = "component://officeauto/documents/vendorOnBoarding_temp.html";
        String fileUrl = FlexibleLocation.resolveLocation(tempPath).getFile();
        return FileUtils.readFileToString(new File(fileUrl), "utf-8");
    }

    public static String getVendorOnBoardingVendorTemp() throws IOException {
        String tempPath = "component://officeauto/documents/vendorOnBoarding_temp_vendor.html";
        String fileUrl = FlexibleLocation.resolveLocation(tempPath).getFile();
        return FileUtils.readFileToString(new File(fileUrl), "utf-8");
    }

}
