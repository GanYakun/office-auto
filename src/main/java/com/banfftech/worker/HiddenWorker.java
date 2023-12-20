package com.banfftech.worker;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.util.List;

public class HiddenWorker {
    public static Boolean applicantSubmitIsHidden(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        if (SupplierWorker.getDDFormType(supplierParty, delegator).equals("No DD")){
            return false;
        }

        GenericValue applicantWorkEffort = EntityQuery.use(delegator).from("WorkEffort").
                where("partyId", supplierParty.getString("partyId"), "workEffortTypeId", "COWORK_TASK").orderBy("createdDate").queryFirst();

        List<GenericValue> vendorWorkEfforts = delegator.findByAnd("WorkEffortAndPartyGroupContact",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "approvePartyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isEmpty(vendorWorkEfforts)){
            return true;
        }
        GenericValue vendorWorkEffort = EntityUtil.getFirst(vendorWorkEfforts);
        if (applicantWorkEffort.get("currentStatusId").equals("PROCESSED") || vendorWorkEffort.get("currentStatusId").equals("NOT_PROCESSED")){
            return true;
        }
        return false;
    }

    public static Boolean procurementSubmitIsHidden(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean procurementSubmitHidden = true;
        List<GenericValue> coworks = delegator.findByAnd("WorkEffort",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "workEffortTypeId", "COWORK"), null, true);
        if (UtilValidate.isNotEmpty(coworks)){
            GenericValue cowork = EntityUtil.getFirst(coworks);
            if (cowork.get("currentStatusId").equals("PROCUREMENT_REVIEW") || cowork.get("currentStatusId").equals("COMPLETED_DD")){
                procurementSubmitHidden = false;
            }
            if (cowork.get("currentStatusId").equals("COMPLIANCE_REVIEW")){
                //如果被合规拒绝 也可以reject
                GenericValue complianceWorkEffort = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("approvePartyId", "HG").queryFirst();
                if (UtilValidate.isNotEmpty(complianceWorkEffort) && "PROCESSED".equals(complianceWorkEffort.getString("currentStatusId"))) {
                    procurementSubmitHidden = false;
                }
            }
        }
        return procurementSubmitHidden;
    }

    public static Boolean procurementRejectIsHidden(GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Boolean procurementRejectHidden = true;
        List<GenericValue> coworks = delegator.findByAnd("WorkEffort",
                UtilMisc.toMap("partyId", supplierParty.get("partyId"), "workEffortTypeId", "COWORK"), null, true);
        if (UtilValidate.isNotEmpty(coworks)){
            GenericValue cowork = EntityUtil.getFirst(coworks);
            if (cowork.get("currentStatusId").equals("PROCUREMENT_REVIEW")){
                procurementRejectHidden = false;
            }
            if (cowork.get("currentStatusId").equals("COMPLIANCE_REVIEW")){
                //如果被合规拒绝 也可以reject
                GenericValue complianceWorkEffort = EntityQuery.use(delegator).from("WorkEffortAndPartyGroupContact").where("approvePartyId", "HG").queryFirst();
                if (UtilValidate.isNotEmpty(complianceWorkEffort) && "PROCESSED".equals(complianceWorkEffort.getString("currentStatusId"))) {
                    procurementRejectHidden = false;
                }
            }
        }
        return procurementRejectHidden;
    }

}
