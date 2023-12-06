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
    Map<String, Integer> statusMap = UtilMisc.toMap("High",1,"Low",3,"Medium",5);
    Map<String, Object> ratingMap = UtilMisc.toMap("High",5L,"Low",1L,"Medium",3L);
    List<Entity> entityList = context.parameters.get("entityList");
    statusColor = 0L;
    ratingNumeric = 0L;
    String description = null;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue partyClassification = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(partyClassification)){
            GenericValue partyClassificationGroup = delegator.findOne("PartyClassificationGroup", UtilMisc.toMap("partyClassificationGroupId", partyClassification.get("partyClassificationGroupId")), true);
            if (UtilValidate.isNotEmpty(partyClassificationGroup)){
                description = partyClassificationGroup.getString("description");
                statusColor = statusMap.get(description);
                ratingNumeric = ratingMap.get(description);
            }
        }
        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor));
        entity.addProperty(new Property(null, "ratingNumeric", ValueType.PRIMITIVE, ratingNumeric));
    }
    return entityList;
}
