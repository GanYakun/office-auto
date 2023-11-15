import com.dpbird.odata.Util
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.olingo.commons.api.data.Entity
import com.banfftech.bfdemo.worker.CriticalValueWorker;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

module = "generateFields.groovy";

def generateFields(Map<String, Object> context){
    statusColor = 0L
    Map<String, Object> statusMap = UtilMisc.toMap("STAGE0001",2L,"STAGE0002",2L,"STAGE0003",5L,"STAGE0004",5L,"STAGE0005",3L)
    List<Entity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        GenericValue party = (GenericValue) entity.getGenericValue();
        String statusId = party.getString("opportunityStageId");
        statusColor = CriticalValueWorker.getCriticalValue(statusMap, statusId);

        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor))
    }
    return entityList;
}

