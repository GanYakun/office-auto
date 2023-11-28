package com.banfftech.events;

import com.banfftech.services.UtilEmail;
import com.dpbird.odata.OfbizODataException;
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
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

import javax.mail.MessagingException;

@EdmService(edmApp = "supplierForm")
public class DDFormEvents {

    @EdmEntity(entityTypes = "SupplierParty", action = DraftAction.SAVE_AFTER)
    public void saveBefore(DraftEventContext context) throws OfbizODataException {
        LocalDispatcher dispatcher = context.getDispatcher();
        Delegator delegator = context.getDelegator();
        GenericValue userLogin = context.getUserLogin();
        OdataOfbizEntity odataOfbizEntity = context.getOdataOfbizEntity();
        String partyId = (String) odataOfbizEntity.getPropertyValue("partyId");
        try {
            //DDForm保存提交 检索是否勾选敏感问题,添加首次提交标志,更新任务状态
            dispatcher.runSync("banfftech.ddFormCommitCheck",UtilMisc.toMap("supplierPartyId", partyId, "userLogin", userLogin));
            GenericValue workEffortTask = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("partyId", partyId, "approvePartyId", partyId).queryFirst();
            if (UtilValidate.isNotEmpty(workEffortTask)) {
                dispatcher.runSync("banfftech.updateWorkEffort", UtilMisc.toMap("workEffortId", workEffortTask.getString("workEffortId"),
                        "currentStatusId", "PROCESSED", "userLogin", userLogin));
            }
            //发送邮件给procurement
            GenericValue procurement = EntityQuery.use(delegator).from("PartyAndContact").where("partyId", "procurement").queryFirst();
            String email = procurement.getString("primaryEmail");
            if (UtilValidate.isNotEmpty(email)) {
                UtilEmail.sendEmail(email, "Submit to procurement", "Submit to procurement: " + UtilDateTime.nowTimestamp());
            }
        } catch (GenericServiceException | MessagingException | GenericEntityException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }
}
