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
    Map<String, Object> statusMap = UtilMisc.toMap("MKTG_CAMP_PLANNED",2L,"MKTG_CAMP_APPROVED",1L,"MKTG_CAMP_APPROVED",5L,"MKTG_CAMP_COMPLETED",3L,"MKTG_CAMP_CANCELLED", 3L,"MKTG_CAMP_INPROGRESS", 2L)
    List<Entity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        GenericValue marketingCampaign = (GenericValue) entity.getGenericValue();
        String statusId = marketingCampaign.getString("statusId");
        statusColor = CriticalValueWorker.getCriticalValue(statusMap, statusId);


        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor))
    }
    return entityList;
}

