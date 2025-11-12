package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Database;
import models.ApplicationStatusesModel;
import models.ProgramModel;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.ErrorAnd;
import services.migration.ProgramMigrationService;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.types.QuestionDefinition;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;
import views.admin.migration.AdminProgramImportForm;

public class AdminImportControllerTest extends ResetPostgres {
  private static final String CREATE_DUPLICATE = "CREATE_DUPLICATE";
  private static final String OVERWRITE_EXISTING = "OVERWRITE_EXISTING";
  private AdminImportController controller;
  private Database database;

  @Before
  public void setUp() {
    controller =
        new AdminImportController(
            instanceOf(AdminImportView.class),
            instanceOf(AdminImportViewPartial.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramMigrationService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class),
            instanceOf(ProgramService.class));
    database = DB.getDefault();
  }

  @Test
  public void index_migrationEnabled_ok() {
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import a program");
  }

  @Test
  public void hxImportProgram_noRequestBody_redirectsToIndex() {
    Result result = controller.hxImportProgram(fakeRequest());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminImportController.index().url());
  }

  @Test
  public void hxImportProgram_malformattedJson_error() {
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", "{\"adminName : \"admin-name\"}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_noTopLevelProgramFieldInJson_error() {
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        "{ \"id\" : 32, \"adminName\" : \"admin-name\","
                            + " \"adminDescription\" : \"description\"}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result))
        .containsPattern("JSON did not have a top-level .*program.* field");
  }

  @Test
  public void hxImportProgram_notEnoughInfoToCreateProgramDef_error() {
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        "{ \"program\": { \"adminName\" : \"admin-name\","
                            + " \"adminDescription\" : \"description\"}}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_programAlreadyExists_error() {
    // save a program
    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    // attempt to import the program again
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("This program already exists in our system.");
    assertThat(contentAsString(result)).contains("Please check your file and and try again.");
  }

  @Test
  public void hxImportProgram_negativeBlockId_error() {
    // attempt to import a program with a negative block id
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_NEGATIVE_BLOCK_ID))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Block definition ids must be greater than 0.");
    assertThat(contentAsString(result))
        .contains("Please check your block definition ids and try again.");
  }

  @Test
  public void hxImportProgram_handlesServerError() {
    // attempt to import program with bad json - question id in block definition does not match
    // question id on question
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_MISMATCHED_QUESTION_ID))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("One or more question errors occured");
    assertThat(contentAsString(result)).contains("Question ID 2 is not defined");
  }

  @Test
  public void hxImportProgram_noDuplicatesNotEnabled_draftsExist_noError() {
    // Create a draft program, so that there are unpublished programs
    ProgramBuilder.newDraftProgram("draft-program").build();

    // save a program
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // no error because duplicate questions for migration is not enabled
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Minimal Sample Program");
    assertThat(contentAsString(result)).contains("minimal-sample-program");
    assertThat(contentAsString(result)).contains("Screen 1");
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void hxImportProgram_jsonHasAllProgramInfo_resultHasProgramAndQuestionInfo() {
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Minimal Sample Program");
    assertThat(contentAsString(result)).contains("minimal-sample-program");
    assertThat(contentAsString(result)).contains("Screen 1");
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void hxSaveProgram_savesTheProgramWithoutQuestions() {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITHOUT_QUESTIONS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "no-questions")
            .findOne()
            .getProgramDefinition();

    assertThat(programDefinition.externalLink()).isEqualTo("https://www.example.com");
  }

  @Test
  public void hxSaveProgram_savesTheProgramWithQuestions() {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "minimal-sample-program")
            .findOne()
            .getProgramDefinition();
    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Name")
            .findOne()
            .getQuestionDefinition();

    assertThat(programDefinition.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(questionDefinition.getQuestionText().getDefault())
        .isEqualTo("Please enter your first and last name");
    assertThat(programDefinition.getQuestionIdsInProgram()).contains(questionDefinition.getId());
  }

  @Test
  public void hxSaveProgram_handlesNestEnumeratorQuestions() {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ENUMERATORS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition enumeratorQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Sample Enumerator Question")
            .findOne()
            .getQuestionDefinition();

    QuestionDefinition nestedEnumeratorQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "cats")
            .findOne()
            .getQuestionDefinition();

    QuestionDefinition childQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "cat-color")
            .findOne()
            .getQuestionDefinition();

    assertThat(enumeratorQuestionDefinition.getId())
        .isEqualTo(nestedEnumeratorQuestionDefinition.getEnumeratorId().get());
    assertThat(nestedEnumeratorQuestionDefinition.getId())
        .isEqualTo(childQuestionDefinition.getEnumeratorId().get());
  }

  @Test
  public void hxSaveProgram_savesUpdatedQuestionIdsOnPredicates()
      throws ProgramBlockDefinitionNotFoundException {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PREDICATES))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "visibility-eligibility")
            .findOne()
            .getProgramDefinition();
    Long eligibilityQuestionId =
        programDefinition
            .getBlockDefinition(1)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getOrNode()
            .children()
            .get(0)
            .getAndNode()
            .children()
            .get(0)
            .getLeafOperationNode()
            .questionId();
    Long visibilityQuestionId =
        programDefinition
            .getBlockDefinition(2)
            .visibilityPredicate()
            .get()
            .rootNode()
            .getLeafOperationNode()
            .questionId();
    Long savedQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "id-test")
            .findOne()
            .getQuestionDefinition()
            .getId();

    assertThat(eligibilityQuestionId).isEqualTo(savedQuestionId);
    assertThat(visibilityQuestionId).isEqualTo(savedQuestionId);

    Long eligibilityInServiceAreaAddressQuestionId =
        programDefinition
            .getBlockDefinition(3)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getLeafAddressNode()
            .questionId();
    Long savedInServiceAreaAddressQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Address")
            .findOne()
            .getQuestionDefinition()
            .getId();
    assertThat(eligibilityInServiceAreaAddressQuestionId)
        .isEqualTo(savedInServiceAreaAddressQuestionId);

    Long eligibilityNotInServiceAreaAddressQuestionId =
        programDefinition
            .getBlockDefinition(4)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getLeafAddressNode()
            .questionId();
    Long savedNotInServiceAreaAddressQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "second-address")
            .findOne()
            .getQuestionDefinition()
            .getId();
    assertThat(eligibilityNotInServiceAreaAddressQuestionId)
        .isEqualTo(savedNotInServiceAreaAddressQuestionId);
  }

  @Test
  public void hxSaveProgram_discardsPaiTagsOnImportedQuestions() {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PAI_TAGS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "dob")
            .findOne()
            .getQuestionDefinition();

    assertThat(questionDefinition.getPrimaryApplicantInfoTags()).isEqualTo(ImmutableSet.of());
  }

  @Test
  public void hxSaveProgram_preservesUniversalSettingOnImportedQuestions() {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                // Questions must be marked as "universal" before being tagged with a PAI
                // tag, so we can reuse the PROGRAM_JSON_WITH_PAI_TAGS json
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PAI_TAGS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "dob")
            .findOne()
            .getQuestionDefinition();

    assertThat(questionDefinition.isUniversal()).isTrue();
  }

  @Test
  public void hxSaveProgram_addsAnEmptyStatus() {
    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    StatusDefinitions statusDefinitions =
        database
            .find(ApplicationStatusesModel.class)
            .where()
            .eq("program_name", "minimal-sample-program")
            .findOne()
            .getStatusDefinitions();

    assertThat(statusDefinitions.getStatuses()).isEmpty();
  }

  @Test
  public void deserialize_malformedOption_returnsError() {
    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult =
        controller.getDeserializeResult(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        PROGRAM_JSON_WITH_ONE_QUESTION,
                        AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX + "Name",
                        "FOO"))
                .build());

    assertThat(deserializeResult.isError()).isTrue();
    assertThat(deserializeResult.getErrors()).hasSize(1);
    assertThat(deserializeResult.getErrors())
        .contains(
            "JSON is incorrectly formatted: No enum constant"
                + " controllers.admin.ProgramMigrationWrapper.DuplicateQuestionHandlingOption.FOO");
  }

  @Test
  public void deserialize_multipleOptions_parsesCorrectly() {
    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult =
        controller.getDeserializeResult(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        PROGRAM_JSON_WITH_ONE_QUESTION,
                        "UnrecognizedPrefix",
                        OVERWRITE_EXISTING,
                        AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX + "Name",
                        OVERWRITE_EXISTING,
                        AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX + "Text",
                        CREATE_DUPLICATE))
                .build());

    assertThat(deserializeResult.isError()).isFalse();
    ImmutableMap<String, ProgramMigrationWrapper.DuplicateQuestionHandlingOption> duplicateOptions =
        deserializeResult.getResult().getDuplicateQuestionHandlingOptions();
    assertThat(duplicateOptions).hasSize(2);
    assertThat(duplicateOptions).containsKey("Name");
    assertThat(duplicateOptions.get("Name"))
        .isEqualTo(ProgramMigrationWrapper.DuplicateQuestionHandlingOption.OVERWRITE_EXISTING);
    assertThat(duplicateOptions).containsKey("Text");
    assertThat(duplicateOptions.get("Text"))
        .isEqualTo(ProgramMigrationWrapper.DuplicateQuestionHandlingOption.CREATE_DUPLICATE);
  }

  public static final String PROGRAM_JSON_WITHOUT_QUESTIONS =
      """
      {
            "program" : {
              "id" : 9,
              "adminName" : "no-questions",
              "adminDescription" : "",
              "externalLink" : "https://www.example.com",
              "displayMode" : "PUBLIC",
              "loginOnly" : false,
              "notificationPreferences" : [ ],
              "localizedName" : {
                "translations" : {
                  "en_US" : "Program With No Questions"
                },
                "isRequired" : true
              },
              "localizedDescription" : {
                "translations" : {
                  "en_US" : "No questions"
                },
                "isRequired" : true
              },
              "localizedConfirmationMessage" : {
                "translations" : {
                  "en_US" : ""
                },
                "isRequired" : true
              },
              "blockDefinitions" : [ {
                "id" : 1,
                "name" : "Screen 1",
                "description" : "Screen 1 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 1"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 1 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ ]
              } ],
              "statusDefinitions" : {
                "statuses" : [ ]
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
                "tiProgramViewAcls" : [ ]
              },
              "categories" : [ ],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }],
             "bridgeDefinitions" : { }
            }
          }
      """;

  public static final String PROGRAM_JSON_WITH_NEGATIVE_BLOCK_ID =
      """
      {
            "program" : {
              "id" : 9,
              "adminName" : "no-questions",
              "adminDescription" : "",
              "externalLink" : "https://www.example.com",
              "displayMode" : "PUBLIC",
              "loginOnly" : false,
              "notificationPreferences" : [ ],
              "localizedName" : {
                "translations" : {
                  "en_US" : "Program With No Questions"
                },
                "isRequired" : true
              },
              "localizedDescription" : {
                "translations" : {
                  "en_US" : "No questions"
                },
                "isRequired" : true
              },
              "localizedConfirmationMessage" : {
                "translations" : {
                  "en_US" : ""
                },
                "isRequired" : true
              },
              "blockDefinitions" : [ {
                "id" : -1,
                "name" : "Screen 1",
                "description" : "Screen 1 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 1"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 1 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ ]
              } ],
              "statusDefinitions" : {
                "statuses" : [ ]
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
                "tiProgramViewAcls" : [ ]
              },
              "categories" : [ ],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }],
             "bridgeDefinitions" : { }
            }
          }
      """;

  public static final String PROGRAM_JSON_WITH_ONE_QUESTION =
      """
          {
          "program" : {
              "id" : 7,
              "adminName" : "minimal-sample-program",
              "adminDescription" : "desc",
              "externalLink" : "https://github.com/civiform/civiform",
              "displayMode" : "PUBLIC",
              "loginOnly" : false,
              "notificationPreferences" : [ ],
              "localizedName" : {
              "translations" : {
                  "en_US" : "Minimal Sample Program"
              },
              "isRequired" : true
              },
              "localizedDescription" : {
              "translations" : {
                  "en_US" : "display description"
              },
              "isRequired" : true
              },
              "localizedShortDescription" : {
              "translations" : {
                  "en_US" : "short display description"
              },
              "isRequired" : true
              },
              "localizedConfirmationMessage" : {
              "translations" : {
                  "en_US" : ""
              },
              "isRequired" : true
              },
              "blockDefinitions" : [ {
              "id" : 1,
              "name" : "Screen 1",
              "description" : "Screen 1",
              "localizedName" : {
                  "translations" : {
                  "en_US" : "Screen 1"
                  },
                  "isRequired" : true
              },
              "localizedDescription" : {
                  "translations" : {
                  "en_US" : "Screen 1"
                  },
                  "isRequired" : true
              },
              "repeaterId" : null,
              "hidePredicate" : null,
              "optionalPredicate" : null,
              "questionDefinitions" : [ {
                  "id" : 1,
                  "optional" : true,
                  "addressCorrectionEnabled" : false
              } ]
              } ],
              "statusDefinitions" : {
              "statuses" : [ ]
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
              "tiProgramViewAcls" : [ ]
              },
              "categories" : [ ],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }],
             "bridgeDefinitions" : {
                 "admin-name-1": {
                     "inputFields" : [ {
                         "questionName": "q-name-1",
                         "questionScalar": "TEXT",
                         "externalName": "e-name-2"
                     }],
                     "outputFields" : [ {
                         "questionName": "q-name-2",
                         "questionScalar": "TEXT",
                         "externalName": "e-name-2"
                     }]
                 }
             }
          },
          "questions" : [ {
              "type" : "name",
              "config" : {
              "name" : "Name",
              "description" : "The applicant's name",
              "questionText" : {
                  "translations" : {
                  "am" : "ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)",
                  "ko" : "성함 (이름 및 성의 경우 이니셜도 괜찮음)",
                  "lo" : "ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)",
                  "so" : "Magaca (magaca koowaad iyo kan dambe okay)",
                  "tl" : "Pangalan (unang pangalan at ang unang titik ng apilyedo ay okay)",
                  "vi" : "Tên (tên và họ viết tắt đều được)",
                  "en_US" : "Please enter your first and last name",
                  "es_US" : "Nombre (nombre y la inicial del apellido está bien)",
                  "zh_TW" : "姓名（名字和姓氏第一個字母便可）"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : { },
                  "isRequired" : false
              },
              "validationPredicates" : {
                  "type" : "name"
              },
              "id" : 1,
              "universal" : false,
              "displayMode" : "VISIBLE",
              "primaryApplicantInfoTags" : [ ]
              }
          } ]
          }
      """;

  public static final String PROGRAM_JSON_WITH_ENUMERATORS =
      """
      {
            "program" : {
              "id" : 18,
              "adminName" : "nested-enumerator",
              "adminDescription" : "",
              "externalLink" : "",
              "displayMode" : "PUBLIC",
              "loginOnly" : false,
              "notificationPreferences" : [ ],
              "localizedName" : {
                "translations" : {
                  "en_US" : "nest enumerator program"
                },
                "isRequired" : true
              },
              "localizedDescription" : {
                "translations" : {
                  "en_US" : "nested enumerator program"
                },
                "isRequired" : true
              },
              "localizedConfirmationMessage" : {
                "translations" : {
                  "en_US" : ""
                },
                "isRequired" : true
              },
              "blockDefinitions" : [ {
                "id" : 5,
                "name" : "Screen 5",
                "description" : "Screen 5 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 5"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 5 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 13,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              }, {
                "id" : 1,
                "name" : "Screen 1",
                "description" : "Screen 1 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 1"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 1 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 10,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              }, {
                "id" : 3,
                "name" : "Screen 3 (repeated from 1)",
                "description" : "Screen 3 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 3 (repeated from 1)"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 3 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : 1,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 94,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              }, {
                "id" : 4,
                "name" : "Screen 4 (repeated from 3)",
                "description" : "Screen 4 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 4 (repeated from 3)"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 4 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : 3,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 95,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              } ],
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
                "tiProgramViewAcls" : [ ]
              },
              "localizedSummaryImageDescription" : null,
              "categories" : [ ],
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }],
             "bridgeDefinitions" : { }
            },
            "questions" : [ {
              "type" : "name",
              "config" : {
                "name" : "Sample Name Question",
                "description" : "description",
                "questionText" : {
                  "translations" : {
                    "en_US" : "What is your name?"
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : {
                    "en_US" : "help text"
                  },
                  "isRequired" : true
                },
                "validationPredicates" : {
                  "type" : "name"
                },
                "id" : 13,
                "universal" : true,
                "displayMode" : "VISIBLE",
                "primaryApplicantInfoTags" : [ "APPLICANT_NAME" ]
              }
            }, {
              "type" : "enumerator",
              "config" : {
                "name" : "Sample Enumerator Question",
                "description" : "description",
                "questionText" : {
                  "translations" : {
                    "en_US" : "List all members of your household."
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : {
                    "en_US" : "help text"
                  },
                  "isRequired" : true
                },
                "validationPredicates" : {
                  "type" : "enumerator",
                  "minEntities" : null,
                  "maxEntities" : null
                },
                "id" : 10,
                "universal" : false,
                "displayMode" : "VISIBLE",
                "primaryApplicantInfoTags" : [ ]
              },
              "entityType" : {
                "translations" : {
                  "en_US" : "household member"
                },
                "isRequired" : true
              }
            }, {
              "type" : "enumerator",
              "config" : {
                "name" : "cats",
                "description" : "",
                "questionText" : {
                  "translations" : {
                    "en_US" : "Please list each cat owned by $this"
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : { },
                  "isRequired" : false
                },
                "validationPredicates" : {
                  "type" : "enumerator",
                  "minEntities" : null,
                  "maxEntities" : null
                },
                "id" : 94,
                "enumeratorId" : 10,
                "universal" : false,
                "displayMode" : "VISIBLE",
                "primaryApplicantInfoTags" : [ ]
              },
              "entityType" : {
                "translations" : {
                  "en_US" : "cat"
                },
                "isRequired" : true
              }
            }, {
              "type" : "text",
              "config" : {
                "name" : "cat-color",
                "description" : "",
                "questionText" : {
                  "translations" : {
                    "en_US" : "What color is $this?"
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : { },
                  "isRequired" : false
                },
                "validationPredicates" : {
                  "type" : "text",
                  "minLength" : null,
                  "maxLength" : null
                },
                "id" : 95,
                "enumeratorId" : 94,
                "universal" : false,
                "displayMode" : "VISIBLE",
                "primaryApplicantInfoTags" : [ ]
              }
            } ]
          }
      """;

  // Can't use multistring here because of escaped characters in predicates
  public static final String PROGRAM_JSON_WITH_PREDICATES =
      """
      {
        "program" : {
          "id" : 1,
          "adminName" : "visibility-eligibility",
          "adminDescription" : "",
          "externalLink" : "",
          "displayMode" : "PUBLIC",
          "loginOnly" : false,
          "notificationPreferences" : [ ],
          "localizedName" : {
            "translations" : {
              "en_US" : "visibility and eligibility test"
            },
            "isRequired" : true
          },
          "localizedDescription" : {
            "translations" : {
              "en_US" : "visibility and eligibility test"
            },
            "isRequired" : true
          },
          "localizedConfirmationMessage" : {
            "translations" : {
              "en_US" : ""
            },
            "isRequired" : true
          },
          "blockDefinitions" : [ {
            "id" : 1,
            "name" : "Screen 1",
            "description" : "Screen 1 description",
            "localizedName" : {
              "translations" : {
                "en_US" : "Screen 1"
              },
              "isRequired" : true
            },
            "localizedDescription" : {
              "translations" : {
                "en_US" : "Screen 1 description"
              },
              "isRequired" : true
            },
            "repeaterId" : null,
            "hidePredicate" : null,
            "eligibilityDefinition" : {
              "predicate" : {
                "rootNode" : {
                  "node" : {
                    "type" : "or",
                    "children" : [ {
                      "node" : {
                        "type" : "and",
                        "children" : [ {
                          "node" : {
                            "type" : "leaf",
                            "questionId" : 3,
                            "scalar" : "SELECTIONS",
                            "operator" : "ANY_OF",
                            "value" : {
                              "value" : "[\\"0\\", \\"9\\", \\"11\\"]",
                              "type" : "LIST_OF_STRINGS"
                            }
                          }
                        }, {
                          "node" : {
                            "type" : "leaf",
                            "questionId" : 3,
                            "scalar" : "SELECTION",
                            "operator" : "IN",
                            "value" : {
                              "value" : "[\\"1\\"]",
                              "type" : "LIST_OF_STRINGS"
                            }
                          }
                        } ]
                      }
                    } ]
                  }
                },
                "action" : "ELIGIBLE_BLOCK"
              }
            },
            "optionalPredicate" : null,
            "questionDefinitions" : [ {
              "id" : 3,
              "optional" : false,
              "addressCorrectionEnabled" : false
            } ]
          }, {
            "id" : 2,
            "name" : "Screen 2",
            "description" : "Screen 2 description",
            "localizedName" : {
              "translations" : {
                "en_US" : "Screen 2"
              },
              "isRequired" : true
            },
            "localizedDescription" : {
              "translations" : {
                "en_US" : "Screen 2 description"
              },
              "isRequired" : true
            },
            "repeaterId" : null,
            "hidePredicate" : {
              "rootNode" : {
                "node" : {
                  "type" : "leaf",
                  "questionId" : 3,
                  "scalar" : "ID",
                  "operator" : "EQUAL_TO",
                  "value" : {
                    "value" : "\\"1\\"",
                    "type" : "STRING"
                  }
                }
              },
              "action" : "HIDE_BLOCK"
            },
            "optionalPredicate" : null,
            "questionDefinitions" : [ {
              "id" : 4,
              "optional" : false,
              "addressCorrectionEnabled" : false
            } ]
          }, {
            "id" : 3,
            "name" : "Screen 3",
            "description" : "Screen 3 description",
            "localizedName" : {
              "translations" : {
                "en_US" : "Screen 3"
              },
              "isRequired" : true
            },
            "localizedDescription" : {
              "translations" : {
                "en_US" : "Screen 3 description"
              },
              "isRequired" : true
            },
            "repeaterId" : null,
            "hidePredicate" : null,
            "eligibilityDefinition" : {
              "predicate" : {
                "rootNode" : {
                  "node" : {
                    "type" : "leafAddressServiceArea",
                    "questionId" : 5,
                    "serviceAreaId" : "Seattle",
                    "operator" : "IN_SERVICE_AREA"
                  }
                },
                "action" : "ELIGIBLE_BLOCK"
              }
            },
            "optionalPredicate" : null,
            "questionDefinitions" : [ {
              "id" : 5,
              "optional" : false,
              "addressCorrectionEnabled" : true
            } ]
          }, {
            "id" : 4,
            "name" : "Screen 4",
            "description" : "Screen 4 description",
            "localizedName" : {
              "translations" : {
                "en_US" : "Screen 4"
              },
              "isRequired" : true
            },
            "localizedDescription" : {
              "translations" : {
                "en_US" : "Screen 4 description"
              },
              "isRequired" : true
            },
            "repeaterId" : null,
            "hidePredicate" : null,
            "eligibilityDefinition" : {
              "predicate" : {
                "rootNode" : {
                  "node" : {
                    "type" : "leafAddressServiceArea",
                    "questionId" : 6,
                    "serviceAreaId" : "Seattle",
                    "operator" : "NOT_IN_SERVICE_AREA"
                  }
                },
                "action" : "ELIGIBLE_BLOCK"
              }
            },
            "optionalPredicate" : null,
            "questionDefinitions" : [ {
              "id" : 6,
              "optional" : false,
              "addressCorrectionEnabled" : true
            } ]
          } ],
          "programType" : "DEFAULT",
          "eligibilityIsGating" : true,
          "acls" : {
            "tiProgramViewAcls" : [ ]
          },
          "localizedSummaryImageDescription" : null,
          "categories" : [ ],
          "applicationSteps" : [ {
            "title" : {
               "translations" : {
                 "en_US" : "step one"
               },
               "isRequired" : true
           },
           "description" : {
              "translations" : {
              "en_US" : "step one"
           },
             "isRequired" : true
           }
         }],
         "bridgeDefinitions" : { }
        },
        "questions" : [ {
          "type" : "id",
          "config" : {
            "name" : "id-test",
            "description" : "",
            "questionText" : {
              "translations" : {
                "en_US" : "Enter a number"
              },
              "isRequired" : true
            },
            "questionHelpText" : {
              "translations" : { },
              "isRequired" : false
            },
            "validationPredicates" : {
              "type" : "id",
              "minLength" : null,
              "maxLength" : null
            },
            "id" : 3,
            "universal" : false,
            "displayMode" : "VISIBLE",
            "primaryApplicantInfoTags" : [ ]
          }
        }, {
          "type" : "text",
          "config" : {
            "name" : "text test",
            "description" : "",
            "questionText" : {
              "translations" : {
                "en_US" : "text test"
              },
              "isRequired" : true
            },
            "questionHelpText" : {
              "translations" : { },
              "isRequired" : false
            },
            "validationPredicates" : {
              "type" : "text",
              "minLength" : null,
              "maxLength" : null
            },
            "id" : 4,
            "universal" : false,
            "displayMode" : "VISIBLE",
            "primaryApplicantInfoTags" : [ ]
          }
        }, {
          "type" : "address",
          "config" : {
            "name" : "Address",
            "description" : "",
            "questionText" : {
              "translations" : {
                "en_US" : "What is your address?"
              },
              "isRequired" : true
            },
            "questionHelpText" : {
              "translations" : { },
              "isRequired" : false
            },
            "validationPredicates" : {
              "type" : "address",
              "disallowPoBox" : false
            },
            "id" : 5,
            "universal" : false,
            "displayMode" : "VISIBLE",
            "primaryApplicantInfoTags" : [ ]
          }
        }, {
          "type" : "address",
          "config" : {
            "name" : "second-address",
            "description" : "",
            "questionText" : {
              "translations" : {
                "en_US" : "Second address question"
              },
              "isRequired" : true
            },
            "questionHelpText" : {
              "translations" : {
                "en_US" : "Second address question"
              },
              "isRequired" : true
            },
            "validationPredicates" : {
              "type" : "address",
              "disallowPoBox" : false
            },
            "id" : 6,
            "universal" : false,
            "displayMode" : "VISIBLE",
            "primaryApplicantInfoTags" : [ ]
          }
        } ]
      }\
      """;

  public static final String PROGRAM_JSON_WITH_PAI_TAGS =
      """
          {
          "program" : {
              "id" : 2,
              "adminName" : "pai-program",
              "adminDescription" : "admin description",
              "externalLink" : "https:usa.gov",
              "displayMode" : "PUBLIC",
              "loginOnly" : false,
              "notificationPreferences" : [],
              "localizedName" : {
              "translations" : {
                  "en_US" : "PAI Program"
              },
              "isRequired" : true
              },
              "localizedDescription" : {
              "translations" : {
                  "en_US" : "program description"
              },
              "isRequired" : true
              },
              "blockDefinitions" : [{
              "id" : 1,
              "name" : "Screen 1",
              "description" : "dummy description",
              "localizedName" : {
                  "translations" : {
                  "en_US" : "Screen 1"
                  },
                  "isRequired" : true
              },
              "localizedDescription" : {
                  "translations" : {
                  "en_US" : "dummy description"
                  },
                  "isRequired" : true
              },
              "repeaterId" : null,
              "hidePredicate" : null,
              "optionalPredicate" : null,
              "questionDefinitions" : [{
                  "id" : 3,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }, {
                  "id" : 4,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }, {
                  "id" : 5,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }, {
                  "id" : 6,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }]
              }],
              "statusDefinitions" : {
              "statuses" : []
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
              "tiProgramViewAcls" : []
              },
              "categories" : [],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }],
             "bridgeDefinitions" : { }
          },
          "questions" : [{
              "type" : "date",
              "config" : {
              "name" : "dob",
              "description" : "date description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Date of birth"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "date question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "date"
              },
              "id" : 3,
              "universal" : true,
              "displayMode" : "VISIBLE",
              "primaryApplicantInfoTags" : ["APPLICANT_DOB"]
              }
          }, {
              "type" : "name",
              "config" : {
              "name" : "name",
              "description" : "name description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Name"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "name question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "name"
              },
              "id" : 4,
              "universal" : true,
              "displayMode" : "VISIBLE",
              "primaryApplicantInfoTags" : ["APPLICANT_NAME"]
              }
          }, {
              "type" : "phone",
              "config" : {
              "name" : "phone",
              "description" : "Phone description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Phone"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "Phone question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "phone"
              },
              "id" : 5,
              "universal" : true,
              "displayMode" : "VISIBLE",
              "primaryApplicantInfoTags" : ["APPLICANT_PHONE"]
              }
          }, {
              "type" : "email",
              "config" : {
              "name" : "email",
              "description" : "email description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Email"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "email question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "email"
              },
              "id" : 6,
              "universal" : true,
              "displayMode" : "VISIBLE",
              "primaryApplicantInfoTags" : ["APPLICANT_EMAIL"]
              }
          }]
          }
      """;

  public static final String PROGRAM_JSON_WITH_MISMATCHED_QUESTION_ID =
      """
      {
        "program" : {
          "id" : 7,
          "adminName" : "minimal-sample-program",
          "adminDescription" : "desc",
          "externalLink" : "https://github.com/civiform/civiform",
          "displayMode" : "PUBLIC",
          "loginOnly" : false,
          "notificationPreferences" : [ ],
          "localizedName" : {
            "translations" : {
              "en_US" : "Minimal Sample Program"
            },
            "isRequired" : true
          },
          "localizedDescription" : {
            "translations" : {
              "en_US" : "display description"
            },
            "isRequired" : true
          },
          "localizedShortDescription" : {
            "translations" : {
              "en_US" : "short display description"
            },
            "isRequired" : true
          },
          "localizedConfirmationMessage" : {
            "translations" : {
              "en_US" : ""
            },
            "isRequired" : true
          },
          "blockDefinitions" : [ {
            "id" : 1,
            "name" : "Screen 1",
            "description" : "Screen 1",
            "localizedName" : {
              "translations" : {
                "en_US" : "Screen 1"
              },
              "isRequired" : true
            },
            "localizedDescription" : {
              "translations" : {
                "en_US" : "Screen 1"
              },
              "isRequired" : true
            },
            "repeaterId" : null,
            "hidePredicate" : null,
            "optionalPredicate" : null,
            "questionDefinitions" : [ {
              "id" : 2,
              "optional" : true,
              "addressCorrectionEnabled" : false
            } ]
          } ],
          "statusDefinitions" : {
            "statuses" : [ ]
          },
          "programType" : "DEFAULT",
          "eligibilityIsGating" : true,
          "acls" : {
            "tiProgramViewAcls" : [ ]
          },
          "categories" : [ ],
          "localizedSummaryImageDescription" : null,
          "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }],
             "bridgeDefinitions" : { }
        },
        "questions" : [ {
          "type" : "name",
          "config" : {
            "name" : "Name",
            "description" : "The applicants name",
            "questionText" : {
              "translations" : {
                "am" : "ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)",
                "ko" : "성함 (이름 및 성의 경우 이니셜도 괜찮음)",
                "lo" : "ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)",
                "so" : "Magaca (magaca koowaad iyo kan dambe okay)",
                "tl" : "Pangalan (unang pangalan at ang unang titik ng apilyedo ay okay)",
                "vi" : "Tên (tên và họ viết tắt đều được)",
                "en_US" : "Please enter your first and last name",
                "es_US" : "Nombre (nombre y la inicial del apellido está bien)",
                "zh_TW" : "姓名（名字和姓氏第一個字母便可）"
              },
              "isRequired" : true
            },
            "questionHelpText" : {
              "translations" : { },
              "isRequired" : false
            },
            "validationPredicates" : {
              "type" : "name"
            },
            "id" : 1,
            "universal" : false,
            "displayMode" : "VISIBLE",
            "primaryApplicantInfoTags" : [ ]
          }
        } ]
      }
      """;
}
