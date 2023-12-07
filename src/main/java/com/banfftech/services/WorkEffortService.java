package com.banfftech.services;


import com.dpbird.odata.OfbizODataException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;


public class WorkEffortService {

    public static Map<String, Object> recordWorkEffortStatus(DispatchContext dctx, Map<String, Object> context) throws GeneralServiceException, GenericEntityException, OfbizODataException, GenericServiceException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String workEffortId = (String) context.get("workEffortId");
        String currentStatusId = (String) context.get("currentStatusId");
        Map<String, Object> serviceParam = new HashMap<>();
        serviceParam.put("workEffortId", workEffortId);
        serviceParam.put("statusId", currentStatusId);
        serviceParam.put("setByUserLogin", userLogin.getString("userLoginId"));
        //不精确到毫秒
        serviceParam.put("statusDatetime", UtilDateTime.toDateString(UtilDateTime.nowTimestamp(), "yyyy-MM-dd HH:mm:ss"));
        serviceParam.put("userLogin", userLogin);
        dispatcher.runSync("banfftech.createWorkEffortStatus", serviceParam);
        return ServiceUtil.returnSuccess();
    }

}
