import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.util.EntityUtil;

import org.apache.ofbiz.base.util.UtilValidate
import sun.net.www.content.text.Generic;

import java.util.Map;

module = "purchaseManage.OrderItem.generateEntityListFields.groovy";
def generateFields(Map<String, Object> context) {
    List<Entity> entityList = context.parameters.get("entityList");
    BigDecimal totalPrice = new BigDecimal("0.00");
    BigDecimal totalQuantity = new BigDecimal("0.00");
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue orderItem = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(orderItem)){
            quantity = orderItem.getBigDecimal("quantity");
            unitPrice = orderItem.getBigDecimal("unitPrice");
        }
        if (UtilValidate.isNotEmpty(quantity)){
            totalPrice = quantity.multiply(unitPrice)
        }




        entity.addProperty(new Property(null, "totalPrice", ValueType.PRIMITIVE, totalPrice.setScale(2)));
        entity.addProperty(new Property(null, "totalQuantity", ValueType.PRIMITIVE, totalQuantity));
    }
    return entityList;
}
