import com.dpbird.odata.Util
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.olingo.commons.api.data.Entity
import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.base.util.UtilValidate;
import java.math.BigDecimal;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.base.util.UtilMisc;
import com.banfftech.worker.SupplierWorker;
import com.banfftech.worker.CriticalValueWorker;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType
import sun.net.www.content.text.Generic


import java.sql.Timestamp;

module = "generateFields.groovy";

def generateFields(Map<String, Object> context){
    List<Entity> entityList = context.parameters.entityList;
    Delegator delegator = context.get("delegator");
    Map<String, Object> ddFormTypeMap = UtilMisc.toMap("No DD", 1L, "Simplified", 2L, "Standard", 3L);
    Map<String, Object> vendorTypeMap = UtilMisc.toMap("GOVERNMENT_TYPE", 1L, "INTERNAL_ORG_TYPE", 2L, "REGULAR_TYPE", 3L, "SMALL_PURCHASE_TYPE", 5L);
    Map<String, Object> ddFormDealMap = UtilMisc.toMap("Not Request", 5L, "Request", 2L, "Submitted", 3L, "Require Changes", 2L, "Wrong Types", 1L);
    Map<String, Object> priorityMap = UtilMisc.toMap("PRIORITY_HIGH", 1L, "PRIORITY_MEDIUM", 3L, "PRIORITY_LOW", 5L);
    entityList.each { entity ->
        String ddFormType;
        String ddFormTypeId;
        ddFormTypeCritical = 0L;
        checkWarningCritical = 0L;
        ddFormTypeCritical = 0L;
        vendorTypeCritical = 0L;
        ratingNumeric = 0L;
        processNumeric = 0L;
        processCritical = 0L;
        priorityCritical = 0L;
        riskCritical = 0L;
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
        ddFormType = SupplierWorker.getDDFormType(supplierParty, delegator);
        ddFormTypeId = SupplierWorker.getDDFormTypeId(ddFormType);
        ratingNumeric = SupplierWorker.getClassificationRatingNumber(supplierParty, delegator);
        processNumeric = SupplierWorker.getProcessNumeric(supplierParty, delegator);
        processCritical = SupplierWorker.getProcessCritical(processNumeric);
        vendorTypeCritical = vendorTypeMap.get(supplierParty.get("groupTypeId"));
        Boolean ddFormIsSubmitted = SupplierWorker.ddFormIsSubmitted(delegator, supplierParty);
        Timestamp lastSubmittedDate = SupplierWorker.getLastSubmittedDate(supplierParty, delegator);
        riskCritical = SupplierWorker.getClassificationCriticalValue(supplierParty, delegator);
        if (UtilValidate.isNotEmpty(supplierParty.get("priority"))){
            priorityCritical = priorityMap.get(supplierParty.get("priority"));
        }
        String cycleTime = SupplierWorker.calculateCycleTime(supplierParty, delegator);
        criticalityValue = 2L
        String statusId = supplierParty.getString("statusId");
        if (statusId.equals("PROCESSED") && UtilValidate.isNotEmpty(criticalityValue)){
            criticalityValue = 4L
        }else if (statusId.equals("NOT_PROCESSED")){
            criticalityValue = 1L
        }

        if (SupplierWorker.isCheckWarning(supplierParty, delegator)){
            checkWarningCritical = 1L
        }
        ddFormTypeCritical = CriticalValueWorker.getCriticalValue(ddFormTypeMap, ddFormType);
        entity.addProperty(new Property(null, "ddFormType", ValueType.PRIMITIVE, ddFormType))
        entity.addProperty(new Property(null, "ddFormTypeId", ValueType.PRIMITIVE, ddFormTypeId))
        entity.addProperty(new Property(null, "criticalityValue", ValueType.PRIMITIVE, criticalityValue))
        entity.addProperty(new Property(null, "ddFormTypeCritical", ValueType.PRIMITIVE, ddFormTypeCritical))
        entity.addProperty(new Property(null, "cycleTime", ValueType.PRIMITIVE, cycleTime))
        entity.addProperty(new Property(null, "riskCritical", ValueType.PRIMITIVE, riskCritical))
        entity.addProperty(new Property(null, "priorityCritical", ValueType.PRIMITIVE, priorityCritical))
        entity.addProperty(new Property(null, "lastSubmittedDate", ValueType.PRIMITIVE, lastSubmittedDate))
        entity.addProperty(new Property(null, "ratingNumeric", ValueType.PRIMITIVE, ratingNumeric))
        entity.addProperty(new Property(null, "processNumeric", ValueType.PRIMITIVE, processNumeric))
        entity.addProperty(new Property(null, "processCritical", ValueType.PRIMITIVE, processCritical))
        entity.addProperty(new Property(null, "vendorTypeCritical", ValueType.PRIMITIVE, vendorTypeCritical))
        entity.addProperty(new Property(null, "ddFormIsSubmitted", ValueType.PRIMITIVE, ddFormIsSubmitted))

        //文件数量
        String supplierId = supplierParty.getString("partyId");
        //DDFormPDF访问地址
        String url = null;
        String formName = null;
        if (ddFormIsSubmitted) {
            GenericValue genericValue = EntityQuery.use(delegator).from("PartyAttribute").where("partyId", supplierId, "attrName", "ddFormType").queryFirst();
            if (UtilValidate.isNotEmpty(genericValue)) {
                String attrValue = genericValue.getString("attrValue");
                if ("SIMPLIFIED_DD".equals(attrValue)) {
                    formName = "Download";
                    url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/SimplifiedForm.pdf"
                };
                if ("STANDARD_DD".equals(attrValue)) {
                    url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/StandardForm.pdf"
                    formName = "Download";
                };
            }
        }
        entity.addProperty(new Property(null, "ddFromUrl", ValueType.PRIMITIVE, url));
        entity.addProperty(new Property(null, "ddFromName", ValueType.PRIMITIVE, formName));
        //WorkScope
        String workScope = null;
        GenericValue surveyQuestionAnswer = EntityQuery.use(delegator).from("SurveyQuestionAnswer").where(UtilMisc.toMap("partyId", supplierId, "surveyQuestionId", "9004")).queryFirst();
        if (UtilValidate.isNotEmpty(surveyQuestionAnswer)) {
            workScope = surveyQuestionAnswer.getString("textResponse");
        }
        entity.addProperty(new Property(null, "workScope", ValueType.PRIMITIVE, workScope));
        GenericValue partyIdentification = EntityQuery.use(delegator).from("PartyIdentification")
                .where(UtilMisc.toMap("partyIdentificationTypeId", "REGISTRATION_NUMBER", "partyId", supplierId)).queryFirst();
        String usccNumber = partyIdentification == null ? null : partyIdentification.getString("idValue")
        //DDForm Bool Field
        List<String> roles = EntityQuery.use(delegator).from("PartyRole").where("partyId", supplierId).getFieldList("roleTypeId");
        entity.addProperty(new Property(null, "agent", ValueType.PRIMITIVE, roles.contains("AGENT")));
        entity.addProperty(new Property(null, "jvPartner", ValueType.PRIMITIVE, roles.contains("JV_PARTNER")));
        entity.addProperty(new Property(null, "subContractor", ValueType.PRIMITIVE, roles.contains("SUB_CONTRACTOR")));
        entity.addProperty(new Property(null, "consultantAdvisor", ValueType.PRIMITIVE, roles.contains("CONSULTANT_ADVISOR")));
        entity.addProperty(new Property(null, "supplierVendor", ValueType.PRIMITIVE, roles.contains("SUPPLIER_VENDOR")));
        entity.addProperty(new Property(null, "distributor", ValueType.PRIMITIVE, roles.contains("DISTRIBUTOR")));
        entity.addProperty(new Property(null, "investmentTarget", ValueType.PRIMITIVE, roles.contains("INVESTMENT_TARGET")));
        entity.addProperty(new Property(null, "otherSpecify", ValueType.PRIMITIVE, roles.contains("OTHER_SPECIFY")));
        entity.addProperty(new Property(null, "usccNumber", ValueType.PRIMITIVE, usccNumber));
        entity.addProperty(new Property(null, "checkWarningCritical", ValueType.PRIMITIVE, checkWarningCritical));

        //coWork Status
        GenericValue parentWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierId, "workEffortTypeId", "COWORK").queryFirst();
        if (UtilValidate.isNotEmpty(parentWork)) {
            List<GenericValue> childWork =parentWork.getRelated("ChildWorkEffortAndPartyGroupContact", null, null, false);
            for (GenericValue child : childWork) {
                String approveCompany = child.getString("approvePartyId");
                GenericValue coWorkStatus = child.getRelatedOne("CurrentStatusItem", false);
                String coWorkStatusDescription = "PROCESSED".equals(coWorkStatus.getString("statusId")) ? "Processed" : "Not Processed";
                int coWorkCriticality = "PROCESSED".equals(coWorkStatus.getString("statusId")) ? 3 : 1;
                if (supplierId.equals(approveCompany)) {
                    entity.addProperty(new Property(null, "supplierWorkStatus", ValueType.PRIMITIVE, coWorkStatusDescription));
                    entity.addProperty(new Property(null, "applicationWorkCriticality", ValueType.PRIMITIVE, coWorkCriticality));
                } else if ("HG".equals(approveCompany)) {
                    entity.addProperty(new Property(null, "complianceWorkStatus", ValueType.PRIMITIVE, coWorkStatusDescription));
                    entity.addProperty(new Property(null, "supplierWorkCriticality", ValueType.PRIMITIVE, coWorkCriticality));
                } else {
                    entity.addProperty(new Property(null, "applicationWorkStatus", ValueType.PRIMITIVE, coWorkStatusDescription));
                    entity.addProperty(new Property(null, "complianceWorkCriticality", ValueType.PRIMITIVE, coWorkCriticality));
                }
            }
        }
    }
    return entityList;
}

