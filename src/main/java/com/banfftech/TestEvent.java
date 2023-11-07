package com.banfftech;

import com.banfftech.bean.FlowHelper;
import com.banfftech.bean.Manual;
import com.banfftech.bean.NodeUser;
import com.banfftech.bean.TreeNode;
import com.banfftech.common.util.PartyServiceUtils;
import com.dpbird.odata.OfbizAppEdmProvider;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import com.dpbird.odata.edm.OfbizCsdlEntityType;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.sf.json.JSONObject;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理审批流相关代码
 *
 * @author scy
 * @date 2023/9/11
 */
public class TestEvent {

    /**
     * 流程设计发布,客户端会传递完整的流程json,保存它
     */
    public static void saveProcess(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                   EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("mainProcess");
        GenericValue genericValue = ofbizEntity.getGenericValue();
        //所有的节点数据
        String nodeData = (String) actionParameters.get("nodeData");
        Boolean enable = (Boolean) actionParameters.get("enable");
        GenericValue process = EntityQuery.use(delegator).from("MainProcess").where(genericValue.getPrimaryKey()).queryOne();
        String workFlowId = process.getString("workFlowId");
        if (UtilValidate.isEmpty(workFlowId)) {
            //创建流程
            String noteId = delegator.getNextSeqId("NoteData");
            delegator.create("NoteData", UtilMisc.toMap("noteId", noteId, "noteName", "FLOW_JSON",
                    "noteInfo", nodeData, "noteDateTime", UtilDateTime.nowTimestamp()));
            String workEffortId = delegator.getNextSeqId("WorkEffort");
            delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "workEffortTypeId", "WORK_FLOW", "noteId", noteId));
            process.set("workFlowId", workEffortId);
        } else {
            //更新流程
            GenericValue workEffort = process.getRelatedOne("WorkEffort", false);
            GenericValue noteData = workEffort.getRelatedOne("NoteData", false);
            noteData.set("noteInfo", nodeData);
            noteData.set("noteDateTime", UtilDateTime.nowTimestamp());
            noteData.store();
            //更新结构缓存
            FlowHelper.flushTreeNodeCache(noteData);
        }
        if (UtilValidate.isNotEmpty(enable) && enable) {
            //停用所有相同业务对象的流程
            delegator.storeByCondition("MainProcess", UtilMisc.toMap("statusId", "PROCESS_NOT_ENABLED"),
                    EntityCondition.makeCondition("dbEntityId", genericValue.getString("dbEntityId")));
            //启用当前流程
            process.set("statusId", "PROCESS_ENABLED");
        }
        process.store();
    }

    /**
     * 发布
     */
    public static void publishFlow(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                   EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("mainProcess");
        //停用所有相同业务对象的流程
        delegator.storeByCondition("MainProcess", UtilMisc.toMap("statusId", "PROCESS_NOT_ENABLED"),
                EntityCondition.makeCondition("dbEntityId", ofbizEntity.getPropertyValue("dbEntityId")));
        //启用当前流程
        delegator.storeByCondition("MainProcess", UtilMisc.toMap("statusId", "PROCESS_ENABLED"),
                EntityCondition.makeCondition(ofbizEntity.getGenericValue().getPrimaryKey()));
    }

    /**
     * 停用
     */
    public static void stopFlow(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("mainProcess");
        delegator.storeByCondition("MainProcess", UtilMisc.toMap("statusId", "PROCESS_NOT_ENABLED"),
                EntityCondition.makeCondition(ofbizEntity.getGenericValue().getPrimaryKey()));
    }

    /**
     * 提交审批
     */
    public static void submitApproval(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OfbizAppEdmProvider edmProvider = (OfbizAppEdmProvider) oDataContext.get("edmProvider");
        HttpServletRequest request = (HttpServletRequest) oDataContext.get("httpServletRequest");
        OfbizCsdlEntityType csdlEntityType = (OfbizCsdlEntityType) edmProvider.getEntityType(edmBindingTarget.getEntityType().getFullQualifiedName());
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.values().stream().filter(v -> v instanceof OdataOfbizEntity).findFirst().get();
        String approverData = (String) actionParameters.get("approverData");
        GenericValue genericValue = ofbizEntity.getGenericValue();
        String entityName = genericValue.getEntityName();
        //修改业务对象状态 审批中
        FlowHelper.updateEntityStatus(genericValue, dispatcher, "APPROVAL_SUBMITTED");
        GenericValue templateWorkEffort = FlowHelper.getTemplateWorkEffort(delegator, entityName, getTypeId(csdlEntityType));
        GenericValue noteData = templateWorkEffort.getRelatedOne("NoteData", false);
        String flowJson = noteData.getString("noteInfo");
        JSONObject jsonObject = JSONObject.fromObject(flowJson);
        //用户自选参数
        String runtimeDataId = delegator.getNextSeqId("RuntimeData");
        delegator.create("RuntimeData", UtilMisc.toMap("runtimeDataId", runtimeDataId, "runtimeInfo", approverData));
        //创建根节点
        String workEffortId = delegator.getNextSeqId("WorkEffort");
        GenericValue rootWorkEffort = delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "workEffortName", jsonObject.getString("nodeName"), "workEffortTypeId", "ROOT_NODE",
                "priority", jsonObject.getLong("nodeId"), "workEffortParentId", templateWorkEffort.getString("workEffortId"),
                "createdByUserLogin", userLogin.getString("userLoginId"),
                "createdDate", UtilDateTime.nowTimestamp(), "runtimeDataId", runtimeDataId));
        //把审批对象关联到根节点
        ModelEntity modelEntity = genericValue.getModelEntity();
        //重复提交
        delegator.removeByAnd("WorkFlowMember", UtilMisc.toMap("memberEntityName", entityName, "memberEntityId", genericValue.getString(modelEntity.getFirstPkFieldName())));
        delegator.create("WorkFlowMember", UtilMisc.toMap("workFlowMemberId", delegator.getNextSeqId("WorkFlowMember"),
                "workEffortId", workEffortId, "memberEntityName", entityName, "memberEntityId", genericValue.getString(modelEntity.getFirstPkFieldName())));
        rootWorkEffort.set("currentStatusId", "WEPR_WAIT");
        rootWorkEffort.store();
    }

    /**
     * 获取用户自选参数
     */
    public static Object getApproverData(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, JsonProcessingException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OfbizAppEdmProvider edmProvider = (OfbizAppEdmProvider) oDataContext.get("edmProvider");
        HttpServletRequest request = (HttpServletRequest) oDataContext.get("httpServletRequest");
        OfbizCsdlEntityType csdlEntityType = (OfbizCsdlEntityType) edmProvider.getEntityType(edmBindingTarget.getEntityType().getFullQualifiedName());
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.values().stream().filter(v -> v instanceof OdataOfbizEntity).findFirst().get();
        GenericValue genericValue = ofbizEntity.getGenericValue();
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<TreeNode> customerDefNodes = FlowHelper.getCustomerDefNodes(delegator, genericValue, getTypeId(csdlEntityType));
        for (TreeNode customerDefNode : customerDefNodes) {
            NodeUser nodeUserList = customerDefNode.getNodeUserList();
            Manual manual = nodeUserList.getManual();
            Map<String, Object> optionMap = UtilGenerics.checkMap(manual.getApprover().getValue());
            //人员范围类型
            JSONObject selectJson = new JSONObject();
            if ("company".equals(optionMap.get("type")) || "company".equals(optionMap.get("value"))) {
                //公司id, 获取公司全部人员
                List<GenericValue> allMembers = new ArrayList<>();
                PartyServiceUtils.getDepartmentALlMembers(delegator, "Company", allMembers);
                for (GenericValue member : allMembers) {
                    selectJson.put(member.getString("partyId"), member.getString("partyName"));
                }
            } else if ("party".equals(optionMap.get("type"))) {
                List<GenericValue> parties = EntityQuery.use(delegator).from("Party").select("partyId", "partyName")
                        .where(EntityCondition.makeCondition("partyId", EntityOperator.IN, optionMap.get("value"))).queryList();
                for (GenericValue party : parties) {
                    selectJson.put(party.getString("partyId"), party.getString("partyName"));
                }
            } else if ("role".equals(optionMap.get("type"))) {
                //根据角色查询人员
                List<String> roles = UtilGenerics.checkList(optionMap.get("value"));
                List<String> partyIds = EntityQuery.use(delegator).from("PartyRole")
                        .where(EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, roles)).getFieldList("partyId");
                List<GenericValue> parties = EntityQuery.use(delegator).from("Party").select("partyId", "partyName")
                        .where(EntityCondition.makeCondition("partyId", EntityOperator.IN, partyIds)).queryList();
                for (GenericValue party : parties) {
                    selectJson.put(party.getString("partyId"), party.getString("partyName"));
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("nodeId", customerDefNode.getNodeId());
            item.put("nodeName", customerDefNode.getNodeName());
            item.put("selectList", selectJson.toString());
            resultList.add(item);
        }
        return resultList;
    }


    /**
     * 类型选项数据
     */
    public static Object getTypeData(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        String typeFieldName = (String) actionParameters.get("typeFieldName");
        String typeEntityName = Util.firstUpperCase(typeFieldName.substring(0, typeFieldName.length() - 2));
        List<GenericValue> genericValues = EntityQuery.use(delegator).from(typeEntityName).queryList();
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (GenericValue genericValue : genericValues) {
            resultList.add(UtilMisc.toMap("label", genericValue.getString("description"), "value", genericValue.getString(typeFieldName)));
        }
        return resultList;
    }


    /**
     * 撤回提交的审批
     */
    public static Object cancelApproval(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.values().stream().filter(v -> v instanceof OdataOfbizEntity).findFirst().get();
        GenericValue genericValue = ofbizEntity.getGenericValue();
        Object primaryKey = new HashMap<>(genericValue.getPrimaryKey()).entrySet().iterator().next().getValue();
        GenericValue workFlowMember = EntityQuery.use(delegator).from("WorkFlowMember")
                .where("memberEntityName", genericValue.getEntityName(), "memberEntityId", primaryKey).queryFirst();
        GenericValue rootWorkEffort = workFlowMember.getRelatedOne("WorkEffort", false);
        //判断审批是否已经开始 没有审批才可以撤回
        List<GenericValue> approvalNodes = rootWorkEffort.getRelated("NodeWorkEffort", UtilMisc.toMap("workEffortTypeId", "APPROVAL"), null, false);
        for (GenericValue approvalNode : approvalNodes) {
            if (!approvalNode.getString("currentStatusId").equals("WEPR_WAIT")) {
                throw new OfbizODataException("撤回失败,审批已在进行中");
            }
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition(approvalNode.getPrimaryKey()),
                    EntityCondition.makeCondition("statusId", EntityOperator.IN, UtilMisc.toList("WEPR_COMPLETE", "WEPR_REFUSE"))));
            long approvedCount = EntityQuery.use(delegator).from("WorkEffortPartyAssignment").where(condition).queryCount();
            if (approvedCount > 0) {
                throw new OfbizODataException("撤回失败,审批已在进行中");
            }
        }
        //将所有节点改为取消
        List<Object> workEffortIds = EntityUtil.getFieldListFromEntityList(approvalNodes, "workEffortId", true);
        delegator.storeByCondition("WorkEffortPartyAssignment", UtilMisc.toMap("statusId", "WEPR_CANCEL"),
                EntityCondition.makeCondition("workEffortId", EntityOperator.IN, workEffortIds));
        delegator.storeByCondition("WorkEffort", UtilMisc.toMap("currentStatusId", "WEPR_CANCEL"),
                EntityCondition.makeCondition("topWorkEffortId", EntityOperator.EQUALS, rootWorkEffort.getString("workEffortId")));
        rootWorkEffort.set("currentStatusId", "WEPR_CANCEL");
        rootWorkEffort.store();

        //将审批对象状态改为已创建
        FlowHelper.updateEntityStatus(genericValue, dispatcher, "APPROVAL_CREATED");
        return null;
    }

    /**
     * 查看审批进度 返回当前待处理节点
     */
    public static Object viewApproval(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.values().stream().filter(v -> v instanceof OdataOfbizEntity).findFirst().get();
        GenericValue genericValue = ofbizEntity.getGenericValue();
        Object primaryKey = new HashMap<>(genericValue.getPrimaryKey()).entrySet().iterator().next().getValue();
        GenericValue workFlowMember = EntityQuery.use(delegator).from("WorkFlowMember")
                .where("memberEntityName", genericValue.getEntityName(), "memberEntityId", primaryKey).queryFirst();
        GenericValue rootWorkEffort = workFlowMember.getRelatedOne("WorkEffort", false);
        GenericValue currentActiveNode = EntityQuery.use(delegator).from("WorkEffort")
                .where("topWorkEffortId", rootWorkEffort.getString("workEffortId"), "currentStatusId", "WEPR_WAIT").queryFirst();
        if (UtilValidate.isNotEmpty(currentActiveNode)) {
            GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess")
                    .where("workFlowId", rootWorkEffort.getString("workEffortParentId")).queryFirst();
            return UtilMisc.toMap("processId", mainProcess.getString("processId"), "nodeId", currentActiveNode.getLong("priority"));
        }
        return null;
    }

    public static String getTypeId(OfbizCsdlEntityType csdlEntityType) {
        String typeId = null;
        String ofbizType = csdlEntityType.getOfbizType();
        if (UtilValidate.isNotEmpty(ofbizType)) {
            typeId = ofbizType.split("=")[1];
        }
        return typeId;
    }

    /**
     * 审批通过/拒绝
     */
    public static void approve(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) actionParameters.get("approval");
        GenericValue approval = odataOfbizEntity.getGenericValue();
        String workEffortPartyAssignmentId = approval.getString("workEffortPartyAssignmentId");
        String comments = (String) actionParameters.get("comments");
        String statusId = (String) actionParameters.get("statusId");
        try {
            dispatcher.runSync("banfftech.updateWorkEffortPartyAssignment",
                    UtilMisc.toMap("workEffortPartyAssignmentId",workEffortPartyAssignmentId,"statusId",statusId,
                            "comments",comments,"mannerEnumId","PASS_APPROVE", "thruDate", UtilDateTime.nowTimestamp(), "userLogin",userLogin));
        } catch (GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

}
