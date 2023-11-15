package services;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import models.LifecycleStage;
import models.Question;
import repository.ResetPostgres;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

/** A Builder to build a fake multiOptionQuestion */
public class MultiOptionQuestionBuilder extends ResetPostgres {
  private List<String> options;
  private String questionName;
  private LifecycleStage stage;

  public MultiOptionQuestionBuilder addOption(String option) {
    options.add(option);
    return this;
  }

  public MultiOptionQuestionBuilder() {
    options = new ArrayList<>();
  }

  public MultiOptionQuestionBuilder withLifeCycleStage(LifecycleStage stage) {
    this.stage = stage;
    return this;
  }

  public MultiOptionQuestionBuilder withName(String name) {
    this.questionName = name;
    return this;
  }

  public Question build() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName(questionName)
            .setDescription(questionName)
            .setQuestionText(LocalizedStrings.of(Locale.US, questionName))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .build();
    Long order = 1L;
    ImmutableList.Builder<QuestionOption> optionList = new ImmutableList.Builder<>();
    for (String option : options) {
      optionList.add(
          QuestionOption.create(order, order, option, LocalizedStrings.of(Locale.US, option)));
      order++;
    }
    ImmutableList<QuestionOption> questionOptions = optionList.build();

    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config,
            questionOptions,
            MultiOptionQuestionDefinition.MultiOptionQuestionType.CHECKBOX);
    return testQuestionBank.maybeSave(definition, stage);
  }
}
