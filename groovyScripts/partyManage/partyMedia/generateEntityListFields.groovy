
import com.dpbird.odata.Util
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.olingo.commons.api.data.Entity
import com.banfftech.common.util.CommonUtils;
import com.banfftech.worker.SupplierWorker;
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
        GenericValue partyMedia = (GenericValue) entity.getGenericValue();
        uploadDocCritical = 0L;
        uploadDocCritical = SupplierWorker.getUploadDocCriticalValue(partyMedia, delegator);
        addNameForUpload = SupplierWorker.addNameForUpload(partyMedia, delegator);
                String partyContentId = partyMedia.getString("partyContentId")
        String url = "/officeauto/control/odataAppSvc/supplierApproveService/PartyMediaResources('" + partyContentId + "')/otherData"
        entity.addProperty(new Property(null, "fileUrl", ValueType.PRIMITIVE, url))
        entity.addProperty(new Property(null, "uploadDocCritical", ValueType.PRIMITIVE, uploadDocCritical))
        entity.addProperty(new Property(null, "addNameForUpload", ValueType.PRIMITIVE, addNameForUpload))
    }
    return entityList;
}

