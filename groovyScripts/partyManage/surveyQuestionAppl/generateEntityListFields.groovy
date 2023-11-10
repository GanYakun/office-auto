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
            GenericValue surveyQuestion = delegator.findOne("SurveyQuestion",UtilMisc.toMap("surveyQuestionId", surveyQuestionAppl.getString("surveyQuestionId")), true);
            String surveyQuestionTypeId = surveyQuestion.getString("surveyQuestionTypeId")
            if(surveyQuestionTypeId.equals("NUMBER_CURRENCY")){
                response = (String) surveyQuestionAnswer.get("currencyResponse") + "CNY"
            }else if (surveyQuestionTypeId.equals("NUMBER_FLOAT")){
                response = (String) surveyQuestionAnswer.get("floatResponse")
            }else if (surveyQuestionTypeId.equals("NUMBER_LONG")){
                response = (String) surveyQuestionAnswer.get("numericResponse")
            }else if (surveyQuestionTypeId.equals("TEXT_LONG")){
                response = (String) surveyQuestionAnswer.get("textResponse")
            }else if (surveyQuestionTypeId.equals("BOOLEAN")){
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
