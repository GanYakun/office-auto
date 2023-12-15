package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import com.dpbird.odata.handler.annotation.DraftAction;
import com.dpbird.odata.handler.annotation.DraftEventContext;
import com.dpbird.odata.handler.annotation.EdmEntity;
import com.dpbird.odata.handler.annotation.EdmService;
import org.apache.ofbiz.base.util.Debug;
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

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DDFormEvents {
    /**
     * submit DDForm
     */
    public static void submitDDForm(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        HttpServletRequest httpServletRequest = (HttpServletRequest) oDataContext.get("httpServletRequest");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity boundEntity = Util.getBoundEntity(actionParameters);
        if (UtilValidate.isEmpty(boundEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        String partyId = (String) boundEntity.getPropertyValue("partyId");
        GenericValue coWork = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", partyId, "approvePartyId", partyId, "workEffortTypeId", "COWORK_TASK").queryFirst();
        String workEffortId = coWork.getString("workEffortId");
        String workEffortParentId = coWork.getString("workEffortParentId");
        //当前的改为已处理
        Map<String, Object> updateWorkMap = UtilMisc.toMap("workEffortId", workEffortId, "currentStatusId", "PROCESSED", "workEffortParentId", workEffortParentId);
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), updateWorkMap, "banfftech.updateWorkEffort", userLogin);
        //parent 改为SUBMITTED_DD
        GenericValue parentWorkEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortParentId).queryOne();
        if (parentWorkEffort.getString("currentStatusId").equals("REQUESTED_DD")) {
            CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("workEffortId", workEffortParentId,
                    "currentStatusId", "SUBMITTED_DD", "userLogin", userLogin), "banfftech.updateWorkEffort", userLogin);
        }
        // create NoteData for the Party
        String noteInfo = (String) actionParameters.get("noteInfo");
        if (UtilValidate.isEmpty(noteInfo)) {
            noteInfo = "Submit BPDD note";
        }
        Map<String, Object> noteMap = UtilMisc.toMap("noteName", "Submit to applicant", "noteInfo", noteInfo,
                "noteParty", userLogin.get("partyId"), "noteDateTime", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
        Map<String, Object> noteDataResult = CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), noteMap, "banfftech.createNoteData", userLogin);
        // create PartyNote
        CommonUtils.setServiceFieldsAndRun(dispatcher.getDispatchContext(), UtilMisc.toMap("noteId", noteDataResult.get("noteId"), "partyId", partyId, "userLogin", userLogin), "banfftech.createPartyNote", userLogin);
        //检索是否勾选敏感问题,添加首次提交标志
        dispatcher.runSync("banfftech.ddFormCommitCheck",UtilMisc.toMap("supplierPartyId", partyId, "userLogin", userLogin));
        //发送邮件
        VendorOnBoardingEmailEvents.ddSubmit(dispatcher, httpServletRequest, boundEntity);
    }

    public static void uboUpload(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException, UnsupportedEncodingException {
        Debug.log("run ubo Upload =============");
    }

}
