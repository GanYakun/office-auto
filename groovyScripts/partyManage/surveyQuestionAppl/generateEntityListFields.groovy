import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.data.Entity
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.util.EntityQuery;

import org.apache.ofbiz.base.util.UtilValidate;

import java.util.Map;

module = "purchaseManage.OrderHeader.generateEntityListFields.groovy";
def generateFields(Map<String, Object> context) {
    List<Entity> entityList = context.parameters.get("entityList");
    String response = null
    entityList.each { entity ->
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) entity;
        GenericValue surveyQuestionAndAnswer = odataOfbizEntity.getGenericValue();
        GenericValue answer = EntityQuery.use(delegator).from("SurveyQuestionAndAnswer").where(UtilMisc.toMap("surveyQuestionId", surveyQuestionAndAnswer.getString("surveyQuestionId"),
                "surveyQuestionAnswerId", surveyQuestionAndAnswer.getString("surveyQuestionAnswerId"))).queryFirst();
        if(UtilValidate.isNotEmpty(answer)){
            String surveyQuestionTypeId = answer.getString("surveyQuestionTypeId")
            if(surveyQuestionTypeId.equals("NUMBER_CURRENCY")){
                response = (String) answer.get("currencyResponse") + "CNY"
            }else if (surveyQuestionTypeId.equals("NUMBER_FLOAT")){
                response = (String) answer.get("floatResponse")
            }else if (surveyQuestionTypeId.equals("NUMBER_LONG")){
                response = (String) answer.get("numericResponse")
            }else if (surveyQuestionTypeId.equals("TEXT_LONG")){
                response = (String) answer.get("textResponse")
            }else if (surveyQuestionTypeId.equals("BOOLEAN")){
                String booleanResponse = (String) answer.get("booleanResponse")
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
