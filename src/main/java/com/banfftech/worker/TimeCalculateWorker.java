package com.banfftech.worker;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TimeCalculateWorker {
    public static Timestamp getLastSubmittedDate (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        List<GenericValue> workEffortAssignments = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", supplierParty.get("workEffortId")), null, true);
        GenericValue workEffortAssignment = EntityUtil.getFirst(workEffortAssignments);
        String approvePartyId = workEffortAssignment.getString("partyId");
        if (approvePartyId.equals("IT")){
            return getStatusChangeDateTime(supplierParty, delegator, "PROCESSED");
        }else if (approvePartyId.equals("CG")){
            return getStatusChangeDateTime(supplierParty, delegator, "COMPLIANCE_REVIEW");
        }else {
            return null;
        }
    }

    private static Timestamp getStatusChangeDateTime (GenericValue supplierParty, Delegator delegator, String statusId) throws GenericEntityException {
        List<String> orderBy = new ArrayList<>();
        orderBy.add("-statusDatetime");
        GenericValue workEffortStatus = EntityQuery.use(delegator).from("WorkEffortStatus").
                where(UtilMisc.toMap("workEffortId", supplierParty.get("workEffortId"), "statusId", statusId)).
                orderBy(orderBy).queryFirst();
        if (UtilValidate.isNotEmpty(workEffortStatus)){
            return workEffortStatus.getTimestamp("statusDatetime");
        }else {
            return null;
        }
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
            return getDaysDifference(UtilDateTime.nowTimestamp(), getFinishUploadDate(supplierParty, delegator));
        }
        GenericValue supplierWorkEffort = EntityUtil.getFirst(supplierWorkEfforts);
        return getDaysDifference(supplierWorkEffort.getTimestamp("createdDate"), getFinishUploadDate(supplierParty, delegator));
    }

    //查询文件上传结束时间
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

}
