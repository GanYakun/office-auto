package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import java.util.Map;

public class SupplierApproveEvents {
    /**
     * 传递
     */
    public static void transfer(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        //要传递的目标 procurement,applicant,compliance,vendor
        String target = (String) actionParameters.get("target");
        String comments = (String) actionParameters.get("comments");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        String workEffortId = (String) boundEntity.getPropertyValue("workEffortId");
        String workEffortParentId = (String) boundEntity.getPropertyValue("workEffortParentId");
        //当前的改为已处理
        Map<String, Object> updateWorkMap = UtilMisc.toMap("workEffortId", workEffortId, "currentStatusId", "PROCESSED");
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), updateWorkMap, "banfftech.updateWorkEffort", userLogin);
        //传递
        String transferTarget = getTransferTarget(delegator, target, boundEntity.getGenericValue());
        String nextWorkEffortId = delegator.getNextSeqId("WorkEffort");
        Map<String, Object> createWorkMap = UtilMisc.toMap("partyId", partyId, "workEffortId", nextWorkEffortId, "comments", comments,
                "workEffortTypeId", "COWORK_TASK", "currentStatusId", "NOT_PROCESSED", "workEffortParentId", workEffortParentId);
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), createWorkMap, "banfftech.createWorkEffort", userLogin);
        dispatcher.runSync("banfftech.createWorkEffortPartyAssignment",
                UtilMisc.toMap("userLogin", userLogin, "workEffortId", nextWorkEffortId, "partyId", transferTarget));
    }

    private static String getTransferTarget(Delegator delegator, String target, GenericValue workEffort) throws GenericEntityException {
        if ("procurement".equals(target)) {
            return "CG";
        }
        if ("compliance".equals(target)) {
            return "HG";
        }
        if ("applicant".equals(target)) {
            //获取申请人的部门
            GenericValue parentWorkEffort = workEffort.getRelatedOne("ParentWorkEffort", false);
            GenericValue createUser = parentWorkEffort.getRelatedOne("UserLogin", false);
            return CommonUtils.getPartyCompany(createUser.getString("partyId"), delegator);
        }
        if ("supplier".equals(target)) {
            return workEffort.getString("partyId");
        }
        return null;
    }

}
