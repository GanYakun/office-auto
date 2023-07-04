package com.dpbird.odata.login;

import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
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

/**
 * @author zhuwenchao
 * externalLoginKey login
 */
public class LoginEvents {
    public static final String module = LoginEvents.class.getName();
    private static Map<String, GenericValue> externalLoginKeys = new ConcurrentHashMap<String, GenericValue>();

    public static String login(HttpServletRequest request, HttpServletResponse response) {
        String loginResult = LoginWorker.login(request, response);
        if ("success".equals(loginResult)) {
            // fix for upgrade，looks like nothing should do here
            // getExternalLoginKey(request);
        }
        return loginResult;
    }

    public static String logInUser(HttpServletRequest request, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin;
        Map<String, Object> serviceMap = WebDavUtil.getCredentialsFromRequest(request);
        HttpSession httpSession = request.getSession(true);
        if (serviceMap == null) {
            userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            request.setAttribute("userLogin", userLogin);
            return "success";
        } else if (UtilValidate.isNotEmpty(serviceMap.get("login.username"))) {
            //externalId
            String externalId = (String) serviceMap.get("login.username");
            GenericValue party = EntityQuery.use(delegator).from("Party").where("externalId", externalId).queryFirst();
            if (UtilValidate.isEmpty(party)) {
                return "error";
            }
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("partyId", party.getString("partyId")).queryFirst();
            if (UtilValidate.isEmpty(userLogin)) {
                return "error";
            }
            serviceMap.put("login.username", userLogin.getString("userLoginId"));
        }
        serviceMap.put("locale", UtilHttp.getLocale(request));
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> result = dispatcher.runSync("userLogin", serviceMap);
        if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
            return "error";
        }
        userLogin = (GenericValue) result.get("userLogin");
        request.setAttribute("userLogin", userLogin);
        httpSession.setAttribute("userLogin", userLogin);
        //添加组织机构信息到session
        GenericValue organization = getOrganization(delegator, userLogin);
        if (UtilValidate.isNotEmpty(organization)) {
            request.setAttribute("company", organization);
            httpSession.setAttribute("company", organization);
        }
        return "success";
    }

    private static GenericValue getOrganization(Delegator delegator, GenericValue userLogin) throws GenericEntityException {
        String partyId = userLogin.getString("partyId");
        GenericValue organizationRelation = EntityQuery.use(delegator).from("PartyRelationship")
                .where("roleTypeIdFrom", "ORGANIZATION_UNIT", "partyIdTo", partyId).queryFirst();
        if (UtilValidate.isNotEmpty(organizationRelation)) {
            return organizationRelation.getRelatedOne("FromParty", false);
        }
        return null;
    }


    public static String extensionCheckLogin(HttpServletRequest request, HttpServletResponse response) {
        return checkLogin(request, response);
    }

    public static String checkLogin(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        if (userLogin == null) {
            response.setStatus(401);
            return "error";
        }
        return "success";
    }

}
