package com.banfftech.worker;

import com.banfftech.common.util.CommonUtils;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
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
        if (UtilValidate.isEmpty(dataResource)){
            return "Missing Copy of Commercial License";
        }
        if (UtilValidate.isEmpty(dataResource.get("dataResourceContent"))){
            noUploadedFilesName = "Missing Copy of Commercial License";
            return noUploadedFilesName;
        }
        return noUploadedFilesName;
    }

    public static String getYesResponse (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        if (isCheckWarning(supplierParty, delegator)){
            return "Compliance certifications warning";
        }
        return "";
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
