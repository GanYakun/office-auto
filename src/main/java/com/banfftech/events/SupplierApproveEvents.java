package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.banfftech.services.UtilEmail;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
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
        //source: applicant, vendor, compliance
        String source = (String) actionParameters.get("source");
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        String workEffortId = (String) boundEntity.getPropertyValue("workEffortId");
        String priority = (String) boundEntity.getPropertyValue("priority");
        String workEffortParentId = (String) boundEntity.getPropertyValue("workEffortParentId");
        if (UtilValidate.isEmpty(workEffortParentId)) {
            //首次提交 创建ParentWorkEffort分配给采购
            Map<String, Object> createParentWorkMap = new HashMap<>();
            workEffortParentId = delegator.getNextSeqId("WorkEffort");
            createParentWorkMap.put("workEffortId", workEffortParentId);
            createParentWorkMap.put("workEffortTypeId", "COWORK");
            createParentWorkMap.put("priority", priority);
            createParentWorkMap.put("currentStatusId", "COWORK_CREATED");
            createParentWorkMap.put("partyId", partyId);
            createParentWorkMap.put("userLogin", userLogin);
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createParentWorkMap, "banfftech.createWorkEffort", userLogin);
            dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                    UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortParentId, "partyId", "CG"));
            List<GenericValue> coWorkTask = EntityQuery.use(delegator).from("WorkEffort").where("partyId", partyId, "workEffortTypeId", "COWORK_TASK").queryList();
            for (GenericValue task : coWorkTask) {
                dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", task.getString("workEffortId"),
                        "workEffortParentId", workEffortParentId, "userLogin", userLogin));
            }
        }
//        else if ("applicant".equals(source)) {
//            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
//                    "currentStatusId", "COWORK_CREATED", "userLogin", userLogin));
//        }
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
            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "COMPLETE_COMP", "userLogin", userLogin));
        }
        //发送邮件
        sendEmailToTarget(delegator, "procurement", httpServletRequest, boundEntity, null, null);
    }

    /**
     * 采购传递给其他部门
     */
    public static void procurementReturn(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException, UnsupportedEncodingException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        String target = (String) actionParameters.get("target");
        String comments = (String) actionParameters.get("comments"); // 也许不需要comments了
        String noteInfo = (String) actionParameters.get("noteInfo");
        String ddFormType = (String) actionParameters.get("ddFormType");
        String email = (String) actionParameters.get("email");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        GenericValue boundGenericValue = boundEntity.getGenericValue();
        String partyId = boundGenericValue.getString("partyId");
        String workEffortParentId = boundGenericValue.getString("workEffortId");
//        if ("applicant".equals(target)) {
//            //打回给applicant 改为需要修改
//            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
//                    "currentStatusId", "REQUIRE_CHANGES", "userLogin", userLogin));
//        }
        if (UtilValidate.isNotEmpty(ddFormType)) {
            GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
            CommonUtils.setObjectAttribute(party, "ddFormType", ddFormType);
        }
        String transferTarget = getTransferTarget(delegator, target, boundGenericValue);
        if (UtilValidate.isEmpty(transferTarget)) {
            throw new OfbizODataException("No department found");
        }
        GenericValue supplierTask = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact")
                .where("approvePartyId", transferTarget, "workEffortParentId", workEffortParentId).queryFirst();
        if (UtilValidate.isEmpty(supplierTask)) {
            //首次传递
            String nextWorkEffortId = delegator.getNextSeqId("WorkEffort");
            Map<String, Object> createWorkMap = UtilMisc.toMap("partyId", partyId, "workEffortId", nextWorkEffortId, "comments", comments,
                    "workEffortTypeId", "COWORK_TASK", "currentStatusId", "NOT_PROCESSED", "workEffortParentId", workEffortParentId, "priority", boundGenericValue.getString("priority"));
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createWorkMap, "banfftech.createWorkEffort", userLogin);
            dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                    UtilMisc.toMap("userLogin", userLogin, "workEffortId", nextWorkEffortId, "partyId", transferTarget));
        } else {
            //改为未处理
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", supplierTask.getString("workEffortId"),
                    "currentStatusId", "NOT_PROCESSED", "comments", comments, "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
        }
        if ("supplier".equals(target)) {
            //发送DD,状态改为REQUEST_DD
            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "REQUEST_DD", "userLogin", userLogin));
        }
        if ("compliance".equals(target)) {
            //发送给合规,状态改为REQUEST_COMP
            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "REQUEST_COMP", "userLogin", userLogin));
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
        //发送邮件
        sendEmailToTarget(delegator, target, httpServletRequest, boundEntity, boundGenericValue, email);
    }


    /**
     * 发送DD
     */
    public static void sendDDForm(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException, UnsupportedEncodingException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        String target = (String) actionParameters.get("target");
        String noteInfo = (String) actionParameters.get("noteInfo");
        String ddFormType = (String) actionParameters.get("ddFormType");
        String email = (String) actionParameters.get("email");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        GenericValue boundGenericValue = boundEntity.getGenericValue();
        String partyId = boundGenericValue.getString("partyId");
        GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
        String workEffortParentId = null;
        GenericValue parentWorkEffort = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", partyId, "workEffortTypeId", "COWORK").queryFirst();
        if (UtilValidate.isNotEmpty(parentWorkEffort)) {
            workEffortParentId = parentWorkEffort.getString("workEffortId");
        }
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
        //标记发送方, 提交时要向这里提交
        String ddFormSource;
        if ("COWORK".equals(boundGenericValue.getString("workEffortTypeId"))) {
            //发送DD,状态改为REQUEST_DD
            ddFormSource = "procurement";
            dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", boundGenericValue.getString("workEffortId"),
                    "currentStatusId", "REQUEST_DD", "userLogin", userLogin));
        } else {
            ddFormSource = "applicant";
        }
        CommonUtils.setObjectAttribute(party, "ddFormSource", ddFormSource);
        // create NoteData for the Party
        if (UtilValidate.isEmpty(noteInfo)) {
            noteInfo = "Procurement to " + target + " note";
        }
        Map<String, Object> noteMap = UtilMisc.toMap("noteName", "Procument to " + target, "noteInfo", noteInfo,
                "noteParty", userLogin.get("partyId"), "noteDateTime", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
        Map<String, Object> noteDataResult = CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), noteMap, "banfftech.createNoteData", userLogin);
        // create PartyNote
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("noteId", noteDataResult.get("noteId"), "partyId", partyId, "userLogin", userLogin), "banfftech.createPartyNote", userLogin);
        //发送邮件
        sendEmailToTarget(delegator, target, httpServletRequest, boundEntity, boundGenericValue, email);
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
            GenericValue firstTask = EntityQuery.use(delegator).from("WorkEffort").where("partyId", parentWorkEffort.getString("partyId")).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", firstTask.getString("createdByUserLogin")), false);
            return CommonUtils.getPartyCompany(createUser.getString("partyId"), delegator);
        }
        if ("supplier".equals(target)) {
            return parentWorkEffort.getString("partyId");
        }
        return null;
    }

    /**
     * 完成注册
     */
    public static void completeCOWork(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        String workEffortId = (String) supplierParty.getPropertyValue("workEffortId");
        String partyId = (String) supplierParty.getPropertyValue("partyId");
        //完成workEffort
        dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "currentStatusId", "COMPLETE", "userLogin", userLogin));
        //启用supplier
        dispatcher.runSync("banfftech.updateParty", UtilMisc.toMap("partyId", partyId,
                "statusId", "PARTY_ENABLED", "userLogin", userLogin));
        //发送邮件
        String content = "We are pleased to inform you that the supplier registration application for [SupplierXXX], submitted by [ApplicantXXX], has been successfully processed and completed.\n" +
                "The supplier account is now active, and we appreciate your prompt attention to this matter. This successful registration marks an important step towards establishing a partnership with [SupplierXXX].\n" +
                "If you have any further questions or require additional information, please do not hesitate to contact us at [XXXXXXX@banff-tech.com].\n" +
                "Thank you for your cooperation, and we look forward to the potential collaboration with [SupplierXXX].\n" +
                "Best regards";
        try {
            GenericValue firstTask = EntityQuery.use(delegator).from("WorkEffort").where("partyId", partyId).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", firstTask.getString("createdByUserLogin")), false);
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            String applicantEmail = applicantParty.getString("primaryEmail");
            if (UtilValidate.isNotEmpty(applicantEmail)) {
                UtilEmail.sendEmail(applicantEmail, "Supplier registration completed", content);
            } else {
                Debug.logWarning("No email address", MODULE);
            }
        } catch (MessagingException | GenericEntityException e) {
            Debug.log("邮件发送失败: " + e.getMessage());
        }
    }


    public static void sendEmailToTarget(Delegator delegator, String target, HttpServletRequest httpServletRequest, OdataOfbizEntity entity,
                                         GenericValue parentWorkEffort, String defaultEmail) throws GenericEntityException, UnsupportedEncodingException {
        //获取app访问地址和邮件
        String currentUrl = httpServletRequest.getRequestURL().toString().replace(httpServletRequest.getRequestURI(), "");
        Object supplierPartyId = entity.getPropertyValue("partyId");
        String odataId = entity.getId().toString();
        String targetEmail = defaultEmail;
        if ("procurement".equals(target)) {
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierPartyId, "approvePartyId", "CG", "workEffortTypeId", "COWORK").queryFirst();
            if (UtilValidate.isEmpty(coWork)) {
                return;
            }
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/menu6/supplierapprove-managebyprocurement/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            targetEmail = procurement.getString("primaryEmail");
        }
        if ("compliance".equals(target)) {
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierPartyId, "approvePartyId", "HG", "workEffortTypeId", "COWORK_TASK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            currentUrl += "/menu6/supplierapprove-managebycompliance/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "compliance").queryFirst();
            targetEmail = procurement.getString("primaryEmail");
        }
        if ("applicant".equals(target)) {
            GenericValue firstTask = EntityQuery.use(delegator).from("WorkEffort").where("partyId", supplierPartyId).orderBy("createdDate").queryFirst();
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", firstTask.getString("createdByUserLogin")), false);
            String createCompany = CommonUtils.getPartyCompany(createUser.getString("partyId"), delegator);
            GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", supplierPartyId, "approvePartyId", createCompany, "workEffortTypeId", "COWORK_TASK").queryFirst();
            String coWorkId = coWork.getString("workEffortId");
            odataId = odataId.replaceAll("'[^']*'", "'" + coWorkId + "'");
            GenericValue applicantParty = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", createUser.getString("partyId")).queryFirst();
            currentUrl += "/menu6/supplierapprove-managebyapplication/SupplierPartiesObjectPage?queryEntity=" + URLEncoder.encode(odataId, "UTF-8");
            targetEmail = applicantParty.getString("primaryEmail");
        }
        if ("supplier".equals(target)) {
            odataId = odataId.replaceAll("'[^']*'", "'" + supplierPartyId + "'");
            currentUrl += "/o3/#Supplier-DDForm&/" + URLEncoder.encode(odataId, "UTF-8");
        }
        //发送邮件
        String content = "Vendor Onboarding Progress Update Notification\n" +
                "<br><br>" +
                "Details:\n" + currentUrl +
                "<br><br>" +
                "Kindly attend to this matter promptly and update the task status accordingly. Should you have any questions or require further assistance, feel free to reach out.\n" +
                "<br><br>" +
                "Thank you,\n" +
                "<br><br>" +
                "[FormXXX]";
        try {
            if (UtilValidate.isNotEmpty(targetEmail)) {
                UtilEmail.sendEmail(targetEmail, "Vendor On-boarding", content);
            } else {
                Debug.logWarning("No email address", MODULE);
            }
        } catch (MessagingException e) {
            Debug.log("邮件发送失败: " + e.getMessage());
        }

    }

    public static Object getProcessFlow(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        GenericValue coWork = supplierParty.getGenericValue();
        String partyId = coWork.getString("partyId");
        //根据状态变更记录补充所有的节点信息
        Map<String, Object> submitted = UtilMisc.toMap("nodeSeq", 0, "nodeName", "Submitted", "nodeDescription", "COWORK_CREATED", "isActive", false);
        Map<String, Object> ddRequested = UtilMisc.toMap("nodeSeq", 1, "nodeName", "DD Requested", "nodeDescription", "REQUEST_DD", "isActive", false);
        Map<String, Object> ddCompleted = UtilMisc.toMap("nodeSeq", 2, "nodeName", "DD Completed", "nodeDescription", "COMPLETE_DD", "isActive", false);
        Map<String, Object> documentReady = UtilMisc.toMap("nodeSeq", 3, "nodeName", "Document Ready", "nodeDescription", "DOC_READY", "isActive", false);
        Map<String, Object> complianceRequested = UtilMisc.toMap("nodeSeq", 4, "nodeName", "Compliance Requested", "nodeDescription", "REQUEST_COMP", "isActive", false);
        Map<String, Object> complianceCompleted = UtilMisc.toMap("nodeSeq", 5, "nodeName", "Compliance Completed", "nodeDescription", "COMPLETE_COMP", "isActive", false);
        Map<String, Object> registered = UtilMisc.toMap("nodeSeq", 6, "nodeName", "Registered", "nodeDescription", "COMPLETE", "isActive", false);
        GenericValue procurementTask = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", partyId, "workEffortTypeId", "COWORK").queryFirst();
        if (UtilValidate.isNotEmpty(procurementTask)) {
            String workEffortId = procurementTask.getString("workEffortId");
            String statusId = procurementTask.getString("currentStatusId");
            GenericValue statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "COWORK_CREATED").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                submitted.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                submitted.put("isActive", statusId.equals("COWORK_CREATED"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus").where("workEffortId", workEffortId, "statusId", "REQUEST_DD").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                ddRequested.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                ddRequested.put("isActive", statusId.equals("REQUEST_DD"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus")
                    .where("workEffortId", workEffortId, "statusId", "COMPLETE_DD").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                ddCompleted.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                ddCompleted.put("isActive", statusId.equals("COMPLETE_DD"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus")
                    .where("workEffortId", workEffortId, "statusId", "DOC_READY").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                documentReady.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                documentReady.put("isActive", statusId.equals("DOC_READY"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus")
                    .where("workEffortId", workEffortId, "statusId", "REQUEST_COMP").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                complianceRequested.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                complianceRequested.put("isActive", statusId.equals("REQUEST_COMP"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus")
                    .where("workEffortId", workEffortId, "statusId", "COMPLETE_COMP").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                complianceCompleted.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                complianceCompleted.put("isActive", statusId.equals("COMPLETE_COMP"));
            }
            statusGV = EntityQuery.use(delegator).from("WorkEffortStatus")
                    .where("workEffortId", workEffortId, "statusId", "COMPLETE").orderBy("statusDatetime").queryFirst();
            if (UtilValidate.isNotEmpty(statusGV)) {
                registered.put("nodeStartDate", statusGV.getTimestamp("statusDatetime"));
                registered.put("isActive", statusId.equals("COMPLETE"));
            }
        }
        return UtilMisc.toList(submitted, ddRequested, ddCompleted, documentReady, complianceRequested, complianceCompleted, registered);
    }


}
