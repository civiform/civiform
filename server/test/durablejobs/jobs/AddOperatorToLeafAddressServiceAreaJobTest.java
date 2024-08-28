package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SqlRow;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.time.Instant;
import models.JobType;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.junit.Test;
import repository.ResetPostgres;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.predicate.Operator;

public class AddOperatorToLeafAddressServiceAreaJobTest extends ResetPostgres {

  private final Database database = DB.getDefault();

  @Test
  public void run_verifyEligibilityDefinitionAddsOperatorPropertyToSingleRootLevelAddressNode()
      throws ProgramBlockDefinitionNotFoundException, JsonProcessingException {
    String blockDefinitionsBeforeJob =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "leafAddressServiceArea",
                    "questionId": 2513,
                    "serviceAreaId": "Seattle"
                  }
                }
              }
            }
          },
          {
            "id": 2,
            "name": "Screen 2",
            "repeaterId": null,
            "description": "Screen 2 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2514,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2 description"
              }
            }
          }
        ]
        """;

    // The only difference in this string is the addition to the "operator" property on
    // leafAddressServiceArea node(s).
    String expectedBlockDefinitionsAfterUpdate =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "leafAddressServiceArea",
                    "operator": "IN_SERVICE_AREA",
                    "questionId": 2513,
                    "serviceAreaId": "Seattle"
                  }
                }
              }
            }
          },
          {
            "id": 2,
            "name": "Screen 2",
            "repeaterId": null,
            "description": "Screen 2 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2514,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2 description"
              }
            }
          }
        ]
        """;

    Long id = insertProgram(blockDefinitionsBeforeJob);

    runJob();

    assertJsonStringsAreTheSame(
        expectedBlockDefinitionsAfterUpdate, findBlockDefinitionsForProgramId(id));

    // Load the program and verify the changes work in the context of the pojo data model
    ProgramModel programModel = findProgramModelById(id);

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .isPresent())
        .isTrue();

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .get()
                .predicate()
                .rootNode()
                .getLeafAddressNode()
                .operator())
        .isEqualTo(Operator.IN_SERVICE_AREA);
  }

  @Test
  public void run_verifyEligibilityDefinitionAddsOperatorPropertyToNestedAddressNodes()
      throws ProgramBlockDefinitionNotFoundException, JsonProcessingException {
    String blockDefinitionsBeforeJob =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              },
              {
                "id": 2349,
                "optional": false,
                "addressCorrectionEnabled": false
              },
              {
                "id": 2511,
                "optional": false,
                "addressCorrectionEnabled": false
              },
              {
                "id": 2509,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "or",
                    "children": [
                      {
                        "node": {
                          "type": "and",
                          "children": [
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"4\\", \\"2\\", \\"5\\", \\"3\\", \\"1\\"]"
                                },
                                "scalar": "SELECTION",
                                "operator": "IN",
                                "questionId": 2349
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"14\\", \\"12\\", \\"15\\", \\"13\\"]"
                                },
                                "scalar": "SELECTIONS",
                                "operator": "ANY_OF",
                                "questionId": 2509
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "DATE",
                                  "value": "320716800000"
                                },
                                "scalar": "DATE",
                                "operator": "IS_AFTER",
                                "questionId": 2511
                              }
                            },
                            {
                              "node": {
                                "type": "leafAddressServiceArea",
                                "questionId": 2513,
                                "serviceAreaId": "Seattle"
                              }
                            }
                          ]
                        }
                      },
                      {
                        "node": {
                          "type": "and",
                          "children": [
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"16\\", \\"17\\", \\"15\\"]"
                                },
                                "scalar": "SELECTION",
                                "operator": "IN",
                                "questionId": 2349
                              }
                            },
                            {
                              "node": {
                                "type": "leafAddressServiceArea",
                                "questionId": 2513,
                                "serviceAreaId": "Seattle"
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"12\\", \\"15\\", \\"13\\"]"
                                },
                                "scalar": "SELECTIONS",
                                "operator": "ANY_OF",
                                "questionId": 2509
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "DATE",
                                  "value": "1701388800000"
                                },
                                "scalar": "DATE",
                                "operator": "IS_AFTER",
                                "questionId": 2511
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              }
            }
          },
          {
            "id": 2,
            "name": "Screen 2",
            "repeaterId": null,
            "description": "Screen 2 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2514,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2 description"
              }
            }
          }
        ]
        """;

    // The only difference in this string is the addition to the "operator" property on
    // leafAddressServiceArea node(s).
    String expectedBlockDefinitionsAfterUpdate =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              },
              {
                "id": 2349,
                "optional": false,
                "addressCorrectionEnabled": false
              },
              {
                "id": 2511,
                "optional": false,
                "addressCorrectionEnabled": false
              },
              {
                "id": 2509,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "or",
                    "children": [
                      {
                        "node": {
                          "type": "and",
                          "children": [
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"4\\", \\"2\\", \\"5\\", \\"3\\", \\"1\\"]"
                                },
                                "scalar": "SELECTION",
                                "operator": "IN",
                                "questionId": 2349
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"14\\", \\"12\\", \\"15\\", \\"13\\"]"
                                },
                                "scalar": "SELECTIONS",
                                "operator": "ANY_OF",
                                "questionId": 2509
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "DATE",
                                  "value": "320716800000"
                                },
                                "scalar": "DATE",
                                "operator": "IS_AFTER",
                                "questionId": 2511
                              }
                            },
                            {
                              "node": {
                                "type": "leafAddressServiceArea",
                                "operator": "IN_SERVICE_AREA",
                                "questionId": 2513,
                                "serviceAreaId": "Seattle"
                              }
                            }
                          ]
                        }
                      },
                      {
                        "node": {
                          "type": "and",
                          "children": [
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"16\\", \\"17\\", \\"15\\"]"
                                },
                                "scalar": "SELECTION",
                                "operator": "IN",
                                "questionId": 2349
                              }
                            },
                            {
                              "node": {
                                "type": "leafAddressServiceArea",
                                "operator": "IN_SERVICE_AREA",
                                "questionId": 2513,
                                "serviceAreaId": "Seattle"
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "LIST_OF_STRINGS",
                                  "value": "[\\"12\\", \\"15\\", \\"13\\"]"
                                },
                                "scalar": "SELECTIONS",
                                "operator": "ANY_OF",
                                "questionId": 2509
                              }
                            },
                            {
                              "node": {
                                "type": "leaf",
                                "value": {
                                  "type": "DATE",
                                  "value": "1701388800000"
                                },
                                "scalar": "DATE",
                                "operator": "IS_AFTER",
                                "questionId": 2511
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              }
            }
          },
          {
            "id": 2,
            "name": "Screen 2",
            "repeaterId": null,
            "description": "Screen 2 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2514,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2 description"
              }
            }
          }
        ]
        """;

    Long id = insertProgram(blockDefinitionsBeforeJob);

    runJob();

    assertJsonStringsAreTheSame(
        expectedBlockDefinitionsAfterUpdate, findBlockDefinitionsForProgramId(id));

    // Load the program and verify the changes work in the context of the pojo data model
    ProgramModel programModel = findProgramModelById(id);

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .isPresent())
        .isTrue();

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .get()
                .predicate()
                .rootNode()
                .getOrNode()
                .children()
                .get(0)
                .getAndNode()
                .children()
                .get(3)
                .getLeafAddressNode()
                .operator())
        .isEqualTo(Operator.IN_SERVICE_AREA);

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .get()
                .predicate()
                .rootNode()
                .getOrNode()
                .children()
                .get(1)
                .getAndNode()
                .children()
                .get(1)
                .getLeafAddressNode()
                .operator())
        .isEqualTo(Operator.IN_SERVICE_AREA);
  }

  @Test
  public void run_verifyHidePredicateAddsOperatorPropertyToAddressNode()
      throws ProgramBlockDefinitionNotFoundException, JsonProcessingException {
    String blockDefinitionsBeforeJob =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "leafAddressServiceArea",
                    "questionId": 2513,
                    "serviceAreaId": "Seattle"
                  }
                }
              }
            }
          },
          {
            "id": 2,
            "name": "Screen 2",
            "repeaterId": null,
            "description": "Screen 2 description",
            "hidePredicate": {
              "action": "HIDE_BLOCK",
              "rootNode": {
                "node": {
                  "type": "leafAddressServiceArea",
                  "questionId": 2513,
                  "serviceAreaId": "Seattle"
                }
              }
            },
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2514,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2 description"
              }
            }
          },
          {
            "id": 3,
            "name": "Screen 3",
            "repeaterId": null,
            "description": "Screen 3 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 3"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2515,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 3 description"
              }
            }
          }
        ]
        """;

    // The only difference in this string is the addition to the "operator" property on
    // leafAddressServiceArea node(s).
    String expectedBlockDefinitionsAfterUpdate =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "leafAddressServiceArea",
                    "operator": "IN_SERVICE_AREA",
                    "questionId": 2513,
                    "serviceAreaId": "Seattle"
                  }
                }
              }
            }
          },
          {
            "id": 2,
            "name": "Screen 2",
            "repeaterId": null,
            "description": "Screen 2 description",
            "hidePredicate": {
              "action": "HIDE_BLOCK",
              "rootNode": {
                "node": {
                  "type": "leafAddressServiceArea",
                  "operator": "IN_SERVICE_AREA",
                  "questionId": 2513,
                  "serviceAreaId": "Seattle"
                }
              }
            },
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2514,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 2 description"
              }
            }
          },
          {
            "id": 3,
            "name": "Screen 3",
            "repeaterId": null,
            "description": "Screen 3 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 3"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2515,
                "optional": false,
                "addressCorrectionEnabled": false
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 3 description"
              }
            }
          }
        ]
        """;

    Long id = insertProgram(blockDefinitionsBeforeJob);

    runJob();

    assertJsonStringsAreTheSame(
        expectedBlockDefinitionsAfterUpdate, findBlockDefinitionsForProgramId(id));

    // Load the program and verify the changes work in the context of the pojo data model
    ProgramModel programModel = findProgramModelById(id);

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(2L)
                .visibilityPredicate()
                .isPresent())
        .isTrue();

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(2L)
                .visibilityPredicate()
                .get()
                .rootNode()
                .getLeafAddressNode()
                .operator())
        .isEqualTo(Operator.IN_SERVICE_AREA);
  }

  @Test
  public void run_verifyExistingOperatorsAreUntouched()
      throws ProgramBlockDefinitionNotFoundException, JsonProcessingException {
    String blockDefinitions =
        """
        [
          {
            "id": 1,
            "name": "Screen 1",
            "repeaterId": null,
            "description": "Screen 1 description",
            "hidePredicate": null,
            "localizedName": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1"
              }
            },
            "optionalPredicate": null,
            "questionDefinitions": [
              {
                "id": 2513,
                "optional": false,
                "addressCorrectionEnabled": true
              }
            ],
            "localizedDescription": {
              "isRequired": true,
              "translations": {
                "en_US": "Screen 1 description"
              }
            },
            "eligibilityDefinition": {
              "predicate": {
                "action": "ELIGIBLE_BLOCK",
                "rootNode": {
                  "node": {
                    "type": "leafAddressServiceArea",
                    "operator": "IN_SERVICE_AREA",
                    "questionId": 2513,
                    "serviceAreaId": "Seattle"
                  }
                }
              }
            }
          }
        ]
        """;

    Long id = insertProgram(blockDefinitions);

    runJob();

    assertJsonStringsAreTheSame(blockDefinitions, findBlockDefinitionsForProgramId(id));

    // Load the program and verify the changes work in the context of the pojo data model
    ProgramModel programModel = findProgramModelById(id);

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .isPresent())
        .isTrue();

    assertThat(
            programModel
                .getProgramDefinition()
                .getBlockDefinition(1L)
                .eligibilityDefinition()
                .get()
                .predicate()
                .rootNode()
                .getLeafAddressNode()
                .operator())
        .isEqualTo(Operator.IN_SERVICE_AREA);
  }

  /**
   * Directly inserts a record into `public.programs` populated with default values and the provided
   * json for the `block_definitions` column.
   *
   * @param blockDefinitions Json string representing the block definition configuration to store
   * @return the new Id of the inserted record
   */
  private Long insertProgram(String blockDefinitions) {
    String insertSql =
        """
        INSERT INTO public.programs
        (
          name,
          description,
          block_definitions,
          slug,
          localized_name,
          localized_description,
          external_link,
          display_mode,
          create_time,
          last_modified_time,
          program_type,
          eligibility_is_gating,
          localized_confirmation_message
        )
        VALUES
        (
          'program-name',
          '',
          CAST(:block_definitions AS jsonb),
          'program-name',
          CAST('{ "translations": {} }' AS jsonb),
          CAST('{ "translations": {} }' AS jsonb),
          '',
          'PUBLIC',
          '2024-06-01',
          '2024-06-01',
          'default',
          true,
          CAST('{ "translations": {} }' AS jsonb)
        )
        RETURNING id;
        """;

    try (Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
      SqlRow sqlRow =
          database
              .sqlQuery(insertSql)
              .setParameter("block_definitions", blockDefinitions)
              .findOne();
      transaction.commit();

      assertThat(sqlRow).isNotNull();

      Long id = sqlRow.getLong("id");
      assertThat(id).isGreaterThan(0);

      return id;
    }
  }

  /**
   * Both strings will be serialized to a Jackson JsonNode and compared against their pretty strings
   * in order to normalize whitespace, formatting, and property ordering.
   *
   * @param expected, a json string value
   * @param actual, a json string value
   */
  private void assertJsonStringsAreTheSame(String expected, String actual)
      throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    assertThat(objectMapper.readTree(expected).toPrettyString())
        .isEqualTo(objectMapper.readTree(actual).toPrettyString());
  }

  /** Build and run the job */
  private void runJob() {
    AddOperatorToLeafAddressServiceAreaJob job =
        new AddOperatorToLeafAddressServiceAreaJob(
            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()));

    job.run();
  }

  /** Load block_definitons from the database for the supplied program id */
  private String findBlockDefinitionsForProgramId(Long id) {
    String selectSql =
        """
        SELECT block_definitions
        FROM programs
        WHERE id = :id
        """;

    SqlRow sqlRow = database.sqlQuery(selectSql).setParameter("id", id).findOne();

    return sqlRow.getString("block_definitions");
  }

  /** Find the {@link ProgramModel} from the supplied id */
  private ProgramModel findProgramModelById(Long id) {
    ProgramModel programModel = database.find(ProgramModel.class).where().idEq(id).findOne();

    assertThat(programModel).isNotNull();
    assertThat(programModel.id).isEqualTo(id);

    return programModel;
  }
}
