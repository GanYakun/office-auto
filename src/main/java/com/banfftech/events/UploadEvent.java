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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tools.mail.ErrorInQuitException;

import javax.resource.spi.work.Work;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadEvent {
    public static final String module = UploadEvent.class.getName();

    public static void uploadFile(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        ParameterContext parameterContext = (ParameterContext) actionParameters.get("file");
        OdataOfbizEntity partyMediaResource = (OdataOfbizEntity) actionParameters.get("partyMediaResource");
        Map<String, Object> serviceParam = new HashMap<>();
        serviceParam.put("contentId", partyMediaResource.getPropertyValue("contentId"));
        serviceParam.put("dataResourceId", partyMediaResource.getPropertyValue("dataResourceId"));
        serviceParam.put("partyContentId", partyMediaResource.getPropertyValue("partyContentId"));
        serviceParam.put("fromDate", UtilDateTime.nowTimestamp());
        serviceParam.put("otherData", parameterContext.getFile());
        serviceParam.put("dataResourceName", parameterContext.getFileName());
        serviceParam.put("mimeTypeId", parameterContext.getFileMimeType());
        serviceParam.put("userLogin", userLogin);
        //创建文件
        Map<String, Object> createResult = dispatcher.runSync("banfftech.updatePartyMediaResource", serviceParam);
        Debug.logInfo("Upload Result: " + createResult, module);
    }


    /**
     * 传递excel 导入其中的产品并关联到当前的供应商
     */
    public static void importProduct(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        ParameterContext parameterContext = (ParameterContext) actionParameters.get("file");

        String supplierPartyId = (String) supplierParty.getPropertyValue("partyId");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(parameterContext.getFile().array()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // 遍历每一行
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                // 遍历每个单元格
                String productName = null, productType = null, unitName = null, categoryName = null;
                for (Cell cell : row) {
                    // 获取单元格的值
                    String cellValue = cell.toString();
                    if (UtilValidate.isNotEmpty(cellValue)) {
                        if (cell.getColumnIndex() == 1) productName = cellValue;
                        if (cell.getColumnIndex() == 4) unitName = cellValue;
//                        if (cell.getColumnIndex() == 5) price = new BigDecimal(cellValue);
                        if (cell.getColumnIndex() == 6) productType = cellValue;
                        if (cell.getColumnIndex() == 8) categoryName = cellValue;
                    }
                }
                if (UtilValidate.isNotEmpty(productName)) {
                    createAndJoinProduct(delegator, supplierPartyId, productName, productType, unitName, categoryName);
                }
            }
        } catch (IOException | GenericEntityException e) {
            e.printStackTrace();
            throw new OfbizODataException("Import Product Error:" + e.getMessage());
        }
    }

    private static void createAndJoinProduct(Delegator delegator, String supplierPartyId, String productName, String productTypeId,
                                             String unit, String categoryName) throws GenericEntityException {
        String categoryId = null;
        String uomId = null;
        //create category
        if (UtilValidate.isNotEmpty(categoryName)) {
            GenericValue category = EntityQuery.use(delegator).from("ProductCategory").where("categoryName", categoryName).queryFirst();
            if (UtilValidate.isEmpty(category)) {
                categoryId = delegator.getNextSeqId("ProductCategory");
                delegator.create("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId, "categoryName", categoryName));
            } else {
                categoryId = category.getString("productCategoryId");
            }
        }
        //create quantity uom
        if (UtilValidate.isNotEmpty(unit)) {
            GenericValue uom = EntityQuery.use(delegator).from("Uom").where("description", unit).queryFirst();
            if (UtilValidate.isEmpty(uom)) {
                uomId = delegator.getNextSeqId("Uom");
                delegator.create("Uom", UtilMisc.toMap("uomId", uomId, "description", unit));
            } else {
                uomId = uom.getString("uomId");
            }
        }
        //create productType
        if (UtilValidate.isNotEmpty(productTypeId)) {
            GenericValue productType = EntityQuery.use(delegator).from("ProductType").where("productTypeId", productTypeId).queryFirst();
            if (UtilValidate.isEmpty(productType)) {
                delegator.create("ProductType", UtilMisc.toMap("productTypeId", productTypeId, "description", productTypeId));
            }
        }
        String productId;
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productName", productName).queryFirst();
        if (UtilValidate.isEmpty(product)) {
            productId = delegator.getNextSeqId("Product");
            delegator.create("Product", UtilMisc.toMap("productId", productId, "productName", productName,
                    "productTypeId", productTypeId, "quantityUomId", uomId, "primaryProductCategoryId", categoryId));
        } else {
            productId = product.getString("productId");
            product.putAll(UtilMisc.toMap("productTypeId", productTypeId, "quantityUomId", uomId, "primaryProductCategoryId", categoryId));
            product.store();
        }
        //create supplierProduct
        GenericValue supplierProduct = EntityQuery.use(delegator).from("SupplierProduct").where("productId", productId, "partyId", supplierPartyId).queryFirst();
        if (UtilValidate.isEmpty(supplierProduct)) {
            delegator.create("SupplierProduct", UtilMisc.toMap("productId", productId, "partyId", supplierPartyId,
                    "availableFromDate", UtilDateTime.nowTimestamp(), "minimumOrderQuantity", BigDecimal.ZERO, "currencyUomId", UtilValidate.isEmpty(uomId) ? "_NA_" : uomId));
        }
    }

    public static void uploadSupplierLogo(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget)
            throws GenericEntityException, OfbizODataException, GeneralServiceException, GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        ParameterContext parameterContext = (ParameterContext) actionParameters.get("file");
        OdataOfbizEntity supplierParty = (OdataOfbizEntity) actionParameters.get("supplierParty");
        Map<String, Object> resultMap = dispatcher.runSync("banfftech.createPartyMediaResource",
                UtilMisc.toMap("contentName", "Supplier Logo", "dataResourceName", "Supplier Logo", "imageData", parameterContext.getFile(),
                        "mimeTypeId", parameterContext.getFileMimeType(), "partyContentTypeId",
                        "SUPPLIER_LOGO", "partyId", supplierParty.getPropertyValue("partyId"), "userLogin", userLogin));

        Map<String, Object> serviceParam = new HashMap<>();
        serviceParam.put("partyId", supplierParty.getPropertyValue("partyId"));
        serviceParam.put("logoImageUrl", "/officeauto/control/odataAppSvc/supplierApproveService/PartyMediaResources('" + resultMap.get("partyContentId") + "')/imageData");
        serviceParam.put("userLogin", userLogin);
        //创建文件
        Map<String, Object> createResult = dispatcher.runSync("banfftech.updatePartyGroup", serviceParam);
        Debug.logInfo("Upload Result: " + createResult, module);
    }

}
