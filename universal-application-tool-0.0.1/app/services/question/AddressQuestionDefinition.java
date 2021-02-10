package services.question;

import com.google.common.collect.ImmutableMap;

public class AddressQuestionDefinition extends QuestionDefinition {
    @Override 
    public QuestionType questionType() {
      return QuestionType.ADDRESS;
    }
     
    @Override
    public ImmutableMap<String, Class> getScalars() {
        return ImmutableMap.of(
            "street", String.class,
            "unit", String.class,
            "city", String.class,
            "state", String.class,
            "zip", String.class);
    }
}
