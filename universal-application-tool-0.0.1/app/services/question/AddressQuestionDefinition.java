package services.question;

import com.google.common.collect.ImmutableMap;

public class AddressQuestionDefinition extends QuestionDefinition {
    @Override 
    public QuestionType questionType() {
      return QuestionType.ADDRESS;
    }
     
    @Override
    public ImmutableMap[] getScalars() {
        return ImmutableMap.of(
            "street", String,
            "unit", String,
            "city", String,
            "state", String,
            "zip", String);
    }
}
