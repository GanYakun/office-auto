package com.banfftech.worker;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.joda.time.DateTimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class SupplierWorker {
    public static final Map<String, Object> DDType = UtilMisc.toMap("No DD", "NOT_DD",
            "Simplified", "SIMPLIFIED_DD", "Standard", "STANDARD_DD");
    /**
     * 判断供应商填写的ddForm类型
     * @param supplierParty 供应商
     * @param delegator
     * @return ddFormType
     */
    public static String getDDFormType (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String ddFormType = null;
        Boolean isGovernment = isGovernment((String) supplierParty.get("partyId"), delegator);
        Boolean isNoFormListCountry = isNoFormListCountry(supplierParty, delegator);

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
            ddFormType = "Simplified";
        }else if (productCategory.get("primaryParentCategoryId").equals("HIGH-VALUE_HIGH-RISK")){
            ddFormType = "Standard";
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

    private static Boolean isNoFormListCountry (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean isNoFormListCountry = false;
        List<GenericValue> partyGeos = delegator.findByAnd("PartyGeo", UtilMisc.toMap("partyId", supplierParty.get("partyId")), null, true);
            if (UtilValidate.isNotEmpty(partyGeos)){
                List<String> geoIds = EntityUtil.getFieldListFromEntityList(partyGeos, "geoId", false);
                if (geoIds.contains("USA")){
                    isNoFormListCountry = true;
                }else if (geoIds.contains("UK")){
                    isNoFormListCountry = true;
                }else if (geoIds.contains("UAE")){
                    isNoFormListCountry = true;
                }else if (geoIds.contains("Europe")){
                    isNoFormListCountry = true;
                }
            }
        return isNoFormListCountry;
    }

    /**
     * 返回供应商填写ddForm情况
     * @param supplierParty 供应商
     * @param delegator
     * @return ddFormDealStatus
     */
    public static String getDDFormDealStatus (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean isSent = ddFormIsSent(delegator, supplierParty);
        Boolean isSubmit = ddFormIsSubmitted(delegator, supplierParty);
        Boolean isRequireChanges = ddFormRequireChanges(delegator, supplierParty);
        Boolean isWrongType = ddFormIsWrongType(delegator, supplierParty);
        if (isWrongType){
            return "Wrong Types";
        }else if (isRequireChanges){
            return "Request";
        }else if (isSubmit){
            return "Submitted";
        }else if (isSent){
            return "Request";
        }
        return "Not Request";
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

    private static Boolean ddFormIsWrongType(Delegator delegator, GenericValue supplierParty) throws GenericEntityException{
        Boolean isWrongType = false;
        GenericValue partyAttribute = delegator.findOne("PartyAttribute",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "attrName", "ddFormType"), true);
        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(supplierWorkEfforts)){
            return false;
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        if (UtilValidate.isEmpty(partyAttribute)){
            return false;
        }
        String selectDDType = partyAttribute.getString("attrValue");
        String recommendDDType = getDDFormType(supplierParty, delegator);
        if (!selectDDType.equals(DDType.get(recommendDDType)) && supplierWorkEffort.get("currentStatusId").equals("NOT_PROCESSED")){
            isWrongType = true;
        }
        return isWrongType;
    }

    private static Boolean ddFormRequireChanges(Delegator delegator, GenericValue supplierParty) throws GenericEntityException {
        Boolean isRequireChanges = false;
        GenericValue ddFormStatusHistory = delegator.findOne("PartyAttribute",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "attrName", "ddFormStatusHistory"), true);
        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(supplierWorkEfforts)){
            return false;
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        if (UtilValidate.isNotEmpty(ddFormStatusHistory) && supplierWorkEffort.get("currentStatusId").equals("NOT_PROCESSED")){
            isRequireChanges = true;
        }
        return isRequireChanges;
    }

    /**
     * 计算表单填写周期
     * @param supplierParty 供应商
     * @param delegator
     * @return cycleTime
     */
    public static String calculateCycleTime (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {

        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "workEffortTypeId", "COWORK", "currentStatusId", "REGISTERED"), null, true);
        if (UtilValidate.isEmpty(supplierWorkEfforts)){
            return getDaysDifference(getFinishUploadDate(supplierParty, delegator), UtilDateTime.nowTimestamp());
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        return getDaysDifference(getFinishUploadDate(supplierParty, delegator), supplierWorkEffort.getTimestamp("lastModifiedDate"));

    }

    private static Timestamp getFinishUploadDate (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        GenericValue partyAttributeDate = delegator.findOne("PartyAttributeDate",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "attrName", "finishDomUploadDate"), true);
        if (UtilValidate.isEmpty(partyAttributeDate)){
            return UtilDateTime.nowTimestamp();
        }
        return partyAttributeDate.getTimestamp("attrValue");
    }

    //计算时间差，并转化为~days的时间格式
    private static String getDaysDifference (Timestamp startDate, Timestamp completionDate){
        String daysDifference = "0 days";
        if (UtilValidate.isEmpty(startDate)){
            startDate = UtilDateTime.nowTimestamp();
        }
        if (UtilValidate.isEmpty(completionDate)){
            completionDate = UtilDateTime.nowTimestamp();
        }
        long startDateSeconds = startDate.getTime();
        long completionDateSeconds = completionDate.getTime();
        long differenceSeconds = completionDateSeconds - startDateSeconds;
        double differenceDaysFloat = (double) differenceSeconds / (3600*1000*24);
        double days = Math.floor(differenceDaysFloat);
        if (days < 0){
            return "0 days";
        }
        daysDifference = (int) days + " days";
        return daysDifference;
    }

    /**
     * 计算表单填写周期
     * @param supplierParty 供应商
     * @param delegator
     * @return responseTime
     */
    public static String calculateResponseTime (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {

        List<GenericValue> supplierWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(supplierWorkEfforts)){
           return "0 days";
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        return getDaysDifference(supplierWorkEffort.getTimestamp("createdDate"), getFinishUploadDate(supplierParty, delegator));
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

    public static Boolean isCheckWarning (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        GenericValue checkAttributeEntity = delegator.findOne("PartyAttribute", UtilMisc.toMap("partyId", supplierParty.get("partyId"), "attrName", "complianceCheckWarning"), true);
        if (UtilValidate.isNotEmpty(checkAttributeEntity)){
            return true;
        }else {
            return false;
        }

    }
}
