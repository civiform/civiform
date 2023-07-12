package controllers.dev.seeding;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import play.i18n.Lang;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.IdQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.NameQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;

public final class SampleQuestionDefinitions {

  public static ImmutableList<QuestionDefinition> getAllSampleQuestionDefinitions() {
    ImmutableList.Builder<QuestionDefinition> questionDefinitions = ImmutableList.builder();
    Field[] fields = SampleQuestionDefinitions.class.getFields();

    for (Field field : fields) {
      if (QuestionDefinition.class.isAssignableFrom(field.getType())) {
        try {
          QuestionDefinition definition = (QuestionDefinition) field.get(null);
          questionDefinitions.add(definition);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Unable to access field: " + field.getName(), e);
        }
      }
    }

    return questionDefinitions.build();
  }

  public static final AddressQuestionDefinition ADDRESS_QUESTION_DEFINITION =
      new AddressQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Address Question")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.withDefaultValue("What is your address?"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  private static final QuestionDefinitionConfig CHECKBOX_QUESTION_DEFINITION_CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("Sample Checkbox Question")
          .setDescription("description")
          .setQuestionText(
              LocalizedStrings.withDefaultValue(
                  "Which of the following kitchen instruments do you own?"))
          .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
          .build();

  private static final ImmutableList<QuestionOption> CHECKBOX_QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(1L, 1L, LocalizedStrings.withDefaultValue("toaster")),
          QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("pepper grinder")),
          QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("garlic press")));

  public static final MultiOptionQuestionDefinition CHECKBOX_QUESTION_DEFINITION =
      new MultiOptionQuestionDefinition(
          CHECKBOX_QUESTION_DEFINITION_CONFIG,
          CHECKBOX_QUESTION_OPTIONS,
          MultiOptionQuestionType.CHECKBOX);

  public static final CurrencyQuestionDefinition CURRENCY_QUESTION_DEFINITION =
      new CurrencyQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Currency Question")
              .setDescription("description")
              .setQuestionText(
                  LocalizedStrings.withDefaultValue("How much should a scoop of ice cream cost?"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  private static final QuestionDefinitionConfig DROPDOWN_QUESTION_CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("Sample Dropdown Question")
          .setDescription("select your favorite ice cream flavor")
          .setQuestionText(
              LocalizedStrings.withDefaultValue(
                  "Select your favorite ice cream flavor from the following"))
          .setQuestionHelpText(LocalizedStrings.withDefaultValue("this is sample help text"))
          .build();
  private static final ImmutableList<QuestionOption> DROPDOWN_QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(1L, 1L, LocalizedStrings.withDefaultValue("chocolate")),
          QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("strawberry")),
          QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("vanilla")),
          QuestionOption.create(4L, 4L, LocalizedStrings.withDefaultValue("coffee")));

  public static final MultiOptionQuestionDefinition DROPDOWN_QUESTION_DEFINITION =
      new MultiOptionQuestionDefinition(
          DROPDOWN_QUESTION_CONFIG, DROPDOWN_QUESTION_OPTIONS, MultiOptionQuestionType.DROPDOWN);

  public static final EmailQuestionDefinition EMAIL_QUESTION_DEFINITION =
      new EmailQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Email Question")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.withDefaultValue("What is your email?"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static final EnumeratorQuestionDefinition ENUMERATOR_QUESTION_DEFINITION =
      new EnumeratorQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Enumerator Question")
              .setDescription("description")
              .setQuestionText(
                  LocalizedStrings.withDefaultValue("List all members of your household."))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build(),
          LocalizedStrings.withDefaultValue("household member"));

  public static final FileUploadQuestionDefinition FILE_UPLOAD_QUESTION_DEFINITION =
      new FileUploadQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample File Upload Question")
              .setDescription("description")
              .setQuestionText(
                  LocalizedStrings.withDefaultValue("Upload anything from your computer"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static final IdQuestionDefinition ID_QUESTION_DEFINITION =
      new IdQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample ID Question")
              .setDescription("description")
              .setQuestionText(
                  LocalizedStrings.withDefaultValue("What is your driver's license ID?"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static final QuestionDefinition NAME_QUESTION_DEFINITION =
      new NameQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Name")
              .setDescription("The applicant's name")
              .setQuestionText(
                  LocalizedStrings.of(
                      ImmutableMap.of(
                          Lang.forCode("am").toLocale(),
                          "ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)",
                          Lang.forCode("ko").toLocale(),
                          "성함 (이름 및 성의 경우 이니셜도 괜찮음)",
                          Lang.forCode("so").toLocale(),
                          "Magaca (magaca koowaad iyo kan dambe okay)",
                          Lang.forCode("lo").toLocale(),
                          "ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)",
                          Lang.forCode("tl").toLocale(),
                          "Pangalan (unang pangalan at ang unang titik ng apilyedo ay okay)",
                          Lang.forCode("vi").toLocale(),
                          "Tên (tên và họ viết tắt đều được)",
                          Lang.forCode("en-US").toLocale(),
                          "Please enter your first and last name",
                          Lang.forCode("es-US").toLocale(),
                          "Nombre (nombre y la inicial del apellido está bien)",
                          Lang.forCode("zh-TW").toLocale(),
                          "姓名（名字和姓氏第一個字母便可）")))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static final DateQuestionDefinition DATE_QUESTION_DEFINITION =
      new DateQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Applicant Date of Birth")
              .setDescription("Applicant's date of birth")
              .setQuestionText(
                  LocalizedStrings.of(
                      Lang.forCode("en-US").toLocale(),
                      "Please enter your date of birth in the format mm/dd/yyyy"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static final NumberQuestionDefinition NUMBER_QUESTION_DEFINITION =
      new NumberQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Number Question")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.withDefaultValue("How many pets do you have?"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  private static final QuestionDefinitionConfig RADIO_BUTTON_QUESTION_CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("Sample Radio Button Question")
          .setDescription("favorite season in the year")
          .setQuestionText(LocalizedStrings.withDefaultValue("What is your favorite season?"))
          .setQuestionHelpText(LocalizedStrings.withDefaultValue("this is sample help text"))
          .build();

  private static final ImmutableList<QuestionOption> RADIO_BUTTON_QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(
              1L, 1L, LocalizedStrings.withDefaultValue("winter (will hide next block)")),
          QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("spring")),
          QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("summer")),
          QuestionOption.create(
              4L, 4L, LocalizedStrings.withDefaultValue("fall (will hide next block)")));

  public static final MultiOptionQuestionDefinition RADIO_BUTTON_QUESTION_DEFINITION =
      new MultiOptionQuestionDefinition(
          RADIO_BUTTON_QUESTION_CONFIG,
          RADIO_BUTTON_QUESTION_OPTIONS,
          MultiOptionQuestionType.RADIO_BUTTON);

  public static final StaticContentQuestionDefinition STATIC_CONTENT_QUESTION_DEFINITION =
      new StaticContentQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Static Content Question")
              .setDescription("description")
              .setQuestionText(
                  LocalizedStrings.withDefaultValue(
                      "Hi I'm a block of static text. \n"
                          + " * Welcome to this test program.\n"
                          + " * It contains one of every question type. \n\n"
                          + "### What are the eligibility requirements? \n"
                          + ">You are 18 years or older."))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue(""))
              .build());

  public static final TextQuestionDefinition TEXT_QUESTION_DEFINITION =
      new TextQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Text Question")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.withDefaultValue("What is your favorite color?"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static final PhoneQuestionDefinition PHONE_QUESTION_DEFINITION =
      new PhoneQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Sample Phone Question")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.withDefaultValue("what is your phone number"))
              .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
              .build());

  public static DateQuestionDefinition dateQuestionDefinition(String name, String questionText) {
    return new DateQuestionDefinition(
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setDescription("description")
            .setQuestionText(LocalizedStrings.withDefaultValue(questionText))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
            .build());
  }

  public static DateQuestionDefinition dateQuestionDefinition(long enumeratorId) {
    return new DateQuestionDefinition(
        QuestionDefinitionConfig.builder()
            .setName("Sample Enumerator Date Question")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.withDefaultValue("When is $this's birthday?"))
            .setQuestionHelpText(
                LocalizedStrings.withDefaultValue("help text for $this's birthday"))
            .setEnumeratorId(enumeratorId)
            .build());
  }
}
