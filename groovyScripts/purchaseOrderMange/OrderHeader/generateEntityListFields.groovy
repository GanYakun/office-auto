
import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import com.dpbird.odata.OdataEntityQuery
import org.apache.ofbiz.base.util.UtilMisc;
import com.banfftech.worker.CriticalValueWorker;
import org.apache.ofbiz.entity.Delegator
import java.math.BigDecimal;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;

module = "purchaseManage.OrderHeader.generateEntityListFields.groovy";
def generateFields(Map<String, Object> context) {
    Map<String, Integer> orderHeaderStatusMap = UtilMisc.toMap("ORDER_CANCELLED",1,"ORDER_SENT",5,"ORDER_PROCESSING",5,"ORDER_CREATED",5,"ORDER_HOLD",2,"ORDER_APPROVED",3,"ORDER_COMPLETED",3);
    int orderHeaderStatus = 0;
    int chartCriticalValue = 0;
    int donutChartCriticalValue = 0;

    String statusId = null;
    List<Entity> entityList = context.parameters.get("entityList");
    BigDecimal temp = new BigDecimal("0");
    BigDecimal amountPaid = new BigDecimal("0");
    BigDecimal receivedQuantity = new BigDecimal("0");
    BigDecimal targetQuantity = new BigDecimal("0");
    String orderId = null;
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        if (UtilValidate.isNotEmpty(odataOfbizEntity)){
            GenericValue orderHeader = odataOfbizEntity.getGenericValue();
            statusId = orderHeader.getString("statusId");
            orderHeaderStatus = orderHeaderStatusMap.get(statusId);
            orderId = orderHeader.getString("orderId");

//            List<String> statuses = UtilMisc.toList("PAYMENT_SETTLED","PAYMENT_RECEIVED");
//            EntityCondition statusesCondition = EntityCondition.makeCondition("statusId", EntityOperator.IN, statuses);
//            EntityCondition orderCondition = EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId);
//            EntityCondition conditions = EntityCondition.makeCondition(statusesCondition, EntityOperator.AND, orderCondition);

//            List<GenericValue> orderPaymentPreferences = delegator.findList("OrderPaymentPreference", conditions, null, null, null, false);
            List<GenericValue> allOrderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId), null, false);

//            if (UtilValidate.isNotEmpty(orderPaymentPreferences)){
//                for(GenericValue orderPaymentPreference : orderPaymentPreferences){
//                    temp = orderPaymentPreference.getBigDecimal('maxAmount');
//                    if(temp != null){
//                        amountPaid = amountPaid.add(temp);
//                    }
//                }
//            }

            for(GenericValue orderItem : allOrderItems){
                temp = orderItem.getBigDecimal('quantity');
                if(temp != null){
                    targetQuantity = targetQuantity.add(temp);
                }
                GenericValue inventoryItem = orderItem.getRelatedOne("FromInventoryItem", false);
                if(UtilValidate.isNotEmpty(inventoryItem)){
                    List<GenericValue> inventoryItemDetails =inventoryItem.getRelated("InventoryItemDetail", null, null, false)
                    for(GenericValue inventoryItemDetail : inventoryItemDetails){
                        temp = inventoryItemDetail.getBigDecimal('quantityOnHandDiff');
                        if(temp != null){
                            if (temp.compareTo(BigDecimal.ZERO)==1){
                                receivedQuantity = receivedQuantity.add(temp);
                            }
                        }
                    }
                }
            }
            BigDecimal grandTotal = orderHeader.getBigDecimal("grandTotal");
            Random random = new Random();
            int grandTotalInt = grandTotal.intValue();
            int subTotal = random.nextInt(grandTotalInt);
            amountPaid = grandTotal.subtract(BigDecimal.valueOf(subTotal))


            donutChartCriticalValue = CriticalValueWorker.giveValueBasedRateCalculated(grandTotal, amountPaid, BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.6))
            chartCriticalValue = CriticalValueWorker.giveValueBasedRateCalculated(targetQuantity, receivedQuantity, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.7))


        }

        entity.addProperty(new Property(null, "amountPaid", ValueType.PRIMITIVE, amountPaid));
        entity.addProperty(new Property(null, "receivedQuantity", ValueType.PRIMITIVE, receivedQuantity.setScale(2)));
        entity.addProperty(new Property(null, "targetQuantity", ValueType.PRIMITIVE, targetQuantity.setScale(2)));
        entity.addProperty(new Property(null, "orderHeaderStatus", ValueType.PRIMITIVE, orderHeaderStatus));
        entity.addProperty(new Property(null, "chartCriticalValue", ValueType.PRIMITIVE, chartCriticalValue));
        entity.addProperty(new Property(null, "donutChartCriticalValue", ValueType.PRIMITIVE, donutChartCriticalValue));
    }
    return entityList;


}
