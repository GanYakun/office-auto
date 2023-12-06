package com.banfftech.services;


import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OfbizODataException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VendorService {

    public static Map<String, Object> updatePartyStatusToEnable(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", context.get("workEffortId")).queryFirst();
        String partyId = workEffort.getString("partyId");
        dispatcher.runSync("banfftech.updateParty", UtilMisc.toMap("userLogin", userLogin,
                "partyId", partyId, "statusId", "PARTY_ENABLED"));
        return resultMap;
    }

    public static Map<String, Object> createWorkEffortAndPartyGroupContact(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericServiceException, GenericEntityException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        //create Party
        Map<String, Object> serviceResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroupAndContact", userLogin);
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult;
        }
//        //create ParentWorkEffort
//        Map<String, Object> createParentWorkMap = new HashMap<>();
//        String workEffortParentId = delegator.getNextSeqId("WorkEffort");
//        createParentWorkMap.put("workEffortId", workEffortParentId);
//        createParentWorkMap.put("workEffortTypeId", "COWORK");
//        createParentWorkMap.put("currentStatusId", "NOT_PROCESSED");
//        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(),
//                createParentWorkMap, "banfftech.createWorkEffort", userLogin);
        //create WorkEffort
        String partyId = (String) serviceResult.get("partyId");
        String workEffortId = delegator.getNextSeqId("WorkEffort");
        context.put("partyId", partyId);
        context.put("workEffortId", workEffortId);
        context.put("workEffortTypeId", "COWORK_TASK");
//        context.put("workEffortParentId", workEffortParentId);
        Map<String, Object> personServiceResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createWorkEffort", userLogin);
        if (ServiceUtil.isError(personServiceResult)) {
            return personServiceResult;
        }
        //分配给当前用户的组织
        String partyCompany = CommonUtils.getPartyCompany(userLogin.getString("partyId"), delegator);
        dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortId, "partyId", partyCompany));
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put("partyId", partyId);
        resultMap.put("workEffortId", workEffortId);
        return resultMap;
    }

    public static Map<String, Object> updateWorkEffortAndPartyGroupContact(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericServiceException, GenericEntityException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        //update WorkEffort
        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updateWorkEffort", userLogin);
        //update Party
        GenericValue WorkEffortAndPartyGroupContact = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("workEffortId", context.get("workEffortId")).queryFirst();
        String partyId = WorkEffortAndPartyGroupContact.getString("partyId");
        context.put("partyId", partyId);
        if ("APPROVAL_APPROVED".equals(context.get("currentStatusId"))) {
            //审批通过 将供应商状态改为启用
            context.put("statusId", "PARTY_ENABLED");
        }
        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updatePartyGroupAndContact", userLogin);
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put("partyId", partyId);
        return resultMap;
    }

    public static Map<String, Object> createSupplierRole(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String partyId = (String) context.get("partyId");
        dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin,
                "partyId", partyId, "roleTypeId", "SUPPLIER"));
        return resultMap;
    }

    public static Map<String, Object> createSupplierUserLogin(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        String partyId = (String) context.get("partyId");
        String code = CommonUtils.getEncryptedPassword(delegator, "ofbiz");
        dispatcher.runSync("banfftech.createUserLogin", UtilMisc.toMap("userLogin", userLogin, "enabled", "Y",
                "partyId", partyId, "currentPassword", code));
        return resultMap;
    }

    public static Map<String, Object> createPartyMediaResource(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericServiceException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> createResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createContentAndMediaDataResource", userLogin);
        context.putAll(createResult);
        return CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyContent", userLogin);
    }

    public static Map<String, Object> updatePartyMediaResource(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericServiceException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updatePartyContent", userLogin);
        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updateContentAndMediaDataResource", userLogin);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> copyRelationshipAttr(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        String partyRelationshipId = (String) context.get("partyRelationshipId");
        String attrName = (String) context.get("attrName");
        String attrValue = (String) context.get("attrValue");
        GenericValue partyRelationship = EntityQuery.use(delegator).from("PartyRelationship").where(UtilMisc.toMap("partyRelationshipId", partyRelationshipId)).queryOne();
        GenericValue toParty = partyRelationship.getRelatedOne("ToParty", false);
        CommonUtils.setObjectAttribute(toParty, attrName, attrValue);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> ddFormCommit(DispatchContext dctx, Map<String, Object> context)
            throws GenericEntityException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        String supplierPartyId = (String) context.get("supplierPartyId");
        GenericValue supplier = EntityQuery.use(delegator).from("Party").where("partyId", supplierPartyId).queryFirst();
        List<GenericValue> surveyQuestions = delegator.findByAnd("SurveyQuestion", UtilMisc.toMap("surveyQuestionTypeId", "BOOLEAN"), null, true);
        if (UtilValidate.isNotEmpty(surveyQuestions)){
            List<String> surveyQuestionIds = EntityUtil.getFieldListFromEntityList(surveyQuestions, "surveyQuestionId", false);
            EntityCondition condition1 = EntityCondition.makeCondition("surveyQuestionId", EntityOperator.IN, surveyQuestionIds);
            EntityCondition condition2 = EntityCondition.makeCondition("partyId", supplierPartyId);
            EntityCondition condition = EntityCondition.makeCondition(condition1, condition2);
            List<GenericValue> surveyQuestionAnswers = EntityQuery.use(delegator).from("SurveyQuestionAnswer").where(condition).queryList();
            List<String> boolAnswer = EntityUtil.getFieldListFromEntityList(surveyQuestionAnswers, "booleanResponse", false);
            CommonUtils.setObjectAttribute(supplier, "complianceCheckWarning", boolAnswer.contains("Y") ? "Warning" : "Normal");
        }
        CommonUtils.setObjectAttribute(supplier, "ddFormStatusHistory", "Submitted");
        return ServiceUtil.returnSuccess();
    }
}
