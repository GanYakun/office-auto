package com.banfftech.worker;

import org.apache.ofbiz.base.util.UtilValidate;

import java.math.BigDecimal;
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


}
