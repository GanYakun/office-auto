import com.dpbird.odata.Util
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.olingo.commons.api.data.Entity
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
        BigDecimal totalAmount = new BigDecimal(620000)
        Random random = new Random();
        int addTotal = random.nextInt(300000);
        totalAmount = totalAmount.add(BigDecimal.valueOf(addTotal));
        criticalityValue = 1L
        String statusId = supplierParty.getString("statusId");
        if (statusId.equals("PARTY_ENABLED") && UtilValidate.isNotEmpty(criticalityValue)){
            criticalityValue = 3L
        }else if (statusId.equals("PARTY_SUSPEND")){
            criticalityValue = 2L
        }

        Debug.log("111111100");
        //全国组织机构统一社会信用代码 usccNumber
        GenericValue partyIdentification = EntityQuery.use(delegator).from("PartyIdentification")
                .where(UtilMisc.toMap("partyIdentificationTypeId", "USCC_OF_CHINESE_ORG", "partyId", supplierParty.getString("partyId"))).queryFirst();
        String usccNumber = partyIdentification == null ? null : partyIdentification.getString("idValue")
        entity.addProperty(new Property(null, "usccNumber", ValueType.PRIMITIVE, usccNumber))
        entity.addProperty(new Property(null, "criticalityValue", ValueType.PRIMITIVE, criticalityValue))
        entity.addProperty(new Property(null, "totalAmount", ValueType.PRIMITIVE, totalAmount))

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