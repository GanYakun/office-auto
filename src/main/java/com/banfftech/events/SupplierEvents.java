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
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

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
            GenericValue supplierParty = supplierPartyEntity.getGenericValue();
            String partyId = (String) supplierParty.get("partyId");
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

            String partyName = (String) actionParameters.get("partyName");
            String primaryPhone = (String) actionParameters.get("primaryPhone");
            String phoneMobile = (String) actionParameters.get("phoneMobile");
            String primaryEmail = (String) actionParameters.get("primaryEmail");
            String position = (String) actionParameters.get("position");
            if (CommonUtils.checkInputRepeat(delegator, "contactNumber", "TelecomNumber", null, phoneMobile)) {
                throw new OfbizODataException("Phone number is repeat！");
            }
            Map<String, Object> result = dispatcher.runSync("banfftech.createPersonAndContact",
                    UtilMisc.toMap("partyName", partyName, "primaryPhone", primaryPhone, "phoneMobile", phoneMobile,
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
        List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdTo", supplierParty.get("partyId"), "roleTypeIdFrom", "SUPPLIER", "roleTypeIdTo", "CONTACT"), null, false);
        GenericValue partyRelationship = EntityUtil.getFirst(partyRelationships);
        List<GenericValue> workEffortAndPartyGroupContacts = delegator.findByAnd("WorkEffortAndPartyGroupContact", UtilMisc.toMap("partyId", partyRelationship.get("partyIdFrom"), "approvePartyId", partyRelationship.get("partyIdFrom")), null, false);
        if (UtilValidate.isEmpty(workEffortAndPartyGroupContacts)){
            throw new OfbizODataException("You must assign party to approve!");
        }
        GenericValue workEffortAndPartyGroupContact = EntityUtil.getFirst(workEffortAndPartyGroupContacts);
        dispatcher.runSync("banfftech.createPartySurveyAppl",
                UtilMisc.toMap("userLogin", userLogin, "partyId", supplierParty.getString("partyId"),
                        "surveyId", "DD_STD_FORM", "surveyApplTypeId", "DD_FORM"));

        //暂时采用更新SurveyQuestionAnswer的方案
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9000",
                        "partyId", supplierParty.getString("partyId"), "booleanResponse", "Y"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9001",
                        "partyId", supplierParty.getString("partyId"), "booleanResponse", "Y"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9002",
                        "partyId", supplierParty.getString("partyId"), "booleanResponse", "N"));
        dispatcher.runSync("banfftech.updateSurveyQuestionAnswer",
                UtilMisc.toMap("userLogin", userLogin, "surveyQuestionId", "9003",
                        "partyId", supplierParty.getString("partyId"), "booleanResponse", "N"));
        dispatcher.runSync("banfftech.updateWorkEffortAndPartyGroupContact",
                UtilMisc.toMap("userLogin", userLogin, "currentStatusId", "PROCESSED", "workEffortId", workEffortAndPartyGroupContact.get("workEffortId"), "partyId", partyRelationship.get("partyIdFrom")));
        return null;
    }

    /**
     * @Author yyp
     * @Description 填写公司基本信息
     * @Date 18:37 2023/11/13
     * @Edmconfig supplierApproveServiceEdmConfig.xml
     **/
    public static Object fillSupplierBaseInfo(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws OfbizODataException, GenericServiceException, GeneralServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        DispatchContext dctx = dispatcher.getDispatchContext();
        
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue supplierParty = supplierPartyEntity.getGenericValue();

        actionParameters.put("workEffortId",supplierParty.getString("workEffortId"));
        actionParameters.put("partyId",supplierParty.getString("partyId"));
        actionParameters.put("userLogin",userLogin);

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
}
