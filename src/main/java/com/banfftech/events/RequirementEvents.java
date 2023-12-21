package com.banfftech.events;

import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author scy
 * @date 2023/12/21
 */
public class RequirementEvents {
    private static final String MODULE = RequirementEvents.class.getName();

    /**
     * 审批Requirement
     */
    public static void approveRequirement(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity requirement = (OdataOfbizEntity) actionParameters.get("productRequirement");
        GenericValue requirementGen = requirement.getGenericValue();
        //状态改为已审批
        Map<String, Object> serviceParams = new HashMap<>(requirementGen.getPrimaryKey());
        serviceParams.put("userLogin", userLogin);
        serviceParams.put("statusId", "REQUIREMENT_APPROVED");
        dispatcher.runSync("banfftech.updateRequirement", serviceParams);
        //生成一个CustRequest 类型为RFQ
        String custRequestId = delegator.getNextSeqId("CustRequest");
        Map<String, Object> createReqParam = new HashMap<>();
        createReqParam.put("custRequestId", custRequestId);
        createReqParam.put("userLogin", userLogin);
        createReqParam.put("custRequestTypeId", "RF_PUR_QUOTE");
        createReqParam.put("statusId", "RFQ_CREATED");
        dispatcher.runSync("banfftech.createCustRequest", createReqParam);
        //创建RFQ Item
        Map<String, Object> createReqItemParam = new HashMap<>();
        createReqItemParam.put("custRequestId", custRequestId);
        createReqItemParam.put("productId", requirementGen.getString("productId"));
        createReqItemParam.put("quantity", requirementGen.getBigDecimal("quantity"));
        createReqItemParam.put("selectedAmount", requirementGen.getBigDecimal("estimatedBudget"));
        createReqItemParam.put("userLogin", userLogin);
        Map<String, Object> createItemResult = dispatcher.runSync("banfftech.createCustRequestItem", createReqItemParam);
        String custRequestItemSeqId = (String) createItemResult.get("custRequestItemSeqId");
        //创建关联
        dispatcher.runSync("banfftech.createRequirementCustRequest",
                UtilMisc.toMap("custRequestId", custRequestId, "custRequestItemSeqId", custRequestItemSeqId,
                        "requirementId", requirementGen.getString("requirementId"), "userLogin", userLogin));


    }


}
