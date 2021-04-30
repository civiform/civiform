package forms.translation;

import java.util.ArrayList;
import java.util.List;

public class MultiOptionQuestionTranslationForm extends QuestionTranslationForm {

  private List<String> options;

  protected MultiOptionQuestionTranslationForm() {
    super();
    this.options = new ArrayList<>();
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }
}
