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
    Map<String, Object> ddFormTypeMap = UtilMisc.toMap("No DD", 1L, "Simplified DD", 2L, "Standard DD", 5L);
    entityList.each { entity ->
        String ddFormType;
        String ddFormDealStatus;
        ddFormTypeCritical = 0L;
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
        ddFormType = SupplierWorker.getDDFormType(supplierParty, delegator);
        ddFormDealStatus = SupplierWorker.getDDFormDealStatus(supplierParty, delegator);
        String cycleTime = SupplierWorker.calculateCycleTime(supplierParty, delegator);
        criticalityValue = 2L
        String statusId = supplierParty.getString("statusId");
        if (statusId.equals("PROCESSED") && UtilValidate.isNotEmpty(criticalityValue)){
            criticalityValue = 4L
        }else if (statusId.equals("NOT_PROCESSED")){
            criticalityValue = 1L
        }
        ddFormTypeCritical = CriticalValueWorker.getCriticalValue(ddFormTypeMap, ddFormType);
        entity.addProperty(new Property(null, "ddFormType", ValueType.PRIMITIVE, ddFormType))
        entity.addProperty(new Property(null, "ddFormDealStatus", ValueType.PRIMITIVE, ddFormDealStatus))
        entity.addProperty(new Property(null, "criticalityValue", ValueType.PRIMITIVE, criticalityValue))
        entity.addProperty(new Property(null, "ddFormTypeCritical", ValueType.PRIMITIVE, ddFormTypeCritical))
        entity.addProperty(new Property(null, "cycleTime", ValueType.PRIMITIVE, cycleTime))


        //文件数量
        List<GenericValue> fsList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "FINANCIAL_STATEMENTS"), null, false);
        List<GenericValue> caList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "CONFIDENTIALITY_AGREEMENT"), null, false);
        List<GenericValue> tfList = supplierParty.getRelated("PartyMediaResource", UtilMisc.toMap("partyContentTypeId", "OTHER_FILES"), null, false);
        entity.addProperty(new Property(null, "fsCount", ValueType.PRIMITIVE, fsList.size()))
        entity.addProperty(new Property(null, "caCount", ValueType.PRIMITIVE, caList.size()))
        entity.addProperty(new Property(null, "ofCount", ValueType.PRIMITIVE, tfList.size()))
        entity.addProperty(new Property(null, "fsCountCriticality", ValueType.PRIMITIVE, fsList.size() < 1 ? 1 : 3))
        entity.addProperty(new Property(null, "caCountCriticality", ValueType.PRIMITIVE, caList.size() < 1 ? 1 : 3))
        entity.addProperty(new Property(null, "ofCountCriticality", ValueType.PRIMITIVE, tfList.size() < 1 ? 1 : 3))
        //DDFormPDF访问地址
        String url = null;
        String name = null;
        if ("Simplified DD".equals(ddFormType)) {
            url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/SimplifiedForm.pdf"
            name = "Business Partner Due Diligence Form – Simplified Form"
        };
        if ("Standard DD".equals(ddFormType)) {
            url = "https://dpbird.oss-cn-hangzhou.aliyuncs.com/scy/StandardForm.pdf"
            name = "Business Partner Due Diligence Form – Standard Form"
        };
        entity.addProperty(new Property(null, "ddFromUrl", ValueType.PRIMITIVE, url));
        entity.addProperty(new Property(null, "ddFromName", ValueType.PRIMITIVE, name));
    }
    return entityList;
}

