package com.banfftech.events;

import com.banfftech.services.UtilEmail;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;

/**
 * @author scy
 * @date 2023/12/8
 */
public class VendorOnBoardingEmailEvents {
    private static final String module = VendorOnBoardingEmailEvents.class.getName();

    public static void toProcurement(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String odataId = entity.getId().toString();
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierId, "workEffortTypeId", "COWORK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String contentStr = "Please see attached link.<br>" +
                    "Looking forward to your reply.<br>" +
                    "Thank you.<br>" +
                    "Kind regards, ";
            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Procurement");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Procurement");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            Debug.logInfo("===== Url: " + currentUrl, module);
            UtilEmail.sendEmail(procurement.getString("primaryEmail"), supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void toVendor(Delegator delegator, OdataOfbizEntity entity, String email) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            String currentUrl = "http://officeauto.officeauto.banff-tech.com/o3/#Supplier-DDForm&/SupplierDDForms('{id}')";
            Object supplierPartyId = entity.getPropertyValue("partyId");
            currentUrl = currentUrl.replace("{id}", supplierPartyId.toString());
            String contentStr = "Dear Mr./Ms. ,<br>" +
                    "Please ensure that the following form is completed in its entirety. Failure to complete or sign will resultin the form being returned and will delay due diligence activities.<br>" +
                    "Please also ensure that a current (not expired) copy of the Business Partner's Commercial License (oPassport, if relevant) is submitted along with this form.<br>" +
                    "Kindly provide documents for your entities.<br>" +
                    "Looking forward to your reply.<br>" +
                    "Thank you.<br>" +
                    "Kind regards, ";

            String content = UtilEmail.getVendorOnBoardingVendorTemp();
            content = content.replace("${{Title}}", "Hello!");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "Hello!");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            Debug.logInfo("===== Url: " + currentUrl, module);
            UtilEmail.sendEmail(email, supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }

    }

    public static void ddSubmit(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            //to applicant
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffort").where("partyId", supplierId, "workEffortTypeId", "COWORK_TASK").orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", coWork.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String emailUrl = applicantParty.getString("primaryEmail");
            String coWorkId = coWork.getString("workEffortId");
            String odataId = "SupplierParties('" + coWorkId + "')";
            String currentUrl = "http://officeauto.banff-tech.com/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String contentStr = "Please see attached link. <br>" +
                    "Looking forward to your reply.<br>" +
                    "Thank you.<br>" +
                    "Kind regards, ";

            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Applicant");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Applicant");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            Debug.logInfo("===== Url: " + currentUrl, module);
            UtilEmail.sendEmail(emailUrl, supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void toCompliance(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            GenericValue compliance = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "compliance").queryFirst();
            String odataId = entity.getId().toString();
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierId, "approvePartyId", "HG").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebycompliance/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String contentStr = "Please see attached link.<br>" +
                    "Looking forward to your reply.<br>" +
                    "Kind regards, ";

            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Compliance");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Compliance");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            Debug.logInfo("===== Url: " + currentUrl, module);
            UtilEmail.sendEmail(compliance.getString("primaryEmail"), supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void complianceComplete(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String odataId = entity.getId().toString();
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierId, "workEffortTypeId", "COWORK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String contentStr = "Please see attached link. <br>" +
                    "Kind regards,";

            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Procurement");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Procurement");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            Debug.logInfo("===== Url: " + currentUrl, module);
            UtilEmail.sendEmail(procurement.getString("primaryEmail"), supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static void vendorComplete(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity) {
        try {
            String odataId = entity.getId().toString();
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            //to applicant
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffort").where("partyId", supplierId).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", coWork.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String emailUrl = applicantParty.getString("primaryEmail");
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String contentStr = "To Applicant,<br>" +
                    "I am writing to present the result of your vendor onboarding application at attached link.<br>" +
                    "Kind regards, ";

            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Applicant");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Applicant");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            UtilEmail.sendEmail(emailUrl, supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    /**
     * 打回给申请人
     */
    public static void returnToApplicant(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity, String comments) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            //to applicant
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffort").where("partyId", supplierId).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", coWork.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String emailUrl = applicantParty.getString("primaryEmail");
            String coWorkId = coWork.getString("workEffortId");
            currentUrl += "/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode("SupplierParties('" + coWorkId + "')", "UTF-8");
            String contentStr = comments + "<br>";

            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Applicant");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Applicant");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            UtilEmail.sendEmail(emailUrl, supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    /**
     * 合规打回给采购
     */
    public static void returnToProcurement(Delegator delegator, HttpServletRequest request, OdataOfbizEntity entity, String comments) {
        try {
            String supplierId = (String) entity.getPropertyValue("partyId");
            GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierId).queryOne();
            String supplierName = supplier.getString("partyName");
            String currentUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            //to procurement
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String emailUrl = procurement.getString("primaryEmail");
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierId, "workEffortTypeId", "COWORK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            currentUrl += "/#/supplier/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode("SupplierParties('" + coWorkId + "')", "UTF-8");
            String contentStr = comments + "<br>";

            String content = UtilEmail.getVendorOnBoardingTemp();
            content = content.replace("${{Title}}", "To Applicant");
            content = content.replace("${{Content}}", contentStr);
            content = content.replace("${{TargetUrl}}", currentUrl);

//            String content = UtilEmail.vendorOnBoardingTemp;
//            content = content.replace("{{title}}", "To Procurement");
//            content = content.replace("{{content}}", contentStr);
//            content = content.replace("{{url}}", currentUrl);
            UtilEmail.sendEmail(emailUrl, supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

}
