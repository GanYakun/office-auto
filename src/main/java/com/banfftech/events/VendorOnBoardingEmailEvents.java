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
            currentUrl += "/#/menu2/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String content = "To Procurement ,<br>" +
                    "Please see attached link " + currentUrl + ".<br>" +
                    "Looking forward to your reply.<br>" +
                    "Thank you.<br>" +
                    "Kind regards, ";
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
            String content = "Dear Mr./Ms. ,<br>" +
                    "Please see attached link " + currentUrl + ". <br>" +
                    "Please ensure that the following form is completed in its entirety. Failure to complete or sign will resultin the form being returned and will delay due diligence activities.\n" +
                    " Please also ensure that a current (not expired) copy of the Business Partner's Commercial License (oPassport, if relevant) is submitted along with this form.\n" +
                    "Kindly provide documents for your entities.<br>" +
                    "Looking forward to your reply.<br>" +
                    "Thank you.<br>" +
                    "Kind regards, ";
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
            String currentUrl = "http://officeauto.banff-tech.com/#/menu2/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String content = "To Procurement,<br>" +
                    "Please see attached link " + currentUrl + ". <br>" +
                    "Kind regards, ";
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
            currentUrl += "/#/menu2/supplierapprove-managebycompliance/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String content = "To Compliance,<br>" +
                    "Please see attached link " + currentUrl + ".<br>" +
                    "Looking forward to your reply.<br>" +
                    "Kind regards, ";
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
            currentUrl += "/#/menu2/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String content = "To Procurement,<br>" +
                    "Please see attached link " + currentUrl + ".<br>" +
                    "Kind regards,";
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
            currentUrl += "/#/menu2/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            String content = "To Applicant,<br>" +
                    "I am writing to present the result of your vendor onboarding application at attached link " + currentUrl + " .<br>" +
                    "Kind regards, ";
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
            currentUrl += "/#/menu2/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode("SupplierParties('" + coWorkId + "')", "UTF-8");
            String content = "To Applicant,<br>" + comments + "<br>" + currentUrl;
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
            currentUrl += "/#/menu2/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode("SupplierParties('" + coWorkId + "')", "UTF-8");
            String content = "To Procurement,<br>" + comments + "<br>" + currentUrl;
            UtilEmail.sendEmail(emailUrl, supplierName + "(" + supplierId + ") onboarding process", content);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

}
