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
    Map<String, Object> ddFormTypeMap = UtilMisc.toMap("No DD", 1L, "Simplified DD", 2L, "Standard DD", 3L);
    Map<String, Object> ddFormDealMap = UtilMisc.toMap("Not Send To Vendor", 1L, "Not Submit", 2L, "Submitted", 3L);
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


        //文件数量
        List<GenericValue> fsList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "FINANCIAL_STATEMENTS"), null, false);
        List<GenericValue> caList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "CONFIDENTIALITY_AGREEMENT"), null, false);
        List<GenericValue> tfList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "OTHER_FILES"), null, false);
        entity.addProperty(new Property(null, "fsStatus", ValueType.PRIMITIVE, fsList.size() < 1 ? "Not uploaded" : "Uploaded"))
        entity.addProperty(new Property(null, "caStatus", ValueType.PRIMITIVE, caList.size() < 1 ? "Not uploaded" : "Uploaded"))
        entity.addProperty(new Property(null, "ofStatus", ValueType.PRIMITIVE, tfList.size() < 1 ? "Not uploaded" : "Uploaded"))
        entity.addProperty(new Property(null, "fsStatusCriticality", ValueType.PRIMITIVE, fsList.size() < 1 ? 1 : 3))
        entity.addProperty(new Property(null, "caStatusCriticality", ValueType.PRIMITIVE, caList.size() < 1 ? 1 : 3))
        entity.addProperty(new Property(null, "ofStatusCriticality", ValueType.PRIMITIVE, tfList.size() < 1 ? 1 : 3))
        //DDFormPDF访问地址
        String url = null;
        String name = null;
        if ("Submitted".equals(ddFormDealStatus)) {
            GenericValue genericValue = EntityQuery.use(delegator).from("PartyAttribute").where("partyId", supplierParty.getString("partyId"), "attrName", "ddFormType").queryFirst();
            if (UtilValidate.isNotEmpty(genericValue)) {
                String attrValue = genericValue.getString("attrValue");
                if ("SIMPLIFIED_DD".equals(attrValue)) {
                    url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/SimplifiedForm.pdf"
                    name = "Business Partner Due Diligence Form – Simplified Form"
                };
                if ("STANDARD_DD".equals(attrValue)) {
                    url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/StandardForm.pdf"
                    name = "Business Partner Due Diligence Form – Standard Form"
                };
            }
        }
        entity.addProperty(new Property(null, "ddFromUrl", ValueType.PRIMITIVE, url));
        entity.addProperty(new Property(null, "ddFromName", ValueType.PRIMITIVE, name));
        //WorkScope
        String workScope = null;
        GenericValue surveyQuestionAnswer = EntityQuery.use(delegator).from("SurveyQuestionAnswer").where(UtilMisc.toMap("partyId", supplierParty.getString("partyId"), "surveyQuestionId", "9004")).queryFirst();
        if (UtilValidate.isNotEmpty(surveyQuestionAnswer)) {
            workScope = surveyQuestionAnswer.getString("textResponse");
        }
        entity.addProperty(new Property(null, "workScope", ValueType.PRIMITIVE, workScope));
    }
    return entityList;
}

