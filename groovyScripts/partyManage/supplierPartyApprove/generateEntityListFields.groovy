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
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.base.util.UtilMisc;
import com.banfftech.worker.SupplierWorker;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType
import sun.net.www.content.text.Generic;

module = "generateFields.groovy";

def generateFields(Map<String, Object> context){
    List<Entity> entityList = context.parameters.entityList;
    Delegator delegator = context.get("delegator");
    entityList.each { entity ->
        String ddFormType;
        String ddFormDealStatus;
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
        ddFormType = SupplierWorker.getDDFormType(supplierParty, delegator);
        ddFormDealStatus = SupplierWorker.getDDFormDealStatus(supplierParty, delegator);
        criticalityValue = 2L
        String statusId = supplierParty.getString("statusId");
        if (statusId.equals("PROCESSED") && UtilValidate.isNotEmpty(criticalityValue)){
            criticalityValue = 4L
        }else if (statusId.equals("NOT_PROCESSED")){
            criticalityValue = 1L
        }
        entity.addProperty(new Property(null, "ddFormType", ValueType.PRIMITIVE, ddFormType))
        entity.addProperty(new Property(null, "ddFormDealStatus", ValueType.PRIMITIVE, ddFormDealStatus))
        entity.addProperty(new Property(null, "criticalityValue", ValueType.PRIMITIVE, criticalityValue))

    }
    return entityList;
}

