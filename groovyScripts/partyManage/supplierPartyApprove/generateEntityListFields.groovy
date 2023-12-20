
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.base.util.UtilMisc;
import com.banfftech.worker.SupplierWorker;
import com.banfftech.worker.TimeCalculateWorker;
import com.banfftech.worker.HiddenWorker;
import com.banfftech.worker.NumericWorker;
import com.banfftech.worker.CriticalValueWorker;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType


import java.sql.Timestamp;

module = "generateFields.groovy";

def generateFields(Map<String, Object> context){
    List<Entity> entityList = context.parameters.entityList;
    Delegator delegator = context.get("delegator");
    Map<String, Object> ddFormTypeMap = UtilMisc.toMap("No DD", 1L, "Simplified", 2L, "Standard", 3L);
    Map<String, Object> vendorTypeMap = UtilMisc.toMap("GOVERNMENTAL_AGENCIES_TYPE", 1L, "HIGHLY_REGULATED_ENTITY_TYPE", 1L, "LOW_VALUE_ONE-TIME_TRANSACTIONS_TYPE", 2L, "OTHERS_TYPE", 3L, "ROUTINE_AND_LOW_VALUE_TRANSACTIONS_TYPE", 5L);
    Map<String, Object> priorityMap = UtilMisc.toMap("PRIORITY_HIGH", 1L, "PRIORITY_MEDIUM", 3L, "PRIORITY_LOW", 5L);
    entityList.each { entity ->
        String ddFormType;
        String ddFormTypeId;
        ddFormTypeCritical = 0L;
        checkWarningCritical = 0L;
        vendorTypeCritical = 0L;
        ratingNumeric = 0L;
        processNumeric = 0L;
        processCritical = 0L;
        priorityCritical = 0L;
        riskCritical = 0L;
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
        Boolean applicantSubmitHidden = HiddenWorker.applicantSubmitIsHidden(supplierParty, delegator);
        Boolean procurementSubmitHidden = HiddenWorker.procurementSubmitIsHidden(supplierParty, delegator);
        Boolean procurementRejectHidden = HiddenWorker.procurementRejectIsHidden(supplierParty, delegator);
        //返回提示procurement的警告内容
        String warningContent = SupplierWorker.getWarningContent(supplierParty, delegator);
        //返回供应商要填写的ddForm类型ID
        ddFormType = SupplierWorker.getDDFormType(supplierParty, delegator);
        //返回供应商要填写的ddForm类型ID
        ddFormTypeId = SupplierWorker.getDDFormTypeId(ddFormType);
        //根据Compliance评级返回数字，用于封装评级星星图
        ratingNumeric = NumericWorker.getClassificationRatingNumber(supplierParty, delegator);
        //根据Cowork状态返回数字，用于封装流程图
        processNumeric = NumericWorker.getProcessNumeric(supplierParty, delegator);
        //ddForm是否已提交
        Boolean ddFormIsSubmitted = SupplierWorker.ddFormIsSubmitted(delegator, supplierParty);
        //查询applicant最后提交日期
        Timestamp lastSubmittedDate = TimeCalculateWorker.getLastSubmittedDate(supplierParty, delegator);
        //查询cycleTime（逻辑未定）
        String cycleTime = TimeCalculateWorker.calculateCycleTime(supplierParty, delegator);
        String complianceCycleTime = TimeCalculateWorker.calculateCycleTime(supplierParty, delegator);
        riskCritical = CriticalValueWorker.getClassificationCriticalValue(supplierParty, delegator);
        processCritical = CriticalValueWorker.getProcessCritical(processNumeric);
        vendorTypeCritical = vendorTypeMap.get(supplierParty.get("partyGroupTypeId"));
        if (UtilValidate.isNotEmpty(supplierParty.get("priority"))){
            priorityCritical = priorityMap.get(supplierParty.get("priority"));
        }


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
        entity.addProperty(new Property(null, "warningContent", ValueType.PRIMITIVE, warningContent))
        entity.addProperty(new Property(null, "applicantSubmitHidden", ValueType.PRIMITIVE, applicantSubmitHidden))
        entity.addProperty(new Property(null, "complianceCycleTime", ValueType.PRIMITIVE, complianceCycleTime))
        entity.addProperty(new Property(null, "procurementSubmitHidden", ValueType.PRIMITIVE, procurementSubmitHidden))
        entity.addProperty(new Property(null, "procurementRejectHidden", ValueType.PRIMITIVE, procurementRejectHidden))

        //文件数量
        String supplierId = supplierParty.getString("partyId");
        //DDFormPDF访问地址
        String url = null;
        String formName = null;
        if (ddFormIsSubmitted) {
            if ("SIMPLIFIED_DD".equals(ddFormTypeId)) {
                formName = "Download";
                url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/SimplifiedForm.pdf"
            };
            if ("STANDARD_DD".equals(ddFormTypeId)) {
                url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/StandardForm.pdf"
                formName = "Download";
            };
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
                .where(UtilMisc.toMap("partyIdentificationTypeId", "USCC_OF_CHINESE_ORG", "partyId", supplierId)).queryFirst();
        String usccNumber = partyIdentification == null ? null : partyIdentification.getString("idValue")
        entity.addProperty(new Property(null, "usccNumber", ValueType.PRIMITIVE, usccNumber));
        entity.addProperty(new Property(null, "checkWarningCritical", ValueType.PRIMITIVE, checkWarningCritical));

    }
    return entityList;
}

