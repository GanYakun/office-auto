import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc;

import org.apache.ofbiz.base.util.UtilValidate;

import java.util.Map;

module = "purchaseManage.OrderHeader.generateEntityListFields.groovy";
def generateFields(Map<String, Object> context) {
    List<Entity> entityList = context.parameters.get("entityList");
    String response = null
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue surveyQuestionAppl = odataOfbizEntity.getGenericValue();
        if(UtilValidate.isNotEmpty(surveyQuestionAppl)){
            GenericValue surveyQuestionAnswer = delegator.findOne("SurveyQuestionAnswer",UtilMisc.toMap("surveyQuestionId", surveyQuestionAppl.getString("surveyQuestionId")), true);
            if(UtilValidate.isNotEmpty(surveyQuestionAnswer.get("currencyResponse"))){
                response = (String) surveyQuestionAnswer.get("currencyResponse") + "CNY"
            }else if (UtilValidate.isNotEmpty(surveyQuestionAnswer.get("floatResponse"))){
                response = (String) surveyQuestionAnswer.get("floatResponse")
            }else if (UtilValidate.isNotEmpty(surveyQuestionAnswer.get("numericResponse"))){
                response = (String) surveyQuestionAnswer.get("numericResponse")
            }else if (UtilValidate.isNotEmpty(surveyQuestionAnswer.get("textResponse"))){
                response = (String) surveyQuestionAnswer.get("textResponse")
            }else if (UtilValidate.isNotEmpty(surveyQuestionAnswer.get("booleanResponse"))){
                String booleanResponse = (String) surveyQuestionAnswer.get("booleanResponse")
                if (booleanResponse.equals("Y")) {
                    response = "yes"
                }else {
                    response = "no"
                }
            }

        }
        entity.addProperty(new Property(null, "response", ValueType.PRIMITIVE, response));
    }
    return entityList;
}
