package com.banfftech.events;

import com.dpbird.odata.OdataParts;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import java.util.List;
import java.util.Map;

public class SupplierEvents {
    public static Object createPartyClassificationGroup(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                       EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
            int odataPartsListSize = odataPartsList.size();
            GenericValue supplierParty = null;
            OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
            OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
            supplierParty = supplierPartyEntity.getGenericValue();
            if (supplierParty == null) {
                throw new OfbizODataException("Account is invalid");
            }
            String partyId = (String) supplierParty.get("partyId");

            String partyClassificationTypeId = (String) actionParameters.get("partyClassificationTypeId");
            String description = (String) actionParameters.get("description");
            Map<String, Object> result = dispatcher.runSync("banfftech.createPartyClassificationGroup",
                    UtilMisc.toMap("partyClassificationTypeId", partyClassificationTypeId,
                            "description", description, "userLogin", userLogin));
            String partyClassificationGroupId = (String) result.get("partyClassificationGroupId");
            dispatcher.runSync("banfftech.createPartyClassification", UtilMisc.toMap("userLogin", userLogin,
                    "partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId, "fromDate", UtilDateTime.nowTimestamp()));
            return delegator.findOne("PartyClassificationGroup", true, "partyClassificationGroupId", partyClassificationGroupId);
        } catch (GenericEntityException | GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

    public static Object updatePartyClassificationGroup(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                                        EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            String partyClassificationTypeId = (String) actionParameters.get("partyClassificationTypeId");
            String description = (String) actionParameters.get("description");
            OdataOfbizEntity partyClassificationGroupEntity = (OdataOfbizEntity) actionParameters.get("partyClassificationGroup");
            GenericValue partyClassificationGroup = partyClassificationGroupEntity.getGenericValue();
            Map<String, Object> result = dispatcher.runSync("banfftech.updatePartyClassificationGroup",
                    UtilMisc.toMap("partyClassificationTypeId", partyClassificationTypeId, "partyClassificationGroupId", partyClassificationGroup.get("partyClassificationGroupId"),
                            "description", description, "userLogin", userLogin));
            String partyClassificationGroupId = (String) result.get("partyClassificationGroupId");
            return delegator.findOne("PartyClassificationGroup", true, "partyClassificationGroupId", partyClassificationGroupId);
        } catch (GenericEntityException | GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

    public static Object createPersonAndContact(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                                        EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        try {
            List<OdataParts> odataPartsList = (List<OdataParts>) oDataContext.get("odataParts");
            int odataPartsListSize = odataPartsList.size();
            GenericValue supplierParty = null;
            OdataParts odataPartsOne = odataPartsList.get(odataPartsListSize - 2);
            OdataOfbizEntity supplierPartyEntity = (OdataOfbizEntity) odataPartsOne.getEntityData();
            supplierParty = supplierPartyEntity.getGenericValue();
            if (supplierParty == null) {
                throw new OfbizODataException("Account is invalid");
            }
            String partyId = (String) supplierParty.get("partyId");

            String partyName = (String) actionParameters.get("partyName");
            String primaryPhone = (String) actionParameters.get("primaryPhone");
            String phoneMobile = (String) actionParameters.get("phoneMobile");
            String primaryEmail = (String) actionParameters.get("primaryEmail");
            String position = (String) actionParameters.get("position");
            Map<String, Object> result = dispatcher.runSync("banfftech.createPersonAndContact",
                    UtilMisc.toMap("partyName", partyName, "primaryPhone", primaryPhone, "phoneMobile", phoneMobile,
                            "primaryEmail", primaryEmail, "userLogin", userLogin));
            String contactPartyId = (String) result.get("partyId");
            dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "roleTypeId", "CONTACT"));
            dispatcher.runSync("banfftech.createPartyAttribute", UtilMisc.toMap("userLogin", userLogin, "partyId", contactPartyId, "attrName", "position", "attrValue", position));
            dispatcher.runSync("banfftech.createPartyRelationship", UtilMisc.toMap("partyIdFrom", partyId, "roleTypeIdFrom", "VENDOR",
                    "partyIdTo", contactPartyId, "roleTypeIdTo", "CONTACT", "fromDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));
            return delegator.findOne("PersonAndContact", true, "partyId", contactPartyId);
        } catch (GenericEntityException | GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }
}
