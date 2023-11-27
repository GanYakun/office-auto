package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OdataParts;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.mail.MessagingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;


public class SupplierEvents {
    public static void fillPartyClassification(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                                 EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
            String adverseResults = (String) actionParameters.get("adverseResults");
            String additionalProtection = (String) actionParameters.get("additionalProtection");
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
        } catch (GenericEntityException | GenericServiceException e) {
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
            dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "copyIdentificationAttached", "attrValue", copyIdentificationAttached ? "Y" :"N"));
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
    public static Object createNoteData(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                        EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");

        String noteName = (String) actionParameters.get("noteName");
        String noteInfo = (String) actionParameters.get("noteInfo");
        Timestamp noteDateTime = UtilDateTime.nowTimestamp();

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

    public static Object generateSurveyQuestionAnswer(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                                      EdmBindingTarget edmBindingTarget) throws OfbizODataException, GenericServiceException, GenericEntityException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "PartyUserLogin");
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        GenericValue supplierParty = null;
        if (UtilValidate.isEmpty(supplierPartyEntity)) {
            return null;
        }
        supplierParty = supplierPartyEntity.getGenericValue();

        //查询登录人所属供应商
        List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdTo", supplierParty.get("partyId"), "roleTypeIdFrom", "SUPPLIER", "roleTypeIdTo", "CONTACT"), null, false);
        GenericValue partyRelationship = EntityUtil.getFirst(partyRelationships);
        String supplierPartyId = UtilValidate.isEmpty(partyRelationship) ?
                supplierParty.getString("partyId") : partyRelationship.getString("partyIdFrom");
        List<GenericValue> workEffortAndPartyGroupContacts = delegator.findByAnd("WorkEffortAndPartyGroupContact", UtilMisc.toMap("partyId", supplierPartyId, "approvePartyId", supplierPartyId), null, false);
        if (UtilValidate.isEmpty(workEffortAndPartyGroupContacts)){
            throw new OfbizODataException("You must assign party to approve!");
        }

        GenericValue workEffortAndPartyGroupContact = EntityUtil.getFirst(workEffortAndPartyGroupContacts);
        dispatcher.runSync("banfftech.createPartySurveyAppl",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierPartyId,
                        "surveyId", "DD_STD_FORM", "surveyApplTypeId", "DD_FORM"));

        //暂时采用更新SurveyQuestionAnswer的方案
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9000",
                        "partyId", supplierPartyId, "booleanResponse", "Y"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9001",
                        "partyId", supplierPartyId, "booleanResponse", "Y"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9002",
                        "partyId", supplierPartyId, "booleanResponse", "N"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9003",
                        "partyId", supplierPartyId, "booleanResponse", "N"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9004",
                        "partyId", supplierPartyId));
        dispatcher.runSync("banfftech.updateWorkEffortAndPartyGroupContact",
                UtilMisc.toMap("userLogin", userLogin, "currentStatusId", "PROCESSED",
                        "workEffortId", workEffortAndPartyGroupContact.get("workEffortId"),
                        "partyId", supplierPartyId));
        //第一次提交后创建attribute字段作为ddForm已提交状态记录
        ddFormFirstSubmitRecord(delegator, dispatcher, userLogin, supplierPartyId);
        return null;
    }

    private static void ddFormFirstSubmitRecord(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,String supplierPartyId)
            throws GenericEntityException, GenericServiceException {

        GenericValue ddFormStatusHistory = delegator.findOne("PartyAttribute",
                UtilMisc.toMap("partyId", supplierPartyId, "attrName", "ddFormStatusHistory"), true);
        if (UtilValidate.isEmpty(ddFormStatusHistory)){
            dispatcher.runSync("banfftech.createPartyAttribute",
                    UtilMisc.toMap("userLogin", userLogin, "partyId", supplierPartyId,
                            "attrName", "ddFormStatusHistory", "attrValue", "Submitted"));
        }
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

        actionParameters.put("workEffortId",supplierParty.getString("workEffortId"));
        actionParameters.put("partyId",supplierParty.getString("partyId"));
        actionParameters.put("userLogin",userLogin);
        if(UtilValidate.isNotEmpty(actionParameters.get("usccNumber"))){
            List<GenericValue> partyIdentifications = delegator.findByAnd("PartyIdentification", UtilMisc.toMap("partyId", supplierParty.getString("partyId")), null, false);
            if (UtilValidate.isNotEmpty(partyIdentifications)){
                GenericValue partyIdentification = EntityUtil.getFirst(partyIdentifications);
                dispatcher.runSync("banfftech.updatePartyIdentification",
                        UtilMisc.toMap("partyId", partyIdentification.getString("partyId"), "userLogin", userLogin,
                                "partyIdentificationTypeId", partyIdentification.get("partyIdentificationTypeId"), "idValue", actionParameters.get("usccNumber")));
            }else{
                dispatcher.runSync("banfftech.createPartyIdentification",
                        UtilMisc.toMap("userLogin", userLogin, "partyIdentificationTypeId", "USCC_OF_CHINESE_ORG",
                                "partyId", supplierParty.get("partyId"), "idValue", actionParameters.get("usccNumber")));
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
                                           EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException {

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        String priority = "PRIORITY_MEDIUM";
        if (UtilValidate.isNotEmpty(actionParameters.get("priority"))){
            priority = (String) actionParameters.get("priority");
        }
        Map<String, Object> resultMap = dispatcher.runSync("banfftech.createWorkEffortAndPartyGroupContact",
                UtilMisc.toMap("userLogin", userLogin, "partyName", actionParameters.get("partyName"), "currentStatusId", "NOT_PROCESSED",
                        "primaryPhone", actionParameters.get("primaryPhone"), "primaryEmail", actionParameters.get("primaryEmail"), "priority", priority));
        Delegator delegator = dispatcher.getDelegator();
        if ((Boolean) actionParameters.get("isGovernment")){
            dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin, "partyId", resultMap.get("partyId"), "roleTypeId", "GOVERNMENT_SUPPLIER"));
        }
        dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", resultMap.get("partyId"), "attrName", "isGovernment", "attrValue", (Boolean) actionParameters.get("isGovernment") ? "Y" : "N"));

        dispatcher.runSync("banfftech.createProductCategoryRole",
                UtilMisc.toMap("userLogin", userLogin, "partyId", resultMap.get("partyId"), "roleTypeId", "SUPPLIER",
                        "productCategoryId", actionParameters.get("productCategoryId"), "fromDate", UtilDateTime.nowTimestamp()));
        String departmentPartyId = CommonUtils.getPartyCompany(userLogin.getString("partyId"), delegator);
        dispatcher.runSync("banfftech.createPartyRelationship",
                UtilMisc.toMap("userLogin", userLogin, "roleTypeIdFrom", "DEPARTMENT", "partyIdFrom", departmentPartyId,
                        "roleTypeIdTo", "SUPPLIER", "partyIdTo", resultMap.get("partyId")));
        String code = CommonUtils.getEncryptedPassword(delegator, "ofbiz");
        dispatcher.runSync("banfftech.createUserLogin",
                UtilMisc.toMap("userLogin", userLogin, "partyId", resultMap.get("partyId"), "userLoginId", "supplier"+resultMap.get("partyId"), "currentPassword", code));

        //create DD Form
        String partyId = (String) resultMap.get("partyId");
        String countryIncorporationId = delegator.getNextSeqId("GeoPoint");
        delegator.create("GeoPoint", UtilMisc.toMap("geoPointId", countryIncorporationId, "latitude", "_NA_", "longitude", "_NA_"));
        delegator.create("PartyGeoPoint", UtilMisc.toMap("partyGeoPointId",delegator.getNextSeqId("PartyGeoPoint"),
                "partyId",partyId, "partyGeoPointTypeId", "COUNTRY_INCORPORATION","geoPointId", countryIncorporationId));

        String businessLocationId = delegator.getNextSeqId("GeoPoint");
        delegator.create("GeoPoint", UtilMisc.toMap("geoPointId", businessLocationId, "latitude", "_NA_", "longitude", "_NA_"));
        delegator.create("PartyGeoPoint", UtilMisc.toMap("partyGeoPointId",delegator.getNextSeqId("PartyGeoPoint"),
                "partyId",partyId, "partyGeoPointTypeId", "BUSINESS_LOCATION","geoPointId", businessLocationId));
        delegator.create("PartyIdentification", UtilMisc.toMap("partyId",partyId, "partyIdentificationTypeId", "REGISTRATION_NUMBER"));

        //SurveyQuestion
        dispatcher.runSync("banfftech.createPartySurveyAppl",
                UtilMisc.toMap("userLogin", userLogin, "partyId", partyId, "surveyId", "DD_STD_FORM", "surveyApplTypeId", "DD_FORM"));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9000", "partyId", partyId, "booleanResponse", "Y"));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9001", "partyId", partyId, "booleanResponse", "Y"));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9002", "partyId", partyId, "booleanResponse", "N"));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9003", "partyId", partyId, "booleanResponse", "N"));
        dispatcher.runSync("banfftech.createSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionAnswerId", delegator.getNextSeqId("SurveyQuestionAnswer"), "surveyQuestionId", "9004", "partyId", partyId));

        //crate first ContactPerson
        Map<String, Object> result = dispatcher.runSync("banfftech.createPersonAndContact", UtilMisc.toMap("userLogin", userLogin));
        String contactPartyId = (String) result.get("partyId");
        dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "roleTypeId", "CONTACT"));
        dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "position"));
        dispatcher.runSync("banfftech.createPartyRelationship", UtilMisc.toMap("partyIdFrom", partyId, "roleTypeIdFrom", "SUPPLIER",
                "partyIdTo", contactPartyId, "roleTypeIdTo", "CONTACT", "fromDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));

    }

    public static void selectDepartment(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
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
            throw new OfbizODataException("Account is invalid");
        }
        String supplierPartyId = (String) supplierParty.get("partyId");
        List<String> partyIds = (List<String>) actionParameters.get("partyId");
        for (String partyId : partyIds){
            if(CommonUtils.checkInputRepeat(delegator, "partyIdFrom", "PartyRelationship", UtilMisc.toMap("partyIdTo", supplierPartyId, "roleTypeIdFrom", "DEPARTMENT"), partyId)){
                throw new OfbizODataException("Don't select repeat department！");
            }
            dispatcher.runSync("banfftech.createPartyRelationship",
                    UtilMisc.toMap("userLogin", userLogin, "roleTypeIdFrom", "DEPARTMENT", "partyIdFrom", partyId,
                            "roleTypeIdTo", "SUPPLIER", "partyIdTo", supplierPartyId));
        }
    }

    public static void removeDepartment(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                           EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException {

        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        OdataOfbizEntity departmentEntity = (OdataOfbizEntity) actionParameters.get("department");
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
                UtilMisc.toMap("partyIdFrom", department.get("partyId"), "roleTypeIdFrom", "DEPARTMENT",
                        "partyIdTo", supplierPartyId, "roleTypeIdTo", "SUPPLIER"), null, true);
        GenericValue partyRelationship = EntityUtil.getFirst(partyRelationships);
        dispatcher.runSync("banfftech.deletePartyRelationship",
                UtilMisc.toMap("userLogin", userLogin, "partyRelationshipId", partyRelationship.get("partyRelationshipId")));
    }

    /**
     * 完成文件上传
     */
    public static void finishDomUpload(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();
        dispatcher.runSync("banfftech.createPartyAttributeDate",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierParty.get("partyId"),
                        "attrName", "finishDomUploadDate", "attrValue", UtilDateTime.nowTimestamp()));
        dispatcher.runSync("banfftech.createPartyAttribute",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierParty.get("partyId"),
                        "attrName", "isFinishedDomUpload", "attrValue", "Y"));
    }
}
