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
    Map<String, Integer> shipmentStatusMap = UtilMisc.toMap("高等风险评级",1,"低等风险评级",3,"中等风险评级",5);
    List<Entity> entityList = context.parameters.get("entityList");
    statusColor = 0L;
    String description = null;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue partyClassificationGroup = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(partyClassificationGroup)){
            description = partyClassificationGroup.getString("description");
        }
        statusColor = shipmentStatusMap.get(description);
        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor));
    }
    return entityList;
}
