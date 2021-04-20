package forms;

import play.data.FormFactory;
import play.mvc.Http.Request;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.RadioButtonQuestionDefinition;
import services.question.types.RepeaterQuestionDefinition;
import services.question.types.TextQuestionDefinition;

public class QuestionFormBuilder {
  public static QuestionForm createFromRequest(
      Request request, FormFactory formFactory, QuestionType questionType)
      throws InvalidQuestionTypeException {
    switch (questionType) {
      case ADDRESS:
        return formFactory.form(AddressQuestionForm.class).bindFromRequest(request).get();
      case CHECKBOX:
        return formFactory.form(CheckboxQuestionForm.class).bindFromRequest(request).get();
      case DROPDOWN:
        return formFactory.form(DropdownQuestionForm.class).bindFromRequest(request).get();
      case FILEUPLOAD:
        return formFactory.form(FileUploadQuestionForm.class).bindFromRequest(request).get();
      case NAME:
        return formFactory.form(NameQuestionForm.class).bindFromRequest(request).get();
      case NUMBER:
        return formFactory.form(NumberQuestionForm.class).bindFromRequest(request).get();
      case RADIO_BUTTON:
        return formFactory.form(RadioButtonQuestionForm.class).bindFromRequest(request).get();
      case REPEATER:
        return formFactory.form(RepeaterQuestionForm.class).bindFromRequest(request).get();
      case TEXT:
        return formFactory.form(TextQuestionForm.class).bindFromRequest(request).get();
      default:
        throw new InvalidQuestionTypeException(questionType.toString());
    }
  }

  public static QuestionForm create(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    switch (questionType) {
      case ADDRESS:
        return new AddressQuestionForm();
      case CHECKBOX:
        return new CheckboxQuestionForm();
      case DROPDOWN:
        return new DropdownQuestionForm();
      case FILEUPLOAD:
        return new FileUploadQuestionForm();
      case NAME:
        return new NameQuestionForm();
      case NUMBER:
        return new NumberQuestionForm();
      case RADIO_BUTTON:
        return new RadioButtonQuestionForm();
      case REPEATER:
        return new RepeaterQuestionForm();
      case TEXT:
        return new TextQuestionForm();
      default:
        throw new UnsupportedQuestionTypeException(questionType);
    }
  }

  public static QuestionForm create(QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException {
    QuestionType questionType = questionDefinition.getQuestionType();
    switch (questionType) {
      case ADDRESS:
        return new AddressQuestionForm((AddressQuestionDefinition) questionDefinition);
      case CHECKBOX:
        return new CheckboxQuestionForm((CheckboxQuestionDefinition) questionDefinition);
      case DROPDOWN:
        return new DropdownQuestionForm((DropdownQuestionDefinition) questionDefinition);
      case FILEUPLOAD:
        return new FileUploadQuestionForm((FileUploadQuestionDefinition) questionDefinition);
      case NAME:
        return new NameQuestionForm((NameQuestionDefinition) questionDefinition);
      case NUMBER:
        return new NumberQuestionForm((NumberQuestionDefinition) questionDefinition);
      case RADIO_BUTTON:
        return new RadioButtonQuestionForm((RadioButtonQuestionDefinition) questionDefinition);
      case REPEATER:
        return new RepeaterQuestionForm((RepeaterQuestionDefinition) questionDefinition);
      case TEXT:
        return new TextQuestionForm((TextQuestionDefinition) questionDefinition);
      default:
        throw new InvalidQuestionTypeException(questionType.toString());
    }
  }
}
