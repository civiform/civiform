package forms;

import play.data.FormFactory;
import play.mvc.Http.Request;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.IdQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;

/** This class helps create question forms for various question types. */
public final class QuestionFormBuilder {
  public static QuestionForm createFromRequest(
      Request request, FormFactory formFactory, QuestionType questionType)
      throws InvalidQuestionTypeException {
    switch (questionType) {
      case ADDRESS:
        return formFactory.form(AddressQuestionForm.class).bindFromRequest(request).get();
      case CHECKBOX:
        return formFactory.form(CheckboxQuestionForm.class).bindFromRequest(request).get();
      case CURRENCY:
        return formFactory.form(CurrencyQuestionForm.class).bindFromRequest(request).get();
      case DATE:
        return formFactory.form(DateQuestionForm.class).bindFromRequest(request).get();
      case DROPDOWN:
        return formFactory.form(DropdownQuestionForm.class).bindFromRequest(request).get();
      case EMAIL:
        return formFactory.form(EmailQuestionForm.class).bindFromRequest(request).get();
      case FILEUPLOAD:
        return formFactory.form(FileUploadQuestionForm.class).bindFromRequest(request).get();
      case ID:
        return formFactory.form(IdQuestionForm.class).bindFromRequest(request).get();
      case NAME:
        return formFactory.form(NameQuestionForm.class).bindFromRequest(request).get();
      case NUMBER:
        return formFactory.form(NumberQuestionForm.class).bindFromRequest(request).get();
      case RADIO_BUTTON:
        return formFactory.form(RadioButtonQuestionForm.class).bindFromRequest(request).get();
      case ENUMERATOR:
        return formFactory.form(EnumeratorQuestionForm.class).bindFromRequest(request).get();
      case STATIC:
        return formFactory.form(StaticContentQuestionForm.class).bindFromRequest(request).get();
      case TEXT:
        return formFactory.form(TextQuestionForm.class).bindFromRequest(request).get();
      case PHONE:
        return formFactory.form(PhoneQuestionForm.class).bindFromRequest(request).get();
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
      case CURRENCY:
        return new CurrencyQuestionForm();
      case DATE:
        return new DateQuestionForm();
      case DROPDOWN:
        return new DropdownQuestionForm();
      case EMAIL:
        return new EmailQuestionForm();
      case FILEUPLOAD:
        return new FileUploadQuestionForm();
      case ID:
        return new IdQuestionForm();
      case NAME:
        return new NameQuestionForm();
      case NUMBER:
        return new NumberQuestionForm();
      case RADIO_BUTTON:
        return new RadioButtonQuestionForm();
      case ENUMERATOR:
        return new EnumeratorQuestionForm();
      case STATIC:
        return new StaticContentQuestionForm();
      case TEXT:
        return new TextQuestionForm();
      case PHONE:
        return new PhoneQuestionForm();
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
        return new CheckboxQuestionForm((MultiOptionQuestionDefinition) questionDefinition);
      case CURRENCY:
        return new CurrencyQuestionForm((CurrencyQuestionDefinition) questionDefinition);
      case DATE:
        return new DateQuestionForm((DateQuestionDefinition) questionDefinition);
      case DROPDOWN:
        return new DropdownQuestionForm((MultiOptionQuestionDefinition) questionDefinition);
      case EMAIL:
        return new EmailQuestionForm((EmailQuestionDefinition) questionDefinition);
      case FILEUPLOAD:
        return new FileUploadQuestionForm((FileUploadQuestionDefinition) questionDefinition);
      case ID:
        return new IdQuestionForm((IdQuestionDefinition) questionDefinition);
      case NAME:
        return new NameQuestionForm((NameQuestionDefinition) questionDefinition);
      case NUMBER:
        return new NumberQuestionForm((NumberQuestionDefinition) questionDefinition);
      case RADIO_BUTTON:
        return new RadioButtonQuestionForm((MultiOptionQuestionDefinition) questionDefinition);
      case ENUMERATOR:
        return new EnumeratorQuestionForm((EnumeratorQuestionDefinition) questionDefinition);
      case STATIC:
        return new StaticContentQuestionForm((StaticContentQuestionDefinition) questionDefinition);
      case TEXT:
        return new TextQuestionForm((TextQuestionDefinition) questionDefinition);
      case PHONE:
        return new PhoneQuestionForm((PhoneQuestionDefinition) questionDefinition);
      default:
        throw new InvalidQuestionTypeException(questionType.toString());
    }
  }
}
