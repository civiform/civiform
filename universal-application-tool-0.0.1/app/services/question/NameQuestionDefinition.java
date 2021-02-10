package services.question;

import com.google.common.collect.ImmutableMap;

public class NameQuestionDefinition extends QuestionDefinition {
    @Override 
    public QuestionType questionType() {
      return QuestionType.NAME;
    }
  
    @Override
    public ImmutableMap[] getScalars() {
        return ImmutableMap.of(
            "title", String.class,
            "first", String.class,
            "middle", String.class,
            "last", String.class,
            "suffix", String.class);
    }
}
