
import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilValidate;
import com.banfftech.bfdemo.worker.CriticalValueWorker;
import com.dpbird.odata.OdataEntityQuery
import org.apache.ofbiz.base.util.UtilMisc;
import com.banfftech.common.util.CommonUtils;

import org.apache.ofbiz.entity.util.EntityUtil;

import org.apache.ofbiz.entity.Delegator



module = "invoiceHeader.generateEntityListFields.groovy";
def generateFields(Map<String, Object> context) {
    List<Entity> entityList = context.parameters.get("entityList");
    Map<String, Object> statusMap = UtilMisc.toMap("INVOICE_GENERAL_READY",5L,"INVOICE_GENERAL_DEAL",2L,"INVOICE_GENERAL_PAID",3L,"INVOICE_GENERAL_CANCELLED",1L)
    BigDecimal totalAmount = new BigDecimal(0);
    BigDecimal totalAmountApplied = new BigDecimal(0);
    if (UtilValidate.isNotEmpty(entityList)){

        OdataOfbizEntity firstEntity = entityList.get(0);
        GenericValue getFirstEntity = (GenericValue) firstEntity.getGenericValue();
        if (getFirstEntity.containsKey("draftUUID")){

            return entityList;
        }
    }
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        invoiceHeader = odataOfbizEntity.getGenericValue();
        GenericValue invoice = odataOfbizEntity.getGenericValue();
        statusColor = 0L;
        Map<String, Boolean> map = new HashMap<>();
        map.put("INVOICE_GENERAL_DEAL", true);
        map.put("INVOICE_GENERAL_READY", true);
        map.put("INVOICE_GENERAL_PAID", true);
        map.put("INVOICE_GENERAL_CANCELLED", true);
        statusColor = CriticalValueWorker.getCriticalValue(statusMap, invoice.getString("statusId"));
        CommonUtils.checkStatus(invoiceHeader.getString("statusId"), map, delegator);


        List<GenericValue> invoiceItem =  invoice.getRelated("InvoiceItem",null,null,false);
        if (UtilValidate.isNotEmpty(invoiceItem)){
            for(GenericValue genericValue : invoiceItem){
                if((genericValue.getBigDecimal("quantity") == null) || (genericValue.getBigDecimal("amount") == null)){
                    continue;
                }
                BigDecimal eachResult = genericValue.getBigDecimal("amount").multiply(genericValue.getBigDecimal("quantity"));
                totalAmount = totalAmount.add(eachResult);
                totalAmount = totalAmount.setScale(2);
            }
            GenericValue sum2 = OdataEntityQuery.use(delegator).from("PaymentApplication")
                    .function("amountApplied","sum","sumTotal2")
                    .where(invoice.getPrimaryKey()).queryFirst();
            totalAmountApplied = sum2.getBigDecimal("sumTotal2");
            if(totalAmountApplied == null){
                totalAmountApplied = 0;
            }
            totalAmountApplied = totalAmountApplied.setScale(2);
        }

//        List<GenericValue> paymentApplication = invoice.getRelated("PaymentApplication",null,null,false);


        entity.addProperty(new Property(null, "cancelHidden", ValueType.PRIMITIVE, map.get("INVOICE_GENERAL_CANCELLED")));
        entity.addProperty(new Property(null, "dealHidden", ValueType.PRIMITIVE, map.get("INVOICE_GENERAL_DEAL")));
        entity.addProperty(new Property(null, "paidHidden", ValueType.PRIMITIVE, map.get("INVOICE_GENERAL_PAID")));
        entity.addProperty(new Property(null, "readyHidden", ValueType.PRIMITIVE, map.get("INVOICE_GENERAL_READY")));
        entity.addProperty(new Property(null, "totalAmount", ValueType.PRIMITIVE, totalAmount));
        entity.addProperty(new Property(null, "totalAmountApplied", ValueType.PRIMITIVE, totalAmountApplied));
        entity.addProperty(new Property(null, "statusColor", ValueType.PRIMITIVE, statusColor));
    }
    return entityList;


}

