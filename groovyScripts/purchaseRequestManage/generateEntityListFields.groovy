import com.dpbird.odata.OfbizMapOdata
import com.dpbird.odata.edm.OdataOfbizEntity
import com.banfftech.bfdemo.worker.CriticalValueWorker;
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;

module = "Payment.generateEntityListFields.groovy";

def generateFields(Map<String, Object> context){
    List<Entity> entityList = context.parameters.get("entityList");
    Map<String, Object> statusMap = UtilMisc.toMap("CRQ_DRAFT",5L,"CRQ_CANCELLED",2L,"CRQ_ACCEPTED",3L,"CRQ_REVIEWED",1L)

    entityList.each { entity ->
//    获取entity Payment

        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        payment = odataOfbizEntity.getGenericValue();
        statusColor = 0L;
        statusId = payment.getString("statusId");
        statusColor = CriticalValueWorker.getCriticalValue(statusMap, statusId);

        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor));

    }
    return entityList;


}

