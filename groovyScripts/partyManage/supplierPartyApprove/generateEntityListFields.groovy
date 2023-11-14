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
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

module = "generateFields.groovy";

def generateFields(Map<String, Object> context){


    List<Entity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
//        disableEditSupplier = false
        criticalityValue = 1L
        String statusId = supplierParty.getString("currentStatusId");
        if (statusId.equals("NOT_PROCESSED") && UtilValidate.isNotEmpty(criticalityValue)){
            criticalityValue = 1L
        }else if (statusId.equals("PROCESSED")){
            criticalityValue = 3L
        }

//        if (supplierParty.get("currentStatusId").equals("PROCESSED")){
//            disableEditSupplier = true
//        }

        Debug.log("111111100");
        //全国组织机构统一社会信用代码 usccNumber
        GenericValue partyIdentification = EntityQuery.use(delegator).from("PartyIdentification")
                .where(UtilMisc.toMap("partyIdentificationTypeId", "USCC_OF_CHINESE_ORG", "partyId", supplierParty.getString("partyId"))).queryFirst();
        String usccNumber = partyIdentification == null ? null : partyIdentification.getString("idValue")
        entity.addProperty(new Property(null, "usccNumber", ValueType.PRIMITIVE, usccNumber))
        entity.addProperty(new Property(null, "criticalityValue", ValueType.PRIMITIVE, criticalityValue))
        entity.addProperty(new Property(null, "totalAmount", ValueType.PRIMITIVE, totalAmount))
//        entity.addProperty(new Property(null, "disableEditSupplier", ValueType.PRIMITIVE, disableEditSupplier))


    }
    return entityList;
}

def updateEntity(Map<String, Object> context){
    Debug.log("updateEntity start");

    fieldMap = new HashMap<>();
    GenericValue draftGenericValue = context.parameters.get("draftGenericValue");
    if (draftGenericValue != null) {
        fieldMap.putAll(draftGenericValue);
    } else {
        Entity entity = context.parameters.get("entity");
        fieldMap = Util.entityToMap(entity);
    }
    Map<String, Object> serviceParams = ServiceUtil.setServiceFields(dispatcher, "banfftech.updateParty", fieldMap, userLogin, null, locale);

    Map<String, Object> serviceResult = dispatcher.runSync("banfftech.updateParty", serviceParams);

    String partyId = fieldMap.get("partyId");

    if (ServiceUtil.isSuccess(serviceResult)) {
        if (UtilValidate.isNotEmpty(fieldMap.get("usccNumber"))) {
            GenericValue partyIdentification = EntityQuery.use(delegator).from("PartyIdentification")
                    .where(UtilMisc.toMap("partyIdentificationTypeId", "USCC_OF_CHINESE_ORG", "partyId", fieldMap.get("partyId"))).queryFirst();
            Map<String, Object> serviceMap = new HashMap<>();
            serviceMap.put("userLogin", Util.getSystemUser(delegator));
            serviceMap.put("partyId", fieldMap.get("partyId"));
            serviceMap.put("partyIdentificationTypeId", "USCC_OF_CHINESE_ORG");
            serviceMap.put("idValue", fieldMap.get("usccNumber"));
            serviceName = UtilValidate.isEmpty(partyIdentification) ? "banfftech.createPartyIdentification" : "banfftech.updatePartyIdentification"
            Map<String, Object> res = dispatcher.runSync(serviceName, serviceMap);
        }
    }
    GenericValue genericValue = delegator.findOne("Party", [partyId:partyId], false);

    return genericValue;
}