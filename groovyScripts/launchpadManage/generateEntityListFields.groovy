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
        GenericValue partyUserLogin = (GenericValue) entity.getGenericValue();
        String type = null;
        GenericValue relationship = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdTo", partyUserLogin.getString("partyId")).queryFirst();
        if (UtilValidate.isNotEmpty(relationship)) {
            String companyId = relationship.getString("partyIdFrom");
            GenericValue party = EntityQuery.use(delegator).from("Party").where(UtilMisc.toMap("partyId", companyId)).queryFirst();
            GenericValue typeAttr = CommonUtils.getObjectAttributeGv(party, "ddFormType")
            if (UtilValidate.isNotEmpty(typeAttr)) {
                type = typeAttr.getString("attrValue");
            }
        }
        entity.addProperty(new Property(null, "ddFormType", ValueType.PRIMITIVE, type))
    }
    return entityList;
}

