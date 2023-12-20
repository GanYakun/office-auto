package com.banfftech.worker;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CriticalValueWorker {
    /**
     * 根据状态传值
     * @param statusMap 状态对应值Map
     * @param statusId 状态ID
     * @return criticalValue
     */
    public static Long getCriticalValue (Map<String, Object> statusMap, String statusId){
        Long criticalValue = 0L;
        criticalValue = (Long) statusMap.get(statusId);
        return criticalValue;
    }

    /**
     *
     * @param partNumber 占比值
     * @param totalNumber 总数
     * @param passRate 合格率
     * @param excelRate 优秀率
     * @return criticalValue
     */
    public static int giveValueBasedRateCalculated (BigDecimal totalNumber, BigDecimal partNumber, BigDecimal passRate, BigDecimal excelRate){
        int criticalValue = 0;
        BigDecimal rateNumber = new BigDecimal(0);
        if (UtilValidate.isNotEmpty(totalNumber) && totalNumber.compareTo(BigDecimal.ZERO) != 0){
            rateNumber = partNumber.divide(totalNumber, 6);
            if (rateNumber.compareTo(passRate) <= 0){
                criticalValue = 1;
            }else if (rateNumber.compareTo(passRate) > 0 && rateNumber.compareTo(excelRate) <= 0){
                criticalValue = 2;
            }else {
                criticalValue = 3;
            }
            return criticalValue;
        }else {
            return criticalValue;
        }
    }

    public static Long getProcessCritical (Long processNumeric) {
        if (UtilValidate.isEmpty(processNumeric)){
            return 0L;
        }
        if (processNumeric == 0L){
            return 0L;
        }else if (0L < processNumeric && processNumeric <= 3L){
            return 1L;
        }else if (3L < processNumeric && processNumeric <= 5L){
            return 2L;
        }else {
            return 3L;
        }
    }

    /**
     * 返回供应商风险评级颜色显示值
     * @param supplierParty 供应商
     * @param delegator
     * @return criticalValue
     */
    public static Long getClassificationCriticalValue (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Long criticalValue = 0L;
        Map<String, Object> statusMap = UtilMisc.toMap("High",1L,"Low",3L,"Middle",4L);
        List<GenericValue> partyClassifications = delegator.findByAnd("PartyClassification",
                UtilMisc.toMap("partyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isNotEmpty(partyClassifications)){
            GenericValue partyClassification = EntityUtil.getFirst(partyClassifications);
            GenericValue partyClassificationGroup = delegator.findOne("PartyClassificationGroup",
                    UtilMisc.toMap("partyClassificationGroupId", partyClassification.get("partyClassificationGroupId")), true);
            String description = partyClassificationGroup.getString("description");
            criticalValue = (Long) statusMap.get(description);
        }
        return criticalValue;
    }
}
