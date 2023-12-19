package com.banfftech.events;

import com.banfftech.worker.SupplierWorker;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.commons.io.FileUtils;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * @author scy
 * @date 2023/12/8
 */
public class VendorOnBoardingEmailEvents {
    private static final String module = VendorOnBoardingEmailEvents.class.getName();

    public static void toProcurement(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            String applicantId = SupplierWorker.getApplicantId(vendorId, delegator);
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            GenericValue applicant = EntityQuery.use(delegator).from("Party").where("partyId", applicantId).queryOne();
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String supplierName = supplier.getString("partyName");
            String odataId = entity.getId().toString();
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", vendorId, "workEffortTypeId", "COWORK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String subject = "Procurement Review Task Assigned - " + supplierName + "(" + vendorId + ")";
            String content = getTemplate("applicant_to_procurement.html");
            content = content.replace("{{ProcurementUser}}", procurement.getString("partyName"));
            content = content.replace("{{ApplicantUser}}", applicant.getString("partyName"));
            content = content.replace("{{VendorName}}", supplierName);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, procurement.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void toVendor(LocalDispatcher dispatcher, OdataOfbizEntity entity, String email) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            //TODO: FE StickySession
            String currentUrl = "http://officeauto.officeauto.banff-tech.com/o3/#Supplier-DDForm&/SupplierDDForms('{id}')";
            Object supplierPartyId = entity.getPropertyValue("partyId");
            currentUrl = currentUrl.replace("{id}", supplierPartyId.toString());
            String subject = vendorName + " Vendor Registration Application";
            String content = getTemplate("applicant_to_vendor.html");
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, email, subject, content, "EMAIL100");
        } catch (Exception e) {
            Debug.logError(e, module);
        }

    }

    public static void ddSubmit(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            //to applicant
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffort").where("partyId", vendorId, "workEffortTypeId", "COWORK_TASK").orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", coWork.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            String odataId = "SupplierParties('" + coWorkId + "')";
            String currentUrl = "http://officeauto.banff-tech.com/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String subject = "Vendor BPDD Form Completion Notification - " + vendorName + "(" + vendorId + ")";
            String content = getTemplate("vendor_to_applicant.html");
            content = content.replace("{{VendorName}}", vendorName);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, applicantParty.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void toCompliance(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            GenericValue compliance = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "compliance").queryFirst();
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String odataId = entity.getId().toString();
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", vendorId, "approvePartyId", "HG").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebycompliance/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String subject = "Compliance Review Task Assigned - " + vendorName + "(" + vendorId + ")";
            String content = getTemplate("procurement_to_compliance.html");
            content = content.replace("{{ProcurementUser}}", procurement.getString("partyName"));
            content = content.replace("{{VendorName}}", vendorName);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, compliance.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void complianceComplete(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            GenericValue compliance = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "compliance").queryFirst();
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String odataId = entity.getId().toString();
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", vendorId, "workEffortTypeId", "COWORK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String subject = "Compliance Review Completion Notification - " + vendorName + "(" + vendorId + ")";
            String content = getTemplate("compliance_to_procurement.html");
            content = content.replace("{{ProcurementUser}}", procurement.getString("partyName"));
            content = content.replace("{{ComplianceUser}}", compliance.getString("partyName"));
            content = content.replace("{{VendorName}}", vendorName);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, procurement.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void vendorComplete(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String odataId = entity.getId().toString();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            //to applicant
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffort").where("partyId", vendorId).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", coWork.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String subject = "Compliance Review Completion Notification - " + vendorName + "(" + vendorId + ")";
            String content = getTemplate("registered_to_applicant.html");
            content = content.replace("{{ApplicantUser}}", applicantParty.getString("partyName"));
            content = content.replace("{{VendorName}}", vendorName);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, applicantParty.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    /**
     * 打回给申请人
     */
    public static void returnToApplicant(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity, String comments) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            //to applicant
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffort").where("partyId", vendorId).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", coWork.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            currentUrl += "/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode("SupplierParties('" + coWorkId + "')", "UTF-8");
            String subject = "Procurement Preview Feedback - " + vendorName + "(" + vendorId + ")";
            String content = getTemplate("return.html");
            content = content.replace("{{Content}}", comments);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, applicantParty.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    /**
     * 合规打回给采购
     */
    public static void returnToProcurement(LocalDispatcher dispatcher, HttpServletRequest request, OdataOfbizEntity entity, String comments) {
        try {
            Delegator delegator = dispatcher.getDelegator();
            String vendorId = (String) entity.getPropertyValue("partyId");
            GenericValue vendor = EntityQuery.use(delegator).from("Party").where("partyId", vendorId).queryOne();
            String vendorName = vendor.getString("partyName");
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            //to procurement
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", vendorId, "workEffortTypeId", "COWORK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            currentUrl += "/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode("SupplierParties('" + coWorkId + "')", "UTF-8");
            String subject = "Compliance Preview Feedback - " + vendorName + "(" + vendorId + ")";
            String content = getTemplate("return.html");
            content = content.replace("{{Content}}", comments);
            content = content.replace("{{TargetUrl}}", currentUrl);
            //创建待发送邮件
            createCommunicationEvent(dispatcher, procurement.getString("primaryEmail"), subject, content, null);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    private static String getTemplate(String templateName) throws IOException {
        String tempPath = "component://officeauto/documents/vendorOnBording_temp/" + templateName;
        String fileUrl = FlexibleLocation.resolveLocation(tempPath).getFile();
        return FileUtils.readFileToString(new File(fileUrl), "utf-8");
    }

    private static void createCommunicationEvent(LocalDispatcher dispatcher, String email, String subject, String content, String contentId) throws GenericEntityException, GenericServiceException {
        Delegator delegator = dispatcher.getDelegator();
        String communicationEventId = delegator.getNextSeqId("CommunicationEvent");
        dispatcher.runSync("banfftech.createCommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId,
                "communicationEventTypeId", "AUTO_EMAIL_COMM","statusId", "COM_IN_PROGRESS", "contactMechIdFrom", "EMAIL100", "subject", subject,
                "contentMimeTypeId", "text/html", "content", content, "toString", email, "headerString", "Procurement System", "userLogin", Util.getSystemUser(delegator)));
        //带附件的邮件
        if (UtilValidate.isNotEmpty(contentId)) {
            dispatcher.runSync("banfftech.createCommEventContentAssoc", UtilMisc.toMap("communicationEventId", communicationEventId,
                    "contentId", contentId, "fromDate", UtilDateTime.nowTimestamp(), "userLogin", Util.getSystemUser(delegator)));
        }
    }

}
