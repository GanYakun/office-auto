package com.banfftech.worker;

import com.banfftech.common.util.CommonUtils;
import org.apache.commons.net.ntp.TimeStamp;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.joda.time.DateTimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
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

        if (satisfiedNoDDRegular(supplierParty, delegator)){
            ddFormType = "No DD";
        }else if (simplifiedDDRegular(supplierParty, delegator)){
            ddFormType = "Simplified";
        }else if (standardDDRegular(supplierParty, delegator)){
            ddFormType = "Standard";
        }
        return ddFormType;
    }

    public static Boolean satisfiedNoDDRegular (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean isGovernment = isGovernment((String) supplierParty.get("partyId"), delegator);
        Boolean isNoFormListCountry = isNoFormListCountry(supplierParty, delegator);
        String vendorTypeEnumCode = getVendorTypeCode(supplierParty, delegator);

        if (isGovernment || isNoFormListCountry || vendorTypeEnumCode.equals("NO_DD")){
            return true;
        }else {
            return false;
        }

    }

    public static Boolean simplifiedDDRegular(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {

        String vendorTypeEnumCode = getVendorTypeCode(supplierParty, delegator);
        if (vendorTypeEnumCode.equals("SIMPLIFIED_DD")){
            return true;
        }else {
            return false;
        }
    }

    public static Boolean standardDDRegular(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {

        String vendorTypeEnumCode = getVendorTypeCode(supplierParty, delegator);
        if (vendorTypeEnumCode.equals("STANDARD_DD")){
            return true;
        }else {
            return false;
        }
    }

    private static String getSupplierParentCategory (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        List<GenericValue> productCategoryRoles = delegator.findByAnd("ProductCategoryRole",
                UtilMisc.toMap("partyId", supplierParty.get("partyId")), null, false);
        if (UtilValidate.isEmpty(productCategoryRoles)){
            return "";
        }
        GenericValue productCategoryRole = EntityUtil.getFirst(productCategoryRoles);
        GenericValue productCategory = delegator.findOne("ProductCategory",
                UtilMisc.toMap("productCategoryId", productCategoryRole.get("productCategoryId")), true);
        return productCategory.getString("primaryParentCategoryId");
    }

    private static String getVendorTypeCode(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String partyGroupTypeId = supplierParty.getString("partyGroupTypeId");
        GenericValue PartyGroupType = delegator.findOne("PartyGroupType", UtilMisc.toMap("partyGroupTypeId", partyGroupTypeId), true);
        if (UtilValidate.isEmpty(PartyGroupType)){
            return "";
        }
        return PartyGroupType.getString("typeCode");
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

    public static Boolean ddFormIsSubmitted(Delegator delegator, GenericValue supplierParty) throws GenericEntityException {
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
           return getDaysDifference(UtilDateTime.nowTimestamp(), getFinishUploadDate(supplierParty, delegator));
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

    public static Long getClassificationRatingNumber (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Long criticalValue = 0L;
        Map<String, Object> statusMap = UtilMisc.toMap("High",3L,"Low",1L,"Medium",2L);
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
        if (UtilValidate.isNotEmpty(checkAttributeEntity) && checkAttributeEntity.get("attrValue").equals("Warning")){
            return true;
        }else {
            return false;
        }

    }

    public static String getDDFormTypeId (String ddFormType) {
        return (String) DDType.get(ddFormType);
    }

    public static Timestamp getLastSubmittedDate (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        List<String> orderBy = new ArrayList<>();
        orderBy.add("-statusDatetime");
        GenericValue workEffortStatus = EntityQuery.use(delegator).from("WorkEffortStatus").
                where(UtilMisc.toMap("workEffortId", supplierParty.get("workEffortId"), "statusId", "PROCESSED")).
                orderBy(orderBy).queryFirst();
        if (UtilValidate.isNotEmpty(workEffortStatus)){
            return workEffortStatus.getTimestamp("statusDatetime");
        }else {
            return null;
        }

    }

    public static Long getUploadDocCriticalValue (GenericValue partyMedia, Delegator delegator) throws GenericEntityException {
        GenericValue partyContent = delegator.findOne("PartyContent", UtilMisc.toMap("partyContentId", partyMedia.get("partyContentId")), true);
        GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", partyContent.get("contentId")), true);
        GenericValue dataResource = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", content.get("dataResourceId")), true);
        if (UtilValidate.isNotEmpty(dataResource.get("dataResourceName"))){
            return 3L;
        }else {
            return 1L;
        }
    }

    public static String noUploadedFiles (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String noUploadedFilesName = "";
        List<GenericValue> partyContents = delegator.findByAnd("PartyMediaResource",
                UtilMisc.toMap("partyId", supplierParty.getString("partyId"),
                        "partyContentTypeId", "COMMERCIAL_LICENSE"), null, true);
        if (UtilValidate.isEmpty(partyContents)){
            return "Missing Copy of Commercial License";
        }
        GenericValue commercialLicense = EntityUtil.getFirst(partyContents);
        GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", commercialLicense.get("contentId")), true);
        GenericValue dataResource = delegator.findOne("OtherDataResource", UtilMisc.toMap("dataResourceId", content.get("dataResourceId")), true);
        if (UtilValidate.isEmpty(dataResource.get("dataResourceContent"))){
            noUploadedFilesName = "Missing Copy of Commercial License";
            return noUploadedFilesName;
        }
        return noUploadedFilesName;
    }

    public static String getYesResponse (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String warningString = "";
        if (isCheckWarning(supplierParty, delegator)){
            warningString = "Compliance certifications warning";
        }
        return warningString;
    }

    public static String noUploadUBODoc(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        List<GenericValue> uboList = supplierParty.getRelated("FromRelationshipAndToParty", UtilMisc.toMap("roleTypeIdTo", "BENEFICIAL_PERSON",
                "roleTypeIdFrom", "SUPPLIER"), null, false);
        for (GenericValue genericValue : uboList) {
            if (UtilValidate.isEmpty(genericValue.get("dataResourceContent"))) {
                return "Missing ID copy of UBO";
            }
        }
        return "";
    }

    public static String noUploadCopyIdOfDir(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        List<GenericValue> seniorStaffList = supplierParty.getRelated("FromRelationshipAndToParty", UtilMisc.toMap("roleTypeIdTo", "SENIOR_STAFF",
                "roleTypeIdFrom", "SUPPLIER"), null, false);
        for (GenericValue genericValue : seniorStaffList) {
            if (UtilValidate.isEmpty(genericValue.get("dataResourceContent"))) {
                return "Missing ID copy of Director/Officer";
            }
        }
        return "";
    }

    //控制警告内容输出格式
    public static String getWarningContent(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        String warningString = getYesResponse(supplierParty, delegator);
        String noUploadedFilesName = noUploadedFiles(supplierParty, delegator);
        String noUploadUBODoc = noUploadUBODoc(supplierParty, delegator);
        String missingIdOfDir = noUploadCopyIdOfDir(supplierParty, delegator);
        String str = "";
        List<String> stringList = new ArrayList<>();
        stringList.add(warningString);
        stringList.add(noUploadedFilesName);
        stringList.add(noUploadUBODoc);
        stringList.add(missingIdOfDir);
        for (int i = 0; i < stringList.size(); i++) {
            String s = stringList.get(i);
            if (UtilValidate.isEmpty(s)) continue;
            if (UtilValidate.isNotEmpty(str)) {
                str += "\n" + s;
            } else {
                str = s;
            }
        }
        return str;
    }

    public static String addNameForUpload (GenericValue partyMedia, Delegator delegator) throws GenericEntityException {
        GenericValue partyContent = delegator.findOne("PartyContent", UtilMisc.toMap("partyContentId", partyMedia.get("partyContentId")), true);
        GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", partyContent.get("contentId")), true);
        GenericValue dataResource = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", content.get("dataResourceId")), true);
        if (UtilValidate.isNotEmpty(dataResource.get("dataResourceName"))){
            return "Download";
        }else {
            return null;
        }

    }

    public static Long getProcessNumeric (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Map<String, Object> processBarMap = UtilMisc.toMap("COWORK_CREATED", 1L, "REQUESTED_DD", 2L,
                "SUBMITTED_DD", 3L, "PROCUREMENT_REVIEW", 4L, "COMPLIANCE_REVIEW", 5L, "COMPLETED_DD", 6L, "REGISTERED", 7L);
        GenericValue workEffort = delegator.findOne("WorkEffort", UtilMisc.toMap("workEffortId", supplierParty.get("workEffortId")), true);
        if (UtilValidate.isEmpty(workEffort.getString("workEffortParentId")) && workEffort.get("workEffortTypeId").equals("COWORK")){
            return (Long) processBarMap.get(workEffort.getString("currentStatusId"));
        }else if (UtilValidate.isNotEmpty(workEffort.getString("workEffortParentId"))){
            List<GenericValue> parentSuppliers = delegator.findByAnd("WorkEffortAndPartyGroupContact", UtilMisc.toMap("workEffortId", workEffort.getString("workEffortParentId")), null, true);
            GenericValue parentSupplier = EntityUtil.getFirst(parentSuppliers);
            return (Long) processBarMap.get(parentSupplier.getString("currentStatusId"));
        }else {
            return 0L;
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

    public static Boolean applicantSubmitIsHidden(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        if (getDDFormType(supplierParty, delegator).equals("No DD")){
            return false;
        }

        GenericValue applicantWorkEffort = EntityQuery.use(delegator).from("WorkEffort").
                where("partyId", supplierParty.getString("partyId"), "workEffortTypeId", "COWORK_TASK").orderBy("createdDate").queryFirst();

        List<GenericValue> vendorWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(vendorWorkEfforts)){
            return true;
        }
        GenericValue vendorWorkEffort = EntityUtil.getFirst(vendorWorkEfforts);
        if (applicantWorkEffort.get("currentStatusId").equals("PROCESSED") || vendorWorkEffort.get("currentStatusId").equals("NOT_PROCESSED")){
            return true;
        }
        return false;
    }

    public static Boolean procurementSubmitIsHidden(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean procurementSubmitHidden = true;
        List<GenericValue> coworks = delegator.findByAnd("WorkEffort",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "workEffortTypeId", "COWORK"), null, true);
        if (UtilValidate.isNotEmpty(coworks)){
            GenericValue cowork = EntityUtil.getFirst(coworks);
            if (cowork.get("currentStatusId").equals("PROCUREMENT_REVIEW") || cowork.get("currentStatusId").equals("COMPLETED_DD")){
                procurementSubmitHidden = false;
            }
            if (cowork.get("currentStatusId").equals("COMPLIANCE_REVIEW")){
                //如果被合规拒绝 也可以reject
                GenericValue complianceWorkEffort = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("approvePartyId", "HG").queryFirst();
                if (UtilValidate.isNotEmpty(complianceWorkEffort) && "PROCESSED".equals(complianceWorkEffort.getString("currentStatusId"))) {
                    procurementSubmitHidden = false;
                }
            }
        }
        return procurementSubmitHidden;
    }

    public static Boolean procurementRejectIsHidden(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean procurementRejectHidden = true;
        List<GenericValue> coworks = delegator.findByAnd("WorkEffort",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "workEffortTypeId", "COWORK"), null, true);
        if (UtilValidate.isNotEmpty(coworks)){
            GenericValue cowork = EntityUtil.getFirst(coworks);
            if (cowork.get("currentStatusId").equals("PROCUREMENT_REVIEW")){
                procurementRejectHidden = false;
            }
            if (cowork.get("currentStatusId").equals("COMPLIANCE_REVIEW")){
                //如果被合规拒绝 也可以reject
                GenericValue complianceWorkEffort = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("approvePartyId", "HG").queryFirst();
                if (UtilValidate.isNotEmpty(complianceWorkEffort) && "PROCESSED".equals(complianceWorkEffort.getString("currentStatusId"))) {
                    procurementRejectHidden = false;
                }
            }
        }
        return procurementRejectHidden;
    }

    public static String getApplicantId(String vendorPartyId, Delegator delegator) throws GenericEntityException {
        //获取申请人的部门
        GenericValue firstTask = EntityQuery.use(delegator).from("WorkEffort").where("partyId", vendorPartyId, "workEffortTypeId", "COWORK_TASK").orderBy("createdDate").queryFirst();
        GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", firstTask.getString("createdByUserLogin")), false);
        return createUser.getString("partyId");
    }

    public static String getApplicantCompanyId(String vendorPartyId, Delegator delegator) throws GenericEntityException {
        //获取申请人的部门
        GenericValue firstTask = EntityQuery.use(delegator).from("WorkEffort").where("partyId", vendorPartyId, "workEffortTypeId", "COWORK_TASK").orderBy("createdDate").queryFirst();
        GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", firstTask.getString("createdByUserLogin")), false);
        return CommonUtils.getPartyCompany(createUser.getString("partyId"), delegator);
    }


}
