package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OdataParts;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;


public class SupplierEvents {
    public static void fillPartyClassification(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                               EdmBindingTarget edmBindingTarget) throws OfbizODataException, UnsupportedEncodingException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
            String adverseResults = (String) actionParameters.get("adverseResults");
            String additionalProtection = (String) actionParameters.get("additionalProtection");
            String complianceComments = (String) actionParameters.get("complianceComments");
            GenericValue supplierParty = supplierPartyEntity.getGenericValue();
            String partyId = (String) supplierParty.get("partyId");
            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
            List<GenericValue> partyClassifications = delegator.findByAnd("PartyClassification", UtilMisc.toMap("partyId", partyId), null, false);
            if (UtilValidate.isNotEmpty(partyClassifications)) {
                for (GenericValue partyClassification : partyClassifications) {
                    dispatcher.runSync("banfftech.deletePartyClassification", UtilMisc.toMap("partyId", partyId, "fromDate", partyClassification.get("fromDate"),
                            "partyClassificationGroupId", partyClassification.get("partyClassificationGroupId"), "userLogin", userLogin));
                }
            }
            String partyClassificationGroupId = (String) actionParameters.get("partyClassificationGroupId");
            dispatcher.runSync("banfftech.createPartyClassification", UtilMisc.toMap("userLogin", userLogin,
                    "partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId, "fromDate", UtilDateTime.nowTimestamp()));
            if (UtilValidate.isNotEmpty(adverseResults)) {
                CommonUtils.setObjectAttribute(party, "adverseResults", adverseResults);
            }
            if (UtilValidate.isNotEmpty(additionalProtection)) {
                CommonUtils.setObjectAttribute(party, "additionalProtection", additionalProtection);
            }
            if (UtilValidate.isNotEmpty(complianceComments)) {
                CommonUtils.setObjectAttribute(party, "complianceComments", complianceComments);
            }
            //submit to procurement
            actionParameters.put("noteInfo", complianceComments);
            actionParameters.put("source", "compliance");
            actionParameters.put("compliancePassed", "Y");
            SupplierApproveEvents.toProcurement(oDataContext, actionParameters, edmBindingTarget);
        } catch (GeneralException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

    public static Object createPersonAndContact(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                                EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
            int odataPartsListSize = odataPartsList.size();
            GenericValue supplierParty = null;
            OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
            OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
            supplierParty = supplierPartyEntity.getGenericValue();
            if (supplierParty == null) {
                throw new OfbizODataException("Account is invalid");
            }
            String partyId = (String) supplierParty.get("partyId");

            String firstName = (String) actionParameters.get("firstName");
            String lastName = (String) actionParameters.get("lastName");
            String primaryPhone = (String) actionParameters.get("primaryPhone");
            String phoneMobile = (String) actionParameters.get("phoneMobile");
            String primaryEmail = (String) actionParameters.get("primaryEmail");
            String position = (String) actionParameters.get("position");
            if (CommonUtils.checkInputRepeat(delegator, "contactNumber", "TelecomNumber", null, phoneMobile)) {
                throw new OfbizODataException("Phone number is repeat！");
            }
            Map<String, Object> result = dispatcher.runSync("banfftech.createPersonAndContact",
                    UtilMisc.toMap("firstName", firstName, "lastName", lastName, "primaryPhone", primaryPhone, "phoneMobile", phoneMobile,
                            "primaryEmail", primaryEmail, "userLogin", userLogin));
            String contactPartyId = (String) result.get("partyId");
            dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "roleTypeId", "CONTACT"));
            dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "position", "attrValue", position));
            dispatcher.runSync("banfftech.createPartyRelationship", UtilMisc.toMap("partyIdFrom", partyId, "roleTypeIdFrom", "SUPPLIER",
                    "partyIdTo", contactPartyId, "roleTypeIdTo", "CONTACT", "fromDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));
            return delegator.findOne("PersonAndContact", true, "partyId", contactPartyId);
        } catch (GenericEntityException | GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }


    public static void createBeneficialPerson(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                              EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
            int odataPartsListSize = odataPartsList.size();
            GenericValue supplierParty = null;
            OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
            OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
            supplierParty = supplierPartyEntity.getGenericValue();
            if (supplierParty == null) {
                throw new OfbizODataException("Account is invalid");
            }
            String partyId = (String) supplierParty.get("partyId");

            String firstName = (String) actionParameters.get("firstName");
            String lastName = (String) actionParameters.get("lastName");
            String primaryPhone = (String) actionParameters.get("primaryPhone");
            String phoneMobile = (String) actionParameters.get("phoneMobile");
            String primaryEmail = (String) actionParameters.get("primaryEmail");
            String position = (String) actionParameters.get("position");
            String nationality = (String) actionParameters.get("nationality");
            Boolean copyIdentificationAttached = (Boolean) actionParameters.get("copyIdentificationAttached");
            BigDecimal amount = (BigDecimal) actionParameters.get("amount");
            if (CommonUtils.checkInputRepeat(delegator, "contactNumber", "TelecomNumber", null, phoneMobile)) {
                throw new OfbizODataException("Phone number is repeat！");
            }
            Map<String, Object> result = dispatcher.runSync("banfftech.createPersonAndContact",
                    UtilMisc.toMap("firstName", firstName, "lastName", lastName, "primaryPhone", primaryPhone, "phoneMobile", phoneMobile,
                            "primaryEmail", primaryEmail, "userLogin", userLogin));
            String contactPartyId = (String) result.get("partyId");
            dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "roleTypeId", "BENEFICIAL_PERSON"));
            dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "position", "attrValue", position));
            dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "nationality", "attrValue", nationality));
            dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "copyIdentificationAttached", "attrValue", copyIdentificationAttached ? "Y" : "N"));
            dispatcher.runSync("banfftech.createPartyRelationship", UtilMisc.toMap("partyIdFrom", partyId, "roleTypeIdFrom", "SUPPLIER",
                    "partyIdTo", contactPartyId, "roleTypeIdTo", "BENEFICIAL_PERSON", "fromDate", UtilDateTime.nowTimestamp(), "amount", amount, "userLogin", userLogin));
        } catch (GenericEntityException | GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

    /**
     * @Author yyp
     * @Description Action创建供应商备注信息
     * @Date 12:15 2023/11/13
     * @Edmconfig supplierApprovalServiceEdmConfig.xml
     **/
    // url example: SupplierParties('10000')/NoteData/com.dpbird.CreateNoteData
    public static Object createNoteData(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                        EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");

        String noteName = (String) actionParameters.get("noteName");
        String noteInfo = (String) actionParameters.get("noteInfo");
        Timestamp noteDateTime = UtilDateTime.nowTimestamp();

        // get OdataOfbizEntity of SupplierParties('10000')
        OdataOfbizEntity supplierPartyEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "SupplierParty");
        GenericValue supplierParty = null;
        if (UtilValidate.isEmpty(supplierPartyEntity)) {
            return null;
        }
        supplierParty = supplierPartyEntity.getGenericValue();
        Map<String, Object> NoteDatResult = dispatcher.runSync("banfftech.createNoteData",
                UtilMisc.toMap("userLogin", userLogin, "noteParty", userLogin.getString("partyId"),
                        "noteName", noteName, "noteInfo", noteInfo, "noteDateTime", noteDateTime));

        dispatcher.runSync("banfftech.createPartyNote",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierParty.getString("partyId"),
                        "noteId", NoteDatResult.get("noteId")));


        return null;
    }

    public static Object createSupplierProduct(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                               EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");

        String productId = (String) actionParameters.get("productId");
        Timestamp availableFromDate = UtilDateTime.nowTimestamp();
        OdataOfbizEntity supplierPartyEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "SupplierParty");
        GenericValue supplierParty = null;
        if (UtilValidate.isEmpty(supplierPartyEntity)) {
            return null;
        }
        supplierParty = supplierPartyEntity.getGenericValue();

        dispatcher.runSync("banfftech.createSupplierProduct",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierParty.getString("partyId"),
                        "productId", productId, "minimumOrderQuantity", 10, "currencyUomId", "CNY",
                        "availableFromDate", availableFromDate));
        return null;
    }

    public static Object createFinAccount(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                          EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");

        String finAccountName = (String) actionParameters.get("finAccountName");
        String finAccountCode = (String) actionParameters.get("finAccountCode");
        String currencyUomId = (String) actionParameters.get("currencyUomId");

        OdataOfbizEntity supplierPartyEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "SupplierParty");
        GenericValue supplierParty = null;
        if (UtilValidate.isEmpty(supplierPartyEntity)) {
            return null;
        }
        supplierParty = supplierPartyEntity.getGenericValue();
        dispatcher.runSync("banfftech.createFinAccount",
                UtilMisc.toMap("userLogin", userLogin, "finAccountName", finAccountName, "finAccountCode",
                        finAccountCode, "ownerPartyId", supplierParty.getString("partyId"), "finAccountTypeId",
                        "BANK_ACCOUNT", "statusId", "FNACT_ACTIVE", "currencyUomId", currencyUomId));
        return null;
    }

    /**
     * @Author yyp
     * @Description 填写公司基本信息
     * @Date 18:37 2023/11/13
     * @Edmconfig supplierApproveServiceEdmConfig.xml
     **/
    public static Object fillSupplierBaseInfo(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws OfbizODataException, GenericServiceException, GeneralServiceException, GenericEntityException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        DispatchContext dctx = dispatcher.getDispatchContext();
        Delegator delegator = (Delegator) oDataContext.get("delegator");

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();

        actionParameters.put("workEffortId", supplierParty.getString("workEffortId"));
        actionParameters.put("partyId", supplierParty.getString("partyId"));
        actionParameters.put("userLogin", userLogin);
        //创建或者更新usccNumber
        if (UtilValidate.isNotEmpty(actionParameters.get("usccNumber"))) {
            List<GenericValue> partyIdentifications = delegator.findByAnd("PartyIdentification",
                    UtilMisc.toMap("partyId", supplierParty.getString("partyId"), "partyIdentificationTypeId", "USCC_OF_CHINESE_ORG"), null, false);
            if (UtilValidate.isNotEmpty(partyIdentifications)) {
                GenericValue partyIdentification = EntityUtil.getFirst(partyIdentifications);
                dispatcher.runSync("banfftech.updatePartyIdentification",
                        UtilMisc.toMap("partyId", partyIdentification.getString("partyId"), "userLogin", userLogin,
                                "partyIdentificationTypeId", partyIdentification.get("partyIdentificationTypeId"), "idValue", actionParameters.get("usccNumber")));
            } else {
                dispatcher.runSync("banfftech.createPartyIdentification",
                        UtilMisc.toMap("userLogin", userLogin, "partyIdentificationTypeId", "USCC_OF_CHINESE_ORG",
                                "partyId", supplierParty.get("partyId"), "idValue", actionParameters.get("usccNumber")));
            }
        }
        //创建或者更新PartyGeo
        if (UtilValidate.isNotEmpty(actionParameters.get("geoId"))) {
            List<String> geoIds = (List<String>) actionParameters.get("geoId");
            List<GenericValue> partyGeos = delegator.findByAnd("PartyGeo", UtilMisc.toMap("partyId", supplierParty.get("partyId"), "partyGeoTypeId", "LISTED_COUNTRY"), null, true);
            if (UtilValidate.isNotEmpty(partyGeos)) {
                for (GenericValue partyGeo : partyGeos) {
                    dispatcher.runSync("banfftech.deletePartyGeo",
                            UtilMisc.toMap("userLogin", userLogin, "partyGeoId", partyGeo.get("partyGeoId")));
                }
            }
            for (String geoId : geoIds) {
                dispatcher.runSync("banfftech.createPartyGeo",
                        UtilMisc.toMap("userLogin", userLogin, "geoId", geoId,
                                "partyId", supplierParty.get("partyId"), "partyGeoTypeId", "LISTED_COUNTRY"));
            }
        }
        CommonUtils.setServiceFieldsAndRun(dctx, actionParameters, "banfftech.updateWorkEffortAndPartyGroupContact", userLogin);
        return null;
    }

    /**
     * @Author yyp
     * @Description 修改联系方式
     * @Date 7:52 2023/11/14
     * @Edmconfig supplierApproveServiceEdmConfig.xml
     **/
    public static Object updateSupplierContactInfo(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                                   EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "SupplierParty");
        GenericValue supplierParty;
        if (UtilValidate.isEmpty(supplierPartyEntity)) {
            return null;
        }
        supplierParty = supplierPartyEntity.getGenericValue();
        String primaryPhone = (String) actionParameters.get("primaryPhone");
        String primaryEmail = (String) actionParameters.get("primaryEmail");
        String address1 = (String) actionParameters.get("address1");

        dispatcher.runSync("banfftech.updateWorkEffortAndPartyGroupContact",
                UtilMisc.toMap("userLogin", userLogin, "workEffortId", supplierParty.getString("workEffortId"),
                        "partyId", supplierParty.getString("partyId"), "primaryPhone", primaryPhone,
                        "primaryEmail", primaryEmail, "address1", address1));
        return null;
    }

    /**
     * @Author yyp
     * @Description 更新时间信息
     * @Date 7:55 2023/11/14
     * @Edmconfig supplierApproveServiceEdmConfig.xml
     **/
    public static void updateSupplierTimeInfo(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                              EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = delegator.findOne("Party", UtilMisc.toMap("partyId", supplierPartyEntity.getPropertyValue("partyId")), false);
        CommonUtils.setObjectAttribute(supplierParty, "PartyAttributeDate", "collectionDate", actionParameters.get("collectionDate"));
        CommonUtils.setObjectAttribute(supplierParty, "PartyAttributeDate", "firstOrderDate", actionParameters.get("firstOrderDate"));
    }

    /**
     * 创建地址列表
     *
     * @param oDataContext
     * @param actionParameters
     * @param edmBindingTarget
     * @throws GenericEntityException
     * @throws OfbizODataException
     * @throws GenericServiceException
     */
    public static void createPostalAddress(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                           EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException {

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
        int odataPartsListSize = odataPartsList.size();
        GenericValue supplierParty = null;
        OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
        supplierParty = supplierPartyEntity.getGenericValue();
        if (supplierParty == null) {
            throw new OfbizODataException("Account is invalid");
        }
        String partyId = (String) supplierParty.get("partyId");
        Map<String, Object> resultMap = dispatcher.runSync("banfftech.createContactMech",
                UtilMisc.toMap("userLogin", userLogin));
        String contactMechId = (String) resultMap.get("contactMechId");
        dispatcher.runSync("banfftech.createPostalAddress",
                UtilMisc.toMap("contactMechId", contactMechId, "userLogin", userLogin, "address1", actionParameters.get("address1"),
                        "countryGeoId", actionParameters.get("countryGeoId"), "cityGeoId", actionParameters.get("cityGeoId"), "address2", actionParameters.get("address2"),
                        "stateProvinceGeoId", actionParameters.get("stateProvinceGeoId"), "contactNumber", actionParameters.get("contactNumber"), "email", actionParameters.get("email")));
        dispatcher.runSync("banfftech.createPartyContactMechPurpose",
                UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "contactMechId", contactMechId,
                        "contactMechPurposeTypeId", actionParameters.get("contactMechPurposeTypeId"), "fromDate", UtilDateTime.nowTimestamp()));
    }

    public static void createSupplierParty(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                           EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException, GeneralServiceException {

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        String priority = "PRIORITY_MEDIUM";
        if (UtilValidate.isNotEmpty(actionParameters.get("priority"))) {
            priority = (String) actionParameters.get("priority");
        }
        //create ParentWork
        Map<String, Object> createParentWorkMap = new HashMap<>();
        String workEffortParentId = delegator.getNextSeqId("WorkEffort");
        String partyId = delegator.getNextSeqId("Party");
        createParentWorkMap.put("workEffortId", workEffortParentId);
        createParentWorkMap.put("workEffortTypeId", "COWORK");
        createParentWorkMap.put("priority", priority);
        createParentWorkMap.put("currentStatusId", "COWORK_CREATED");
        createParentWorkMap.put("partyId", partyId);
        createParentWorkMap.put("userLogin", userLogin);
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createParentWorkMap, "banfftech.createWorkEffort", userLogin);
        dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortParentId, "partyId", "CG", "priority", priority));
        //create cowork task
        dispatcher.runSync("banfftech.createWorkEffortAndPartyGroupContact",
                UtilMisc.toMap("userLogin", userLogin, "partyName", actionParameters.get("partyName"),
                        "currentStatusId", "NOT_PROCESSED", "groupTypeId", actionParameters.get("groupTypeId"),
                        "primaryPhone", actionParameters.get("primaryPhone"), "primaryEmail", actionParameters.get("primaryEmail"),
                        "priority", priority, "partyId", partyId, "workEffortParentId", workEffortParentId));

        if (actionParameters.get("groupTypeId").equals("GOVERNMENTAL_AGENCIES_TYPE")) {
            dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "roleTypeId", "GOVERNMENT_SUPPLIER"));
        }
        dispatcher.runSync("banfftech.createProductCategoryRole",
                UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "roleTypeId", "SUPPLIER",
                        "productCategoryId", actionParameters.get("productCategoryId"), "fromDate", UtilDateTime.nowTimestamp()));
        String departmentPartyId = CommonUtils.getPartyCompany(userLogin.getString("partyId"), delegator);
        List<GenericValue> companyRelations = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("roleTypeIdFrom", "COMPANY", "partyIdTo", departmentPartyId), null, true);
        GenericValue companyRelation = EntityUtil.getFirst(companyRelations);
        dispatcher.runSync("banfftech.createPartyRelationship",
                UtilMisc.toMap("userLogin", userLogin, "roleTypeIdFrom", "COMPANY", "partyIdFrom", companyRelation.get("partyIdFrom"),
                        "roleTypeIdTo", "SUPPLIER", "partyIdTo", partyId));
        String code = CommonUtils.getEncryptedPassword(delegator, "ofbiz");
        dispatcher.runSync("banfftech.createUserLogin",
                UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "userLoginId", "supplier" + partyId, "currentPassword", code));

        //create DD Form
//        delegator.create("PartyGeo", UtilMisc.toMap("partyGeoId", delegator.getNextSeqId("PartyGeo"),
//                "partyId", partyId, "partyGeoTypeId", "REGISTERED_COUNTRY"));

        dispatcher.runSync("banfftech.createPartyContactMechPurposeAndAddress",
                UtilMisc.toMap("userLogin", userLogin, "contactMechId", delegator.getNextSeqId("ContactMech"),
                        "partyId", partyId, "contactMechPurposeTypeId", "REGISTERED_LOCATION"));

        String businessLocationId = delegator.getNextSeqId("GeoPoint");
        delegator.create("GeoPoint", UtilMisc.toMap("geoPointId", businessLocationId, "latitude", "_NA_", "longitude", "_NA_"));
        delegator.create("PartyGeoPoint", UtilMisc.toMap("partyGeoPointId", delegator.getNextSeqId("PartyGeoPoint"),
                "partyId", partyId, "partyGeoPointTypeId", "BUSINESS_LOCATION", "geoPointId", businessLocationId));
        delegator.create("PartyIdentification", UtilMisc.toMap("partyId", partyId, "partyIdentificationTypeId", "REGISTRATION_NUMBER"));
        delegator.create("PartyIdentification", UtilMisc.toMap("partyId", partyId, "partyIdentificationTypeId", "USCC_OF_CHINESE_ORG"));

        //SurveyQuestion
        dispatcher.runSync("banfftech.createPartySurveyAppl",
                UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "surveyId", "DD_STD_FORM", "surveyApplTypeId", "DD_FORM"));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9000", "partyId", partyId));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9001", "partyId", partyId));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9002", "partyId", partyId));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9003", "partyId", partyId));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9004", "partyId", partyId));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9005", "partyId", partyId));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9006", "partyId", partyId));

        //create PartyMediaResource
        Map<String, Object> serviceParam = new HashMap<>();
        serviceParam.put("partyId", partyId);
        serviceParam.put("userLogin", userLogin);
        serviceParam.put("contentName", "Latest Three-years Finance Audit Report");
        serviceParam.put("description", "Download");
        serviceParam.put("fromDate", UtilDateTime.nowTimestamp());
        serviceParam.put("partyContentTypeId", "SUPPLIER_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Non-disclosure Agreement (NDA)");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "SUPPLIER_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Copy of Commercial License");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Copy of VAT Certificate");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "PoA (Power of Atterney)");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "SUPPLIER_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Statement of Qualifications (SOQ)");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "SUPPLIER_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Technical Eveluation Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "SUPPLIER_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Other Documents");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "SUPPLIER_VIEW");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        //compliance report内容
        serviceParam.put("contentName", "Full Name of Company or Individual Party Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Share Holder Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Director Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Key Officer Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Previous Name Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Business Code of Conduct");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);

        serviceParam.put("contentName", "Ultimate Beneficial Owners Report");
        serviceParam.put("description", "Download");
        serviceParam.put("partyContentTypeId", "COMPLIANCE_REPORT");
        dispatcher.runSync("banfftech.createPartyMediaResource", serviceParam);
    }

    /**
     * 选择供应商所属公司
     *
     * @param oDataContext
     * @param actionParameters
     * @param edmBindingTarget
     * @throws GenericEntityException
     * @throws OfbizODataException
     * @throws GenericServiceException
     */
    public static void selectCompany(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                        EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException {

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
        int odataPartsListSize = odataPartsList.size();
        GenericValue supplierParty = null;
        OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
        supplierParty = supplierPartyEntity.getGenericValue();
        if (supplierParty == null) {
            throw new OfbizODataException("Supplier is invalid");
        }
        String supplierPartyId = (String) supplierParty.get("partyId");
        List<String> partyIds = (List<String>) actionParameters.get("partyId");
        for (String partyId : partyIds) {
            if (CommonUtils.checkInputRepeat(delegator, "partyIdFrom", "PartyRelationship", UtilMisc.toMap("partyIdTo", supplierPartyId, "roleTypeIdFrom", "COMPANY"), partyId)) {
                throw new OfbizODataException("Don't select repeat company！");
            }
            dispatcher.runSync("banfftech.createPartyRelationship",
                    UtilMisc.toMap("userLogin", userLogin, "roleTypeIdFrom", "COMPANY", "partyIdFrom", partyId,
                            "roleTypeIdTo", "SUPPLIER", "partyIdTo", supplierPartyId));
        }
    }

    /**
     * 移除权限部门
     *
     * @param oDataContext
     * @param actionParameters
     * @param edmBindingTarget
     * @throws GenericEntityException
     * @throws OfbizODataException
     * @throws GenericServiceException
     */
    public static void removeCompany(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                        EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException {

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        OdataOfbizEntity departmentEntity = (OdataOfbizEntity) actionParameters.get("company");
        GenericValue department = departmentEntity.getGenericValue();
        List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
        int odataPartsListSize = odataPartsList.size();
        GenericValue supplierParty = null;
        OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
        supplierParty = supplierPartyEntity.getGenericValue();
        if (supplierParty == null) {
            throw new OfbizODataException("Account is invalid");
        }
        String supplierPartyId = (String) supplierParty.get("partyId");
        List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship",
                UtilMisc.toMap("partyIdFrom", department.get("partyId"), "roleTypeIdFrom", "COMPANY",
                        "partyIdTo", supplierPartyId, "roleTypeIdTo", "SUPPLIER"), null, true);
        GenericValue partyRelationship = EntityUtil.getFirst(partyRelationships);
        dispatcher.runSync("banfftech.deletePartyRelationship",
                UtilMisc.toMap("userLogin", userLogin, "partyRelationshipId", partyRelationship.get("partyRelationshipId")));
    }

    /**
     * 完成文件上传
     */
    public static void finishDomUpload(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException, GenericEntityException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();
        GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", supplierParty.getString("partyId")).queryOne();
        CommonUtils.setObjectAttribute(party, "PartyAttributeDate", "finishDomUploadDate", UtilDateTime.nowTimestamp());
        dispatcher.runSync("banfftech.updateWorkEffort",
                UtilMisc.toMap("userLogin", userLogin, "workEffortId", supplierParty.getString("workEffortId"), "currentStatusId", "DOC_READY"));
    }

    /**
     * 禁用供应商
     */
    public static void disableSupplier(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();
        changeSupplierStatus(userLogin, dispatcher, supplierParty, "PARTY_DISABLED");
    }

    /**
     * 启用供应商
     */
    public static void enableSupplier(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();
        changeSupplierStatus(userLogin, dispatcher, supplierParty, "PARTY_ENABLED");
    }

    /**
     * on-hold供应商
     */
    public static void onHoldSupplier(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();
        changeSupplierStatus(userLogin, dispatcher, supplierParty, "PARTY_ON_HOLD");
    }

    private static void changeSupplierStatus(GenericValue userLogin, LocalDispatcher dispatcher, GenericValue supplierParty, String statusId) throws GenericServiceException {
        dispatcher.runSync("banfftech.updatePartyGroupAndContact",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierParty.get("partyId"), "statusId", statusId));

    }

    // Implement CreatePurchaseAgreement action
    // url example: SupplierParties('10000')/PurchaseAgreement/com.dpbird.CreateAgreement
    public static Object createPurchaseAgreement(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                         EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException, GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        String description = (String) actionParameters.get("description");
        String productId = (String) actionParameters.get("productId");
        Timestamp agreementDate = (Timestamp) actionParameters.get("agreementDate");
        Timestamp fromDate = (Timestamp) actionParameters.get("fromDate");
        Timestamp thruDate = (Timestamp) actionParameters.get("thruDate");
        // get the first bound OdataOfbizEntity. example: SupplierParties('10000')
        OdataOfbizEntity supplierPartyEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "SupplierParty");
        GenericValue supplierParty = null;
        if (UtilValidate.isEmpty(supplierPartyEntity)) {
            return null;
        }
        supplierParty = supplierPartyEntity.getGenericValue();
        String partyIdTo = supplierParty.getString("partyId");
        String partyIdFrom = CommonUtils.getPartyCompany(userLogin.getString("partyId"), delegator);

        Map<String, Object> createAgreementResult;
        try {
            createAgreementResult = dispatcher.runSync("banfftech.createAgreement",
                    UtilMisc.toMap("userLogin", userLogin, "description", description,
                            "productId", productId, "agreementDate", agreementDate, "partyIdFrom", partyIdFrom,
                            "partyIdTo", partyIdTo, "agreementTypeId", "PURCHASE_AGREEMENT", "fromDate", fromDate, "thruDate", thruDate));
        } catch (GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
        String agreementId = (String) createAgreementResult.get("agreementId");
        GenericValue agreement;
        try {
            agreement = delegator.findOne("Agreement", UtilMisc.toMap("agreementId", agreementId), false);
        } catch (GenericEntityException e) {
            throw new OfbizODataException(e.getMessage());
        }
        return agreement;
    }

    public static void demoAction(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException, GenericEntityException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");

    }

    public static void fillCommentsForPRM(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException, GenericEntityException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();
        dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", supplierParty.get("workEffortId"), "approveComments", actionParameters.get("comments"), "userLogin", userLogin));

    }


}
