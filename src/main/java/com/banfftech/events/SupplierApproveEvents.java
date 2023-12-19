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
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class SupplierApproveEvents {
    private static final String MODULE = SupplierApproveEvents.class.getName();

    /**
     * 传递给采购
     */
    public static void toProcurement(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException, UnsupportedEncodingException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        //source: applicant, compliance
        String source = (String) actionParameters.get("source");
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        String workEffortId = (String) boundEntity.getPropertyValue("workEffortId");
        String workEffortParentId = (String) boundEntity.getPropertyValue("workEffortParentId");
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
        if ("compliance".equals(source)) {
            //保存合规建议,状态改为COMPLETE_COMP
            GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
            CommonUtils.setObjectAttribute(party, "complianceNote", noteInfo);
            if (actionParameters.containsKey("compliancePassed")) {
                dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
                        "currentStatusId", "COMPLETED_DD", "userLogin", userLogin));
                VendorOnBoardingEmailEvents.complianceComplete(dispatcher, httpServletRequest, boundEntity);
            } else {
                //compliance打回
                VendorOnBoardingEmailEvents.returnToProcurement(dispatcher, httpServletRequest, boundEntity, noteInfo);
            }
        }
        if ("applicant".equals(source)) {
            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "PROCUREMENT_REVIEW", "userLogin", userLogin));
            VendorOnBoardingEmailEvents.toProcurement(dispatcher, httpServletRequest, boundEntity);
        }
    }

    /**
     * 采购传递给compliance或applicant
     */
    public static void procurementReturn(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException, UnsupportedEncodingException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        String target = (String) actionParameters.get("target"); // compliance, applicant
        String noteInfo = (String) actionParameters.get("noteInfo");
        String ddFormType = (String) actionParameters.get("ddFormType");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        GenericValue boundGenericValue = boundEntity.getGenericValue();
        String partyId = boundGenericValue.getString("partyId");
        String currentStatusId = boundGenericValue.getString("currentStatusId");
        String workEffortParentId = boundGenericValue.getString("workEffortId");
        if (UtilValidate.isNotEmpty(ddFormType)) {
            GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
            CommonUtils.setObjectAttribute(party, "ddFormType", ddFormType);
        }
        //to compliance
        if ("compliance".equals(target)) {
            GenericValue complianceWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("approvePartyId", "HG", "workEffortParentId", workEffortParentId).queryFirst();
            if (UtilValidate.isEmpty(complianceWork)) {
                //首次传递
                String nextWorkEffortId = delegator.getNextSeqId("WorkEffort");
                Map<String, Object> createWorkMap = UtilMisc.toMap("partyId", partyId, "workEffortId", nextWorkEffortId,
                        "workEffortTypeId", "COWORK_TASK", "currentStatusId", "NOT_PROCESSED", "workEffortParentId", workEffortParentId, "priority", boundGenericValue.getString("priority"));
                CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createWorkMap, "banfftech.createWorkEffort", userLogin);
                dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                        UtilMisc.toMap("userLogin", userLogin, "workEffortId", nextWorkEffortId, "partyId", "HG"));
            } else {
                CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", complianceWork.getString("workEffortId"),
                        "currentStatusId", "NOT_PROCESSED", "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
            }
            //发送给合规,状态改为COMPLIANCE_REVIEW
            if (currentStatusId.equals("PROCUREMENT_REVIEW")) {
                dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
                        "currentStatusId", "COMPLIANCE_REVIEW", "userLogin", userLogin));
            }
            VendorOnBoardingEmailEvents.toCompliance(dispatcher, httpServletRequest, boundEntity);
        }

        //to applicant
        if ("applicant".equals(target)) {
            //改为未处理
            GenericValue firstTask = EntityQuery.use(delegator).from("WorkEffort").where("workEffortParentId", workEffortParentId).orderBy("createdDate").queryFirst();
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", firstTask.getString("workEffortId"),
                    "currentStatusId", "NOT_PROCESSED","userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
            VendorOnBoardingEmailEvents.returnToApplicant(dispatcher, httpServletRequest, boundEntity, noteInfo);
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


    /**
     * 发送DD
     */
    public static void sendDDForm(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        String noteInfo = (String) actionParameters.get("noteInfo");
        String ddFormType = (String) actionParameters.get("ddFormType");
        String email = (String) actionParameters.get("email");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        GenericValue boundGenericValue = boundEntity.getGenericValue();
        String partyId = boundGenericValue.getString("partyId");
        String workEffortParentId = boundGenericValue.getString("workEffortParentId");
        GenericValue parentWorkEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortParentId).queryOne();

        GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
        if (UtilValidate.isNotEmpty(ddFormType)) {
            CommonUtils.setObjectAttribute(party, "ddFormType", ddFormType);
        }
        GenericValue ddFormTask = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("approvePartyId", partyId, "partyId", partyId).queryFirst();
        if (UtilValidate.isEmpty(ddFormTask)) {
            //首次传递
            String nextWorkEffortId = delegator.getNextSeqId("WorkEffort");
            Map<String, Object> createWorkMap = UtilMisc.toMap("partyId", partyId, "workEffortId", nextWorkEffortId,
                    "workEffortTypeId", "COWORK_TASK", "currentStatusId", "NOT_PROCESSED", "workEffortParentId", workEffortParentId, "priority", boundGenericValue.getString("priority"));
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createWorkMap, "banfftech.createWorkEffort", userLogin);
            dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                    UtilMisc.toMap("userLogin", userLogin, "workEffortId", nextWorkEffortId, "partyId", partyId));
        } else {
            //改为未处理
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", ddFormTask.getString("workEffortId"),
                    "currentStatusId", "NOT_PROCESSED", "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
        }
        if (parentWorkEffort.getString("currentStatusId").equals("COWORK_CREATED")) {
            //parent改为Request_DD
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "REQUESTED_DD", "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
        }
        // create NoteData for the Party
        if (UtilValidate.isEmpty(noteInfo)) {
            noteInfo = "Applicant to Vendor note";
        }
        Map<String, Object> noteMap = UtilMisc.toMap("noteName", "Applicant to Vendor", "noteInfo", noteInfo,
                "noteParty", userLogin.get("partyId"), "noteDateTime", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
        Map<String, Object> noteDataResult = CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), noteMap, "banfftech.createNoteData", userLogin);
        // create PartyNote
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("noteId", noteDataResult.get("noteId"), "partyId", partyId, "userLogin", userLogin), "banfftech.createPartyNote", userLogin);
        // send email
        VendorOnBoardingEmailEvents.toVendor(dispatcher, boundEntity, email);
    }

    /**
     * 完成注册
     */
    public static void completeCOWork(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException, GenericEntityException, GeneralServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        String workEffortId = (String) supplierParty.getPropertyValue("workEffortId");
        String partyId = (String) supplierParty.getPropertyValue("partyId");
        //完成workEffort
        dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "currentStatusId", "REGISTERED", "userLogin", userLogin));
        //启用supplier
        dispatcher.runSync("banfftech.updateParty", UtilMisc.toMap("partyId", partyId,
                "statusId", "PARTY_ENABLED", "userLogin", userLogin));
        //查看是否有未完成的cowork_task, 全部结束掉
        List<GenericValue> taskList = EntityQuery.use(delegator).from("WorkEffort").where("workEffortParentId", workEffortId, "currentStatusId", "NOT_PROCESSED").queryList();
        for (GenericValue task : taskList) {
            Map<String, Object> updateWorkMap = UtilMisc.toMap("workEffortId", task.getString("workEffortId"), "currentStatusId", "PROCESSED");
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), updateWorkMap, "banfftech.updateWorkEffort", userLogin);
        }
        //发送邮件
        VendorOnBoardingEmailEvents.vendorComplete(dispatcher, httpServletRequest, supplierParty);
    }

    public static Object getProcessFlow(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue coWork = supplierParty.getGenericValue();
        String partyId = coWork.getString("partyId");
        //根据状态变更记录补充所有的节点信息
        Map<String, Object> submitted = UtilMisc.toMap("nodeSeq", 0, "nodeName", "Created", "nodeDescription", "COWORK_CREATED", "isActive", false);
        Map<String, Object> ddRequested = UtilMisc.toMap("nodeSeq", 1, "nodeName", "BPDD Requested", "nodeDescription", "REQUESTED_DD", "isActive", false);
        Map<String, Object> ddCompleted = UtilMisc.toMap("nodeSeq", 2, "nodeName", "BPDD Submitted", "nodeDescription", "SUBMITTED_DD", "isActive", false);
        Map<String, Object> documentReady = UtilMisc.toMap("nodeSeq", 3, "nodeName", "Procurement Review", "nodeDescription", "PROCUREMENT_REVIEW", "isActive", false);
        Map<String, Object> complianceRequested = UtilMisc.toMap("nodeSeq", 4, "nodeName", "Compliance Review", "nodeDescription", "COMPLIANCE_REVIEW", "isActive", false);
        Map<String, Object> complianceCompleted = UtilMisc.toMap("nodeSeq", 5, "nodeName", "BPDD Completed", "nodeDescription", "COMPLETED_DD", "isActive", false);
        Map<String, Object> registered = UtilMisc.toMap("nodeSeq", 6, "nodeName", "Registered", "nodeDescription", "REGISTERED", "isActive", false);
        GenericValue procurementTask = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", partyId, "workEffortTypeId", "COWORK").queryFirst();
        if (UtilValidate.isNotEmpty(procurementTask)) {
            String workEffortId = procurementTask.getString("workEffortId");
            String statusId = procurementTask.getString("currentStatusId");
            GenericValue statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "COWORK_CREATED").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                submitted.put("setUser", setUserName);
                submitted.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                submitted.put("isActive", statusId.equals("COWORK_CREATED"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "REQUESTED_DD").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                ddRequested.put("setUser", setUserName);
                ddRequested.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                ddRequested.put("isActive", statusId.equals("REQUESTED_DD"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "SUBMITTED_DD").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                ddCompleted.put("setUser", setUserName);
                ddCompleted.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                ddCompleted.put("isActive", statusId.equals("SUBMITTED_DD"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "PROCUREMENT_REVIEW").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                documentReady.put("setUser", setUserName);
                documentReady.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                documentReady.put("isActive", statusId.equals("PROCUREMENT_REVIEW"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "COMPLIANCE_REVIEW").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                complianceRequested.put("setUser", setUserName);
                complianceRequested.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                complianceRequested.put("isActive", statusId.equals("COMPLIANCE_REVIEW"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus")
                    .where("workEffortId", workEffortId, "statusId", "COMPLETED_DD").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                complianceCompleted.put("setUser", setUserName);
                complianceCompleted.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                complianceCompleted.put("isActive", statusId.equals("COMPLETED_DD"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "REGISTERED").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                String setUserName = CommonUtils.getPartyNameByLoginId(delegator, statusGV.getString("setByUserLogin"));
                registered.put("setUser", setUserName);
                registered.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                registered.put("isActive", statusId.equals("REGISTERED"));
            }
        }
        return UtilMisc.toList(submitted, ddRequested, ddCompleted, documentReady, complianceRequested, complianceCompleted, registered);
    }

    public static void onHold(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue coWork = supplierParty.getGenericValue();
        String workEffortId = coWork.getString("workEffortId");
        dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "currentStatusId", "ON_HOLD", "userLogin", userLogin));
    }

    public static void enable(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue coWork = supplierParty.getGenericValue();
        String workEffortId = coWork.getString("workEffortId");
        //查找onHold的之前的状态 恢复到这个状态
        EntityCondition queryCondition = EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ON_HOLD"),
                EntityCondition.makeCondition("workEffortId", workEffortId));
        GenericValue lastStatus = EntityQuery.use(delegator).from("WorkEffortStatus").where(queryCondition).orderBy("-statusDatetime").queryFirst();
        dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "currentStatusId", lastStatus.getString("statusId"), "userLogin", userLogin));
    }


}
