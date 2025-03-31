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
    return switch (questionType) {
      case ADDRESS -> formFactory.form(AddressQuestionForm.class).bindFromRequest(request).get();
      case CHECKBOX -> formFactory.form(CheckboxQuestionForm.class).bindFromRequest(request).get();
      case CURRENCY -> formFactory.form(CurrencyQuestionForm.class).bindFromRequest(request).get();
      case DATE -> formFactory.form(DateQuestionForm.class).bindFromRequest(request).get();
      case DROPDOWN -> formFactory.form(DropdownQuestionForm.class).bindFromRequest(request).get();
      case EMAIL -> formFactory.form(EmailQuestionForm.class).bindFromRequest(request).get();
      case FILEUPLOAD ->
          formFactory.form(FileUploadQuestionForm.class).bindFromRequest(request).get();
      case ID -> formFactory.form(IdQuestionForm.class).bindFromRequest(request).get();
      case NAME -> formFactory.form(NameQuestionForm.class).bindFromRequest(request).get();
      case NUMBER -> formFactory.form(NumberQuestionForm.class).bindFromRequest(request).get();
      case RADIO_BUTTON ->
          formFactory.form(RadioButtonQuestionForm.class).bindFromRequest(request).get();
      case ENUMERATOR ->
          formFactory.form(EnumeratorQuestionForm.class).bindFromRequest(request).get();
      case STATIC ->
          formFactory.form(StaticContentQuestionForm.class).bindFromRequest(request).get();
      case TEXT -> formFactory.form(TextQuestionForm.class).bindFromRequest(request).get();
      case PHONE -> formFactory.form(PhoneQuestionForm.class).bindFromRequest(request).get();
      default -> throw new InvalidQuestionTypeException(questionType.toString());
    };
  }

  public static QuestionForm create(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    return switch (questionType) {
      case ADDRESS -> new AddressQuestionForm();
      case CHECKBOX -> new CheckboxQuestionForm();
      case CURRENCY -> new CurrencyQuestionForm();
      case DATE -> new DateQuestionForm();
      case DROPDOWN -> new DropdownQuestionForm();
      case EMAIL -> new EmailQuestionForm();
      case FILEUPLOAD -> new FileUploadQuestionForm();
      case ID -> new IdQuestionForm();
      case NAME -> new NameQuestionForm();
      case NUMBER -> new NumberQuestionForm();
      case RADIO_BUTTON -> new RadioButtonQuestionForm();
      case ENUMERATOR -> new EnumeratorQuestionForm();
      case STATIC -> new StaticContentQuestionForm();
      case TEXT -> new TextQuestionForm();
      case PHONE -> new PhoneQuestionForm();
      default -> throw new UnsupportedQuestionTypeException(questionType);
    };
  }

  public static QuestionForm create(QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException {
    QuestionType questionType = questionDefinition.getQuestionType();
    return switch (questionType) {
      case ADDRESS -> new AddressQuestionForm((AddressQuestionDefinition) questionDefinition);
      case CHECKBOX -> new CheckboxQuestionForm((MultiOptionQuestionDefinition) questionDefinition);
      case CURRENCY -> new CurrencyQuestionForm((CurrencyQuestionDefinition) questionDefinition);
      case DATE -> new DateQuestionForm((DateQuestionDefinition) questionDefinition);
      case DROPDOWN -> new DropdownQuestionForm((MultiOptionQuestionDefinition) questionDefinition);
      case EMAIL -> new EmailQuestionForm((EmailQuestionDefinition) questionDefinition);
      case FILEUPLOAD ->
          new FileUploadQuestionForm((FileUploadQuestionDefinition) questionDefinition);
      case ID -> new IdQuestionForm((IdQuestionDefinition) questionDefinition);
      case NAME -> new NameQuestionForm((NameQuestionDefinition) questionDefinition);
      case NUMBER -> new NumberQuestionForm((NumberQuestionDefinition) questionDefinition);
      case RADIO_BUTTON ->
          new RadioButtonQuestionForm((MultiOptionQuestionDefinition) questionDefinition);
      case ENUMERATOR ->
          new EnumeratorQuestionForm((EnumeratorQuestionDefinition) questionDefinition);
      case STATIC ->
          new StaticContentQuestionForm((StaticContentQuestionDefinition) questionDefinition);
      case TEXT -> new TextQuestionForm((TextQuestionDefinition) questionDefinition);
      case PHONE -> new PhoneQuestionForm((PhoneQuestionDefinition) questionDefinition);
      default -> throw new InvalidQuestionTypeException(questionType.toString());
    };
  }
}
