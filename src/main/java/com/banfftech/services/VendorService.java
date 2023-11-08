package com.banfftech.services;


import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OfbizODataException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.*;

import java.util.Map;


public class VendorService {

    public static Map<String, Object> updatePartyStatusToEnable(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String partyId = (String) context.get("partyId");
        dispatcher.runSync("banfftech.updateParty", UtilMisc.toMap("userLogin", userLogin,
                "partyId", partyId, "statusId", "PARTY_ENABLED"));
        return resultMap;
    }

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

    public static Map<String, Object> createSupplierRole(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String partyId = (String) context.get("partyId");
        dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin,
                "partyId", partyId, "roleTypeId", "VENDOR"));
        return resultMap;
    }

    public static Map<String, Object> createSupplierUserLogin(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String partyId = (String) context.get("partyId");
        dispatcher.runSync("banfftech.createUserLogin", UtilMisc.toMap("userLogin", userLogin, "enabled", "Y",
                "partyId", partyId));
        return resultMap;
    }

}
