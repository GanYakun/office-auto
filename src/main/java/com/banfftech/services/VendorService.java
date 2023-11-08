package com.banfftech.services;


import com.banfftech.common.util.CommonUtils;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.*;

import java.util.Map;


public class VendorService {

    public static Map<String, Object> createWorkEffortAndPartyGroupContact(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericServiceException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        //create Party
        Map<String, Object> serviceResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroupAndContact", userLogin);
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult;
        }
        //create WorkEffort
        String partyId = (String) serviceResult.get("partyId");
        context.put("partyId", partyId);
        context.put("workEffortId", delegator.getNextSeqId("WorkEffort"));
        context.put("workEffortTypeId", "COWORK_TASK");
        Map<String, Object> personServiceResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createWorkEffort", userLogin);
        if (ServiceUtil.isError(personServiceResult)) {
            return serviceResult;
        }
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put("partyId", partyId);
        return resultMap;
    }

    public static Map<String, Object> updateWorkEffortAndPartyGroupContact(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericServiceException, GenericEntityException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String partyId = (String) context.get("partyId");
        //update Party
        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updatePartyGroupAndContact", userLogin);
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put("partyId", partyId);
        return resultMap;
    }
}
