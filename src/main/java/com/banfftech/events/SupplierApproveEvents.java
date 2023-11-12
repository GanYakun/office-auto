package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import java.util.HashMap;
import java.util.Map;

public class SupplierApproveEvents {

    /**
     * 传递给采购
     */
    public static void toProcurement(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        String workEffortId = (String) boundEntity.getPropertyValue("workEffortId");
        String workEffortParentId = (String) boundEntity.getPropertyValue("workEffortParentId");
        if (UtilValidate.isEmpty(workEffortParentId)) {
            //首次提交 创建ParentWorkEffort分配给采购
            Map<String, Object> createParentWorkMap = new HashMap<>();
            workEffortParentId = delegator.getNextSeqId("WorkEffort");
            createParentWorkMap.put("workEffortId", workEffortParentId);
            createParentWorkMap.put("workEffortTypeId", "COWORK");
            createParentWorkMap.put("currentStatusId", "APPROVAL_CREATED");
            createParentWorkMap.put("partyId", partyId);
            createParentWorkMap.put("userLogin", userLogin);
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createParentWorkMap, "banfftech.createWorkEffort", userLogin);
            dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                    UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortParentId, "partyId", "CG"));
        }
        //当前的改为已处理
        Map<String, Object> updateWorkMap = UtilMisc.toMap("workEffortId", workEffortId, "currentStatusId", "PROCESSED", "workEffortParentId", workEffortParentId);
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), updateWorkMap, "banfftech.updateWorkEffort", userLogin);
        // create NoteData for the Party
        String noteInfo = (String) actionParameters.get("noteInfo");
        if (UtilValidate.isEmpty(noteInfo)) {
            noteInfo = "Submit to procurement note";
        }
        Map<String, Object> noteMap = UtilMisc.toMap("noteName", "Submit to procurement", "noteInfo", noteInfo,
                "noteParty", userLogin.get("partyId"), "noteDateTime", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
        Map<String, Object> noteDataResult = CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), noteMap, "banfftech.createNoteData", userLogin);
        // create PartyNote
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("noteId", noteDataResult.get("noteId"), "partyId", partyId, "userLogin", userLogin), "banfftech.createPartyNote", userLogin);
    }

    /**
     * 采购传递给其他部门
     */
    public static void procurementReturn(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        String target = (String) actionParameters.get("target");
        String comments = (String) actionParameters.get("comments"); // 也许不需要comments了
        String noteInfo = (String) actionParameters.get("noteInfo");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        String transferTarget = getTransferTarget(delegator, target, boundEntity.getGenericValue());
        if (UtilValidate.isEmpty(transferTarget)) {
            throw new OfbizODataException("No department found");
        }
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        String workEffortId = (String) boundEntity.getPropertyValue("workEffortId");
        GenericValue supplierTask = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact")
                .where("approvePartyId", transferTarget, "workEffortParentId", workEffortId).queryFirst();
        if (UtilValidate.isEmpty(supplierTask)) {
            //首次传递
            String nextWorkEffortId = delegator.getNextSeqId("WorkEffort");
            Map<String, Object> createWorkMap = UtilMisc.toMap("partyId", partyId, "workEffortId", nextWorkEffortId, "comments", comments,
                    "workEffortTypeId", "COWORK_TASK", "currentStatusId", "NOT_PROCESSED", "workEffortParentId", workEffortId);
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createWorkMap, "banfftech.createWorkEffort", userLogin);
            dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                    UtilMisc.toMap("userLogin", userLogin, "workEffortId", nextWorkEffortId, "partyId", transferTarget));
        } else {
            //改为未处理
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", supplierTask.getString("workEffortId"),
                    "currentStatusId", "NOT_PROCESSED", "comments", comments, "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
        }
        // create NoteData for the Party
        if (UtilValidate.isEmpty(noteInfo)) {
            noteInfo = "Procurement to " + target + " note";
        }
        Map<String, Object> noteMap = UtilMisc.toMap("noteName", "Procument to " + target, "noteInfo", noteInfo,
                "noteParty", userLogin.get("partyId"), "noteDateTime", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
        Map<String, Object> noteDataResult = CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), noteMap, "banfftech.createNoteData", userLogin);
        // create PartyNote
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("noteId", noteDataResult.get("noteId"), "partyId", partyId, "userLogin", userLogin), "banfftech.createPartyNote", userLogin);
    }

    private static String getTransferTarget(Delegator delegator, String target, GenericValue parentWorkEffort) throws GenericEntityException {
        if ("procurement".equals(target)) {
            return "CG";
        }
        if ("compliance".equals(target)) {
            return "HG";
        }
        if ("applicant".equals(target)) {
            //获取申请人的部门
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", parentWorkEffort.getString("createdByUserLogin")), false);
            return CommonUtils.getPartyCompany(createUser.getString("partyId"), delegator);
        }
        if ("supplier".equals(target)) {
            return parentWorkEffort.getString("partyId");
        }
        return null;
    }

}
