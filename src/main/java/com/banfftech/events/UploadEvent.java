package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.OdataParts;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import com.dpbird.odata.edm.ParameterContext;
import org.apache.fop.util.ListUtil;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.tools.mail.ErrorInQuitException;

import javax.resource.spi.work.Work;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadEvent {
    public static final String module = UploadEvent.class.getName();

    public static void uploadFile(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        ParameterContext parameterContext = (ParameterContext) actionParameters.get("file");
        List<OdataParts> odataParts = UtilGenerics.checkList(oDataContext.get("odataParts"));
        OdataOfbizEntity supplierEntity = CommonUtils.getOdataPartByEntityType(oDataContext, "SupplierParty");
        if (UtilValidate.isEmpty(supplierEntity)) {
            throw new OfbizODataException("Parameter error");
        }
        Map<String, Object> serviceParam = new HashMap<>();
        serviceParam.put("otherData", parameterContext.getFile());
        serviceParam.put("contentName", parameterContext.getFileName());
        serviceParam.put("mimeTypeId", parameterContext.getFileMimeType());
        serviceParam.put("createdDate", UtilDateTime.nowTimestamp());
        serviceParam.put("userLogin", userLogin);
        //创建文件
        Map<String, Object> createResult = dispatcher.runSync("banfftech.createContentAndMediaDataResource", serviceParam);
        Debug.logInfo("Upload Result: " + createResult, module);
        //绑定到当前对象
        String workEffortId = (String) supplierEntity.getPropertyValue("workEffortId");
        String segmentValue = ListUtil.getLast(odataParts).getUriResource().getSegmentValue();
        String contentType = segmentValue.equals("FinancialStatements") ? "FINANCIAL_STATEMENTS" :
                segmentValue.equals("ConfidentialityAgreement") ? "CONFIDENTIALITY_AGREEMENT" : "OTHER_FILES";
        Map<String, Object> boundResult = dispatcher.runSync("banfftech.createWorkEffortContent",
                UtilMisc.toMap("workEffortId",workEffortId, "contentId",createResult.get("contentId"),
                        "workEffortContentTypeId", contentType,"userLogin", userLogin));
        Debug.logInfo("Bound Result: " + boundResult, module);
    }


}
