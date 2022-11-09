package com.dpbird.test;

import com.dpbird.odata.OdataProcessorHelper;
import com.dpbird.odata.OfbizAppEdmProvider;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.handler.NavigationHandler;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.uri.queryoption.QueryOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @date 2022/11/4
 */
public class DemoFacilityProductHandler implements NavigationHandler {

    @Override
    public List<GenericValue> getNavigationData(Map<String, Object> odataContext, Map<String, Object> linkParam, Map<String, QueryOption> queryOptions) throws OfbizODataException {
        try {
            Delegator delegator = (Delegator) odataContext.get("delegator");
            EntityCondition cond = EntityCondition.makeCondition("productId", EntityOperator.IN, linkParam.get("productIds"));
            return EntityQuery.use(delegator).from("Product").where(cond).queryList();
        } catch (GenericEntityException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

    @Override
    public GenericValue createNavigationData(Map<String, Object> odataContext, OdataOfbizEntity entity, OdataOfbizEntity nestedEntityToCreate, Map<String, QueryOption> queryOptions) {
        return null;
    }

    @Override
    public void deleteNavigationData(Map<String, Object> odataContext, OdataOfbizEntity entity, OdataOfbizEntity nestedEntity) {

    }

    @Override
    public void bindNavigationLink(Map<String, Object> odataContext, OdataOfbizEntity entity, OdataOfbizEntity nestedEntity) {

    }
}
