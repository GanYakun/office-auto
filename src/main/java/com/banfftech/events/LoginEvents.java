package com.banfftech.events;

import com.banfftech.common.util.CommonUtils;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.webapp.webdav.WebDavUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginEvents {
    public static final String module = LoginEvents.class.getName();
    private static final String resource = "SecurityextUiLabels";

    public static String telAndUserLoginPreProcess(HttpServletRequest request, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        GenericValue userLogin;
        Map<String, Object> serviceMap = WebDavUtil.getCredentialsFromRequest(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (serviceMap == null) {
            userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            request.setAttribute("userLogin", userLogin);
            //放入组织或供应商
            GenericValue organization = getOrganization(delegator, userLogin);
            if (UtilValidate.isNotEmpty(organization)) {
                HttpSession httpSession = request.getSession(true);
                request.setAttribute("company", organization);
                httpSession.setAttribute("company", organization);
            }
            return "success";
        }
        String userLoginId = (String) serviceMap.get("login.username");
        if (UtilValidate.isNotEmpty(serviceMap.get("login.username"))) {
            GenericValue partyAndContact = EntityQuery.use(delegator).from("PartyAndContact").where("phoneMobile", serviceMap.get("login.username")).queryFirst();
            if (UtilValidate.isNotEmpty(partyAndContact)){
                GenericValue userLoginEntity = EntityQuery.use(delegator).from("UserLogin").where("partyId", partyAndContact.getString("partyId")).queryFirst();
                if (UtilValidate.isNotEmpty(userLoginEntity)){
                    userLoginId = userLoginEntity.getString("userLoginId");
                }
            }
        }
        serviceMap.put("login.username", userLoginId);
        return preProcessLogin(serviceMap, request);
    }

    private static String preProcessLogin(Map<String, Object> serviceMap, HttpServletRequest request) throws GenericServiceException, GenericEntityException {
        if (UtilValidate.isEmpty(serviceMap.get("login.username"))) {
            String message = UtilProperties.getMessage(resource, "loginevents.username_not_found_reenter", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", message);
            return "error";
        }
        serviceMap.put("locale", UtilHttp.getLocale(request));
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> result = dispatcher.runSync("userLogin", serviceMap);
        if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
            return "error";
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) result.get("userLogin");
        request.setAttribute("userLogin", userLogin);
        HttpSession httpSession = request.getSession(true);
        httpSession.setAttribute("userLogin", userLogin);
        //放入组织或供应商
        GenericValue organization = getOrganization(delegator, userLogin);
        if (UtilValidate.isNotEmpty(organization)) {
            request.setAttribute("company", organization);
            httpSession.setAttribute("company", organization);
        }
        return "success";
    }

    private static GenericValue getOrganization(Delegator delegator, GenericValue userLogin) throws GenericEntityException {
        if (UtilValidate.isNotEmpty(userLogin) && UtilValidate.isNotEmpty(userLogin.getString("partyId"))) {
            String partyId = userLogin.getString("partyId");
            GenericValue partyRole = EntityQuery.use(delegator).from("PartyRole")
                    .where("partyId", partyId, "roleTypeId", "CONTACT").queryOne();
            String partyCompany;
            if (UtilValidate.isNotEmpty(partyRole)) {
                //获取供应商
                GenericValue relationship = EntityQuery.use(delegator).from("PartyRelationship")
                        .where("partyIdTo", partyId, "roleTypeIdFrom", "SUPPLIER").queryFirst();
                partyCompany = relationship.getString("partyIdFrom");
            } else {
                //获取部门
                partyCompany = CommonUtils.getPartyCompany(partyId, delegator);
            }
            if (UtilValidate.isNotEmpty(partyCompany)) {
                return EntityQuery.use(delegator).from("Party").where("partyId", partyCompany).queryOne();
            }
        }
        return null;
    }
}
