import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc;
import com.dpbird.odata.OdataParts;
import com.banfftech.common.util.CommonUtils;
import org.apache.ofbiz.base.util.UtilValidate;

import java.util.Map;

module = "partyManage.shipment.statusControl.groovy";
def generateFields(Map<String, Object> context) {
    List<Entity> entityList = context.parameters.get("entityList");
    Boolean actionHidden = false
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        List<OdataParts> odataPartsList = (List<OdataParts>)odataOfbizEntity.getOdataParts();
        int odataPartsListSize = odataPartsList.size();
        GenericValue supplierParty = null;
        OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
        supplierParty = supplierPartyEntity.getGenericValue();
        if (supplierParty.get("currentStatusId").equals("PROCESSED")){
            actionHidden = true
        }

        entity.addProperty(new Property(null, "actionHidden", ValueType.PRIMITIVE, actionHidden));
    }
    return entityList;
}
