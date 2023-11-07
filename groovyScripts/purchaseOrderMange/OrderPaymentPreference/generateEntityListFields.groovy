import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc;

import org.apache.ofbiz.base.util.UtilValidate;

import java.util.Map;

module = "purchaseManage.OrderHeader.generateEntityListFields.groovy";
def generateFields(Map<String, Object> context) {
    Map<String, Integer> orderPaymentStatusMap = UtilMisc.toMap("PAYMENT_GENERAL_PAID", 1, "PAYMENT_GENERAL_UNPAID", 2,"PAYMENT_GENERAL_CONFIRM",3,"PAYMENT_GENERAL_CANCELLED",5);
    List<Entity> entityList = context.parameters.get("entityList");
    int orderPaymentStatus = 0;
    String statusId = null;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue orderPaymentPreference = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(orderPaymentPreference)){
            statusId = orderPaymentPreference.getString("statusId");
        }
        orderPaymentStatus = orderPaymentStatusMap.get(statusId);
        entity.addProperty(new Property(null, "orderPaymentStatus", ValueType.PRIMITIVE, orderPaymentStatus));
    }
    return entityList;
}
