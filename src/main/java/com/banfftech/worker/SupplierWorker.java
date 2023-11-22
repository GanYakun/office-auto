package com.banfftech.worker;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class SupplierWorker {
    /**
     * 判断供应商填写的ddForm类型
     * @param supplierParty 供应商
     * @param delegator
     * @return ddFormType
     */
    public static String getDDFormType (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String ddFormType = null;
        Boolean isGovernment = isGovernment((String) supplierParty.get("partyId"), delegator);
        Boolean isNoFormListCountry = isNoFormListCountry();

        List<GenericValue> productCategoryRoles = delegator.findByAnd("ProductCategoryRole",
                UtilMisc.toMap("partyId", supplierParty.get("partyId")), null, false);
        if (UtilValidate.isEmpty(productCategoryRoles)){
            return null;
        }
        GenericValue productCategoryRole = EntityUtil.getFirst(productCategoryRoles);
        GenericValue productCategory = delegator.findOne("ProductCategory",
                UtilMisc.toMap("productCategoryId", productCategoryRole.get("productCategoryId")), true);

        if (isGovernment || isNoFormListCountry || productCategory.get("primaryParentCategoryId").equals("NEGLIGIBLE_RISK")){
            ddFormType = "No DD";
        }else if (productCategory.get("primaryParentCategoryId").equals("LOW_RISK")){
            ddFormType = "Simplified DD";
        }else if (productCategory.get("primaryParentCategoryId").equals("HIGH-VALUE_HIGH-RISK")){
            ddFormType = "Standard DD";
        }
        return ddFormType;
    }

    private static Boolean isGovernment (String partyId, Delegator delegator) throws GenericEntityException {
        Boolean isGovernment = false;
        List<GenericValue> partyRoles = delegator.findByAnd("PartyRole",
                UtilMisc.toMap("partyId", partyId), null, true);
        for (GenericValue partyRole : partyRoles){
            if (partyRole.get("roleTypeId").equals("GOVERNMENT_SUPPLIER")){
                isGovernment = true;
                break;
            }
        }
        return isGovernment;
    }

    private static Boolean isNoFormListCountry (){
        Boolean isNoFormListCountry = false;

        return isNoFormListCountry;
    }

    /**
     * 返回供应商填写ddForm情况
     * @param supplierParty 供应商
     * @param delegator
     * @return ddFormDealStatus
     */
    public static String getDDFormDealStatus (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String ddFormDealStatus = "Not Send To Vendor";
        Boolean isSent = ddFormIsSent(delegator, supplierParty);
        Boolean isSubmit = ddFormIsSubmitted(delegator, supplierParty);
        if (isSent && !isSubmit){
            ddFormDealStatus = "Not Submit";
        }else if (isSent && isSubmit){
            ddFormDealStatus = "Submitted";
        }
        return ddFormDealStatus;
    }

    private static Boolean ddFormIsSent(Delegator delegator, GenericValue supplierParty) throws GenericEntityException {
        Boolean isSent = false;
        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isNotEmpty(supplierWorkEfforts)){
            isSent = true;
        }
        return isSent;
    }

    private static Boolean ddFormIsSubmitted(Delegator delegator, GenericValue supplierParty) throws GenericEntityException {
        Boolean isSubmit = false;
        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(supplierWorkEfforts)){
            return false;
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        if (supplierWorkEffort.get("currentStatusId").equals("PROCESSED")){
            isSubmit = true;
        }
        return isSubmit;
    }

    /**
     * 计算表单填写周期
     * @param supplierParty 供应商
     * @param delegator
     * @return cycleTime
     */
    public static String calculateCycleTime (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String cycleTime = "0h0min";
        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(supplierWorkEfforts)){
            return "0 days";
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        Timestamp submitTime = supplierWorkEffort.getTimestamp("createdDate");
        Timestamp completeDDFormTime = supplierWorkEffort.getTimestamp("lastModifiedDate");
        //计算时间差，并转化为~days的时间格式
        long submitTimeSecond = submitTime.getTime();
        long completeDDFormTimeSecond = completeDDFormTime.getTime();
        long cycleTimeSecond = completeDDFormTimeSecond - submitTimeSecond;
        double cycleTimeFloat = (double) cycleTimeSecond / (3600*1000*24);
        double days = Math.floor(cycleTimeFloat);
//        double hours = (cycleTimeFloat - days)*24;
        cycleTime = (int) days + " days";
        return cycleTime;
    }

    /**
     * 返回供应商风险评级颜色显示值
     * @param supplierParty 供应商
     * @param delegator
     * @return criticalValue
     */
    public static Long getClassificationCriticalValue (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Long criticalValue = 0L;
        Map<String, Object> statusMap = UtilMisc.toMap("High Level",1L,"Low Level",3L,"Middle Level",5L);
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
