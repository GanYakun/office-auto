package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import com.dpbird.odata.handler.annotation.DraftAction;
import com.dpbird.odata.handler.annotation.DraftEventContext;
import com.dpbird.odata.handler.annotation.EdmEntity;
import com.dpbird.odata.handler.annotation.EdmService;
import org.apache.ofbiz.base.test.BaseUnitTests;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DDFormEvents {
    /**
     * submit DDForm
     */
    public static void submitDDForm(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        //校验必填项
        checkRequiredField(boundEntity, delegator);
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", partyId, "approvePartyId", partyId, "workEffortTypeId", "COWORK_TASK").queryFirst();
        String workEffortId = coWork.getString("workEffortId");
        String workEffortParentId = coWork.getString("workEffortParentId");
        //当前的改为已处理
        Map<String, Object> updateWorkMap = UtilMisc.toMap("workEffortId", workEffortId, "currentStatusId", "PROCESSED", "workEffortParentId", workEffortParentId);
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), updateWorkMap, "banfftech.updateWorkEffort", userLogin);
        //parent 改为SUBMITTED_DD
        GenericValue parentWorkEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortParentId).queryOne();
        if (parentWorkEffort.getString("currentStatusId").equals("REQUESTED_DD")) {
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "SUBMITTED_DD", "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
        }
        // create NoteData for the Party
        String noteInfo = (String) actionParameters.get("noteInfo");
        if (UtilValidate.isEmpty(noteInfo)) {
            noteInfo = "Submit BPDD note";
        }
        Map<String, Object> noteMap = UtilMisc.toMap("noteName", "Submit to applicant", "noteInfo", noteInfo,
                "noteParty", userLogin.get("partyId"), "noteDateTime", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
        Map<String, Object> noteDataResult = CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), noteMap, "banfftech.createNoteData", userLogin);
        // create PartyNote
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("noteId", noteDataResult.get("noteId"), "partyId", partyId, "userLogin", userLogin), "banfftech.createPartyNote", userLogin);
        //检索是否勾选敏感问题,添加首次提交标志
        dispatcher.runSync("banfftech.ddFormCommitCheck",UtilMisc.toMap("supplierPartyId", partyId, "userLogin", userLogin));
        //发送邮件
        VendorOnBoardingEmailEvents.ddSubmit(dispatcher, httpServletRequest, boundEntity);
    }

    public static void checkRequiredField(OdataOfbizEntity vendorEntity, Delegator delegator) throws OfbizODataException, GenericEntityException {
        String partyId = (String) vendorEntity.getPropertyValue("partyId");
        checkThrowErr(vendorEntity.getPropertyValue("partyName"));
        checkThrowErr(vendorEntity.getPropertyValue("dateIncorporation"));
        checkThrowErr(vendorEntity.getPropertyValue("businessLocation"));
        checkThrowErr(vendorEntity.getPropertyValue("registrationNumber"));
        checkThrowErr(vendorEntity.getPropertyValue("workScope"));
        //check address
        GenericValue partyContactMechPurpose = EntityQuery.use(delegator).from("PartyContactMechPurpose").where("partyId", partyId, "contactMechPurposeTypeId", "REGISTERED_LOCATION").queryFirst();
        GenericValue postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", partyContactMechPurpose.getString("contactMechId")).queryFirst();
        checkThrowErr(postalAddress.getString("countryGeoId"));
        checkThrowErr(postalAddress.getString("address1"));
        //check Business Relationship
        long businessRelationshipCount = EntityQuery.use(delegator).from("PartyRoleType")
                .where("partyId", partyId, "parentTypeId", "BUSINESS_RELATIONSHIP").queryCount();
        if (businessRelationshipCount == 0) {
            throw new OfbizODataException("Please fill in business relationship to company.");
        }

        //check Contact
        long contactCount = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", partyId, "roleTypeIdFrom", "SUPPLIER", "roleTypeIdTo", "CONTACT").queryCount();
        if (contactCount == 0) {
            throw new OfbizODataException("Please fill in contact information.");
        }
        //check UBO
        long uboCount = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", partyId, "roleTypeIdFrom", "SUPPLIER", "roleTypeIdTo", "BENEFICIAL_PERSON").queryCount();
        if (uboCount == 0) {
            throw new OfbizODataException("Please fill in ultimate beneficial owner information.");
        }
        //check director/officer
        long directorCount = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", partyId, "roleTypeIdFrom", "SUPPLIER", "roleTypeIdTo", "SENIOR_STAFF").queryCount();
        if (directorCount == 0) {
            throw new OfbizODataException("Please fill in director/officer information.");
        }
        //check COMPLIANCE CERTIFICATIONS
        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition("surveyQuestionId", EntityOperator.IN, UtilMisc.toList("9000","9001","9002","9003","9005","9006")),
                EntityCondition.makeCondition("partyId", partyId));
        List<GenericValue> surveyQuestionAnswer = EntityQuery.use(delegator).from("SurveyQuestionAnswer").where(condition).queryList();
        for (GenericValue answer : surveyQuestionAnswer) {
            String response = answer.getString("enumResponse");
            if (UtilValidate.isEmpty(response) || ("TRUE".equals(response) && UtilValidate.isEmpty(answer.getString("textResponse")))) {
                throw new OfbizODataException("Please fill in compliance certifications.");
            }
        }
    }

    private static void checkThrowErr(Object value) throws OfbizODataException {
        String errMsg = "Please supplement the required fields in the form before submitting.";
        if (UtilValidate.isEmpty(value)) {
            throw new OfbizODataException(errMsg);
        }
    }

}
