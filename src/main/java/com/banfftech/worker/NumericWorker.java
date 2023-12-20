package com.banfftech.worker;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.util.List;
import java.util.Map;

public class NumericWorker {
    public static Long getClassificationRatingNumber (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Long criticalValue = 0L;
        Map<String, Object> statusMap = UtilMisc.toMap("High",3L,"Low",1L,"Medium",2L);
        List<GenericValue> partyClassifications = delegator.findByAnd("PartyClassification",
                UtilMisc.toMap("partyId", supplierParty.get("partyId")), null, true);
        if (UtilValidate.isNotEmpty(partyClassifications)){
            GenericValue partyClassification = EntityUtil.getFirst(partyClassifications);
            GenericValue partyClassificationGroup = delegator.findOne("PartyClassificationGroup",
                    UtilMisc.toMap("partyClassificationGroupId", partyClassification.get("partyClassificationGroupId")), true);
            String description = partyClassificationGroup.getString("description");
            criticalValue = (Long) statusMap.get(description);
        }
        return criticalValue;
    }

    public static Long getProcessNumeric (GenericValue supplierParty, Delegator delegator) throws GenericEntityException {
        Map<String, Object> processBarMap = UtilMisc.toMap("COWORK_CREATED", 1L, "REQUESTED_DD", 2L,
                "SUBMITTED_DD", 3L, "PROCUREMENT_REVIEW", 4L, "COMPLIANCE_REVIEW", 5L, "COMPLETED_DD", 6L, "REGISTERED", 7L);
        GenericValue workEffort = delegator.findOne("WorkEffort", UtilMisc.toMap("workEffortId", supplierParty.get("workEffortId")), true);
        if (UtilValidate.isEmpty(workEffort.getString("workEffortParentId")) && workEffort.get("workEffortTypeId").equals("COWORK")){
            return (Long) processBarMap.get(workEffort.getString("currentStatusId"));
        }else if (UtilValidate.isNotEmpty(workEffort.getString("workEffortParentId"))){
            List<GenericValue> parentSuppliers = delegator.findByAnd("WorkEffortAndPartyGroupContact", UtilMisc.toMap("workEffortId", workEffort.getString("workEffortParentId")), null, true);
            GenericValue parentSupplier = EntityUtil.getFirst(parentSuppliers);
            return (Long) processBarMap.get(parentSupplier.getString("currentStatusId"));
        }else {
            return 0L;
        }
    }
}
