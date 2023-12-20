import com.dpbird.odata.Util
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.olingo.commons.api.data.Entity
import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.base.util.UtilValidate;
import java.math.BigDecimal;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.base.util.UtilMisc;
import com.banfftech.worker.SupplierWorker;
import com.banfftech.worker.CriticalValueWorker;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType
import sun.net.www.content.text.Generic


import java.sql.Timestamp;

module = "generateFields.groovy";

def generateFields(Map<String, Object> context) {
    List<Entity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) entity;
        String findEntityName = "RelationshipAndToParty";
        if (UtilValidate.isNotEmpty(ofbizEntity.getPropertyValue("draftUUID"))) {
            findEntityName = "RelationshipAndToPartyDraft";
        }
        GenericValue genericValue = EntityQuery.use(delegator).from(findEntityName)
                .where("partyRelationshipId", ofbizEntity.getPropertyValue("partyRelationshipId")).queryFirst();
        Object fileContent = genericValue.get("dataResourceContent")
        String partyRelationshipId = genericValue.getString("partyRelationshipId")
        if (UtilValidate.isNotEmpty(fileContent)) {
            entity.addProperty(new Property(null, "fileUrl",ValueType.PRIMITIVE, "/officeauto/control/odataAppSvc/supplierApproveService/RelationshipAndToParties('" + partyRelationshipId + "')/dataResourceContent"))
        }
        entity.addProperty(new Property(null, "addNameForUpload",ValueType.PRIMITIVE, UtilValidate.isNotEmpty(fileContent) ? "Download" : null))
        entity.addProperty(new Property(null, "uploadDocCritical",ValueType.PRIMITIVE, UtilValidate.isNotEmpty(fileContent) ? 3L : 1L))
    }
    return entityList;
}

