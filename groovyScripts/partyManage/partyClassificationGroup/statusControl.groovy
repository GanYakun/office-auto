import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc;

import org.apache.ofbiz.base.util.UtilValidate;

import java.util.Map;

module = "partyManage.shipment.statusControl.groovy";
def generateFields(Map<String, Object> context) {
    Map<String, Integer> shipmentStatusMap = UtilMisc.toMap("HIGH_LEVEL",1,"LOW_LEVEL",3,"MIDDLE_LEVEL",5);
    List<Entity> entityList = context.parameters.get("entityList");
    statusColor = 0L;
    String description = null;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue partyClassificationGroup = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(partyClassificationGroup)){
            description = partyClassificationGroup.getString("description");
        }

        if(partyClassificationGroup.get("partyClassificationTypeId").equals("RISQUE_RATING")){
            statusColor = shipmentStatusMap.get(description);
        }else {
            statusColor = 0L
        }
        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor));
    }
    return entityList;
}
