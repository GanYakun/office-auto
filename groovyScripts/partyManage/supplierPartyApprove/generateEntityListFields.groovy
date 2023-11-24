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
    Map<String, Object> ddFormDealMap = UtilMisc.toMap("Not Request", 5L, "Request", 2L, "Submitted", 3L, "Require Changes", 2L, "Wrong Types", 1L);
    entityList.each { entity ->
        String ddFormType;
        String ddFormDealStatus;
        ddFormTypeCritical = 0L;
        ddFormDealCritical = 0L;
        riskCritical = 0L;
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
        ddFormType = SupplierWorker.getDDFormType(supplierParty, delegator);
        ddFormDealStatus = SupplierWorker.getDDFormDealStatus(supplierParty, delegator);
        riskCritical = SupplierWorker.getClassificationCriticalValue(supplierParty, delegator);
        String cycleTime = SupplierWorker.calculateCycleTime(supplierParty, delegator);
        String ddResponseTime = SupplierWorker.calculateResponseTime(supplierParty, delegator);
        criticalityValue = 2L
        String statusId = supplierParty.getString("statusId");
        if (statusId.equals("PROCESSED") && UtilValidate.isNotEmpty(criticalityValue)){
            criticalityValue = 4L
        }else if (statusId.equals("NOT_PROCESSED")){
            criticalityValue = 1L
        }
        ddFormTypeCritical = CriticalValueWorker.getCriticalValue(ddFormTypeMap, ddFormType);
        ddFormDealCritical = CriticalValueWorker.getCriticalValue(ddFormDealMap, ddFormDealStatus);
        entity.addProperty(new Property(null, "ddFormType", ValueType.PRIMITIVE, ddFormType))
        entity.addProperty(new Property(null, "ddFormDealStatus", ValueType.PRIMITIVE, ddFormDealStatus))
        entity.addProperty(new Property(null, "criticalityValue", ValueType.PRIMITIVE, criticalityValue))
        entity.addProperty(new Property(null, "ddFormTypeCritical", ValueType.PRIMITIVE, ddFormTypeCritical))
        entity.addProperty(new Property(null, "cycleTime", ValueType.PRIMITIVE, cycleTime))
        entity.addProperty(new Property(null, "ddFormDealCritical", ValueType.PRIMITIVE, ddFormDealCritical))
        entity.addProperty(new Property(null, "riskCritical", ValueType.PRIMITIVE, riskCritical))
        entity.addProperty(new Property(null, "ddResponseTime", ValueType.PRIMITIVE, ddResponseTime))

        //文件数量
        List<GenericValue> fsList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "FINANCIAL_STATEMENTS"), null, false);
        List<GenericValue> caList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "CONFIDENTIALITY_AGREEMENT"), null, false);
        List<GenericValue> cpList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "COMPLIANCE_REPORT"), null, false);
        entity.addProperty(new Property(null, "fsStatus", ValueType.PRIMITIVE, fsList.size() < 1 ? "Not uploaded" : "Uploaded"))
        entity.addProperty(new Property(null, "caStatus", ValueType.PRIMITIVE, caList.size() < 1 ? "Not uploaded" : "Uploaded"))
        entity.addProperty(new Property(null, "cpStatus", ValueType.PRIMITIVE, cpList.size() < 1 ? "Not uploaded" : "Uploaded"))
        entity.addProperty(new Property(null, "fsStatusCriticality", ValueType.PRIMITIVE, fsList.size() < 1 ? 1 : 3))
        entity.addProperty(new Property(null, "caStatusCriticality", ValueType.PRIMITIVE, caList.size() < 1 ? 1 : 3))
        entity.addProperty(new Property(null, "cpStatusCriticality", ValueType.PRIMITIVE, cpList.size() < 1 ? 1 : 3))
        //DDFormPDF访问地址
        String url = null;
        String formName = null;
        if ("Submitted".equals(ddFormDealStatus)) {
            GenericValue genericValue = EntityQuery.use(delegator).from("PartyAttribute").where("partyId", supplierParty.getString("partyId"), "attrName", "ddFormType").queryFirst();
            if (UtilValidate.isNotEmpty(genericValue)) {
                String attrValue = genericValue.getString("attrValue");
                if ("SIMPLIFIED_DD".equals(attrValue)) {
                    formName = "Download DDForm";
                    url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/SimplifiedForm.pdf"
                };
                if ("STANDARD_DD".equals(attrValue)) {
                    url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/StandardForm.pdf"
                    formName = "Download DDForm";
                };
            }
        }
        entity.addProperty(new Property(null, "ddFromUrl", ValueType.PRIMITIVE, url));
        entity.addProperty(new Property(null, "ddFromName", ValueType.PRIMITIVE, formName));
        //WorkScope
        String workScope = null;
        GenericValue surveyQuestionAnswer = EntityQuery.use(delegator).from("SurveyQuestionAnswer").where(UtilMisc.toMap("partyId", supplierParty.getString("partyId"), "surveyQuestionId", "9004")).queryFirst();
        if (UtilValidate.isNotEmpty(surveyQuestionAnswer)) {
            workScope = surveyQuestionAnswer.getString("textResponse");
        }
        entity.addProperty(new Property(null, "workScope", ValueType.PRIMITIVE, workScope));
        GenericValue partyIdentification = EntityQuery.use(delegator).from("PartyIdentification")
                .where(UtilMisc.toMap("partyIdentificationTypeId", "USCC_OF_CHINESE_ORG", "partyId", supplierParty.getString("partyId"))).queryFirst();
        String usccNumber = partyIdentification == null ? null : partyIdentification.getString("idValue")
        //DDForm Bool Field
        entity.addProperty(new Property(null, "agent", ValueType.PRIMITIVE, true));
        entity.addProperty(new Property(null, "jvPartner", ValueType.PRIMITIVE, true));
        entity.addProperty(new Property(null, "subContractor", ValueType.PRIMITIVE, false));
        entity.addProperty(new Property(null, "consultantAdvisor", ValueType.PRIMITIVE, true));
        entity.addProperty(new Property(null, "supplierVendor", ValueType.PRIMITIVE, true));
        entity.addProperty(new Property(null, "distributor", ValueType.PRIMITIVE, false));
        entity.addProperty(new Property(null, "investmentTarget", ValueType.PRIMITIVE, false));
        entity.addProperty(new Property(null, "otherSpecify", ValueType.PRIMITIVE, false));
        entity.addProperty(new Property(null, "usccNumber", ValueType.PRIMITIVE, usccNumber));
    }
    return entityList;
}

