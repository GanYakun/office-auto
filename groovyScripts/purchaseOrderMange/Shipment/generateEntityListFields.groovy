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
    Map<String, Integer> shipmentStatusMap = UtilMisc.toMap("GENERAL_SHIP_CANCELLED",1,"GENERAL_SHIP_SEND",3,"GENERAL_SHIP_RECEIVED",5,"GENERAL_SHIP_SHIPPED",3);
    List<Entity> entityList = context.parameters.get("entityList");
    int shipmentStatus = 0;
    String statusId = null;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue shipment = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(shipment)){
            statusId = shipment.getString("statusId");
        }
        shipmentStatus = shipmentStatusMap.get(statusId);
        entity.addProperty(new Property(null, "shipmentStatus", ValueType.PRIMITIVE, shipmentStatus));
    }
    return entityList;
}
