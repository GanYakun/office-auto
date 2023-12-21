package com.banfftech.events;

import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author scy
 * @date 2023/12/21
 */
public class CustRequestEvents {
    private static final String MODULE = CustRequestEvents.class.getName();

    /**
     * rfq提交
     */
    public static void rfqSubmit(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity purchaseRequest = (OdataOfbizEntity) actionParameters.get("purchaseRequest");
        GenericValue purchaseRequestGen = purchaseRequest.getGenericValue();
        //状态改为已审提交
        Map<String, Object> serviceParams = new HashMap<>(purchaseRequestGen.getPrimaryKey());
        serviceParams.put("userLogin", userLogin);
        serviceParams.put("statusId", "RFQ_SUBMITTED");
        dispatcher.runSync("banfftech.updateCustRequest", serviceParams);
        //生成一个WorkEffort 分配给it
//        Map<String, Object> createReqParam = new HashMap<>();
//        createReqParam.put("custRequestId", delegator.getNextSeqId("CustRequest"));
//        createReqParam.put("userLogin", userLogin);
//        createReqParam.put("custRequestTypeId", "RF_PUR_QUOTE");
//        dispatcher.runSync("banfftech.createCustRequest", createReqParam);
    }


}
