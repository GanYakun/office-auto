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

def generateFields(Map<String, Object> context){
    List<Entity> entityList = context.parameters.entityList;
    Delegator delegator = context.get("delegator");
    entityList.each { entity ->
        GenericValue supplierParty = (GenericValue) entity.getGenericValue();
        String supplierId = supplierParty.getString("partyId");
        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierId, "approvePartyId", supplierId).queryFirst();
        if (UtilValidate.isNotEmpty(workEffort)) {
            entity.addProperty(new Property(null, "ddStatus", ValueType.PRIMITIVE, "PROCESSED".equals(workEffort.getString("currentStatusId")) ? "Processed" : "Not Processed"))
        }
    }
    return entityList;
}
