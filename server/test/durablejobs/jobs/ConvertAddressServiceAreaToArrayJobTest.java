package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;

@RunWith(JUnitParamsRunner.class)
public class ConvertAddressServiceAreaToArrayJobTest extends ResetPostgres {

  //   private final Database database = DB.getDefault();

  static ImmutableList<Object[]> convertStringToArrayParameters() {
    return ImmutableList.of(
        new Object[] {"a_b_1", "a", "b", 1L},
        //      new Object[]{ "a_b_1", "a", "b", 1L },
        //      new Object[]{ "a_b_1", "a", "b", 1L },
        //      new Object[]{ "a_b_1", "a", "b", 1L },
        new Object[] {"a_b_c_1", "a_b", "c", 1L});
  }

  @Test
  // @TestCaseName("{index} {0} config get() should be parsable")
  @Parameters(method = "convertStringToArrayParameters")
  public void convertStringToArray(
      String value, String serviceAreaId, String state, Long timestamp) {
    var row = ConvertAddressServiceAreaToArrayJob.Row.create(value);

    assertThat(row.serviceAreaId()).isEqualTo(serviceAreaId);
    assertThat(row.state()).isEqualTo(state);
    assertThat(row.timestamp()).isEqualTo(timestamp);
  }

  //  @Test
  //  public void run_migrates() {
  //    String objectJson =
  //        """
  //        {
  //           "applicant": {
  //             "email": {
  //               "email": "test2@test.com",
  //               "updated_at": 1711473354908,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_fruit": {
  //               "selection": "6",
  //               "updated_at": 1711475796437,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_greens": {
  //               "selection": "8",
  //               "updated_at": 1711475796437,
  //               "program_updated_in": 1788
  //             },
  //             "age_groupings": {
  //               "selection": "3",
  //               "updated_at": 1711473828350,
  //               "program_updated_in": 1788
  //             },
  //             "health_rating": {
  //               "selection": "4",
  //               "updated_at": 1711473837599,
  //               "program_updated_in": 1788
  //             },
  //             "applicant_name": {
  //               "last_name": "Tester2",
  //               "first_name": "Tester",
  //               "updated_at": 1711473354908,
  //               "program_updated_in": 1788
  //             },
  //             "applicant_race": {
  //               "selections": [
  //                 "6",
  //                 "5",
  //                 "4"
  //               ],
  //               "updated_at": 1711473828350,
  //               "program_updated_in": 1788
  //             },
  //             "fried_potatoes": {
  //               "selection": "7",
  //               "updated_at": 1711475796437,
  //               "program_updated_in": 1788
  //             },
  //             "other_potatoes": {
  //               "selection": "5",
  //               "updated_at": 1711475796437,
  //               "program_updated_in": 1788
  //             },
  //             "children_under_": {
  //               "selection": "0",
  //               "updated_at": 1711473828350,
  //               "program_updated_in": 1788
  //             },
  //             "other_vegetables": {
  //               "selection": "3",
  //               "updated_at": 1711475796437,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_bucks_last_": {
  //               "number": 265846,
  //               "updated_at": 1711473234885,
  //               "program_updated_in": 1788
  //             },
  //             "household_address": {
  //               "zip": "98104",
  //               "city": "SEATTLE",
  //               "line2": "",
  //               "state": "WA",
  //               "street": "700 5TH AVE",
  //               "latitude": 224277.8911539596,
  //               "corrected": "Corrected",
  //               "longitude": 1271253.7198547737,
  //               "updated_at": 1711473076094,
  //               "service_area": "Seattle_InArea_1711473076093",
  //               "well_known_id": 2926,
  //               "program_updated_in": 1788
  //             },
  //             "privacy_statement": {
  //               "updated_at": 1711473131012,
  //               "program_updated_in": 1788
  //             },
  //             "applicant_ethnicity": {
  //               "selection": "1",
  //               "updated_at": 1711473828350,
  //               "program_updated_in": 1788
  //             },
  //             "people_in_household": {
  //               "number": 19,
  //               "updated_at": 1711473059839,
  //               "program_updated_in": 1788
  //             },
  //             "research_disclaimer": {
  //               "updated_at": 1711473131012,
  //               "program_updated_in": 1788
  //             },
  //             "applicantphonenumber": {
  //               "updated_at": 1711473354908,
  //               "country_code": "US",
  //               "phone_number": "2063456789",
  //               "program_updated_in": 1788
  //             },
  //             "food_security_concerns": {
  //               "selection": "0",
  //               "updated_at": 1711475788334,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_bucks_card_number": {
  //               "id": "11112222333344445",
  //               "updated_at": 1711473154185,
  //               "program_updated_in": 1788
  //             },
  //             "food_security_healthy_foods": {
  //               "selection": "1",
  //               "updated_at": 1711475788334,
  //               "program_updated_in": 1788
  //             },
  //             "check_different_mailing_address": {
  //               "selection": "1",
  //               "updated_at": 1711473354908,
  //               "program_updated_in": 1788
  //             },
  //             "food_security_financial_ability": {
  //               "selection": "1",
  //               "updated_at": 1711475788334,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_bucks_cbo_enrollment_info": {
  //               "updated_at": 1711472068915,
  //               "program_updated_in": 1788
  //             },
  //             "total_household_income__universal": {
  //               "updated_at": 1711473059839,
  //               "currency_cents": 1849900,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_bucks_agreementcbo_enrollment": {
  //               "selections": [
  //                 "0"
  //               ],
  //               "updated_at": 1711473131012,
  //               "program_updated_in": 1788
  //             },
  //             "intro_race_ethnicity_language_static": {
  //               "updated_at": 1711473828350,
  //               "program_updated_in": 1788
  //             },
  //             "preferred_language_for_communication": {
  //               "selection": "17",
  //               "updated_at": 1711473828350,
  //               "program_updated_in": 1788
  //             },
  //             "additional_fresh_bucks_questions_static": {
  //               "updated_at": 1711475788334,
  //               "program_updated_in": 1788
  //             },
  //             "fresh_bucks_news_and_information_selection": {
  //               "selection": "0",
  //               "updated_at": 1711473834877,
  //               "program_updated_in": 1788
  //             },
  //             "preferred_language_for_communication__other": {
  //               "text": "Test2",
  //               "updated_at": 1711473832398,
  //               "program_updated_in": 1788
  //             }
  //           }
  //         }
  //        """;
  //
  //    String insertSql =
  //        """
  //        INSERT INTO public.applicants
  //        (
  //          object
  //        )
  //        VALUES
  //        (
  //          to_jsonb(((:objectJson)::jsonb)::text)
  //        )
  //        RETURNING id;
  //        """;
  //
  //    Long id;
  //
  //    try (Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
  //      id =
  //          database
  //              .sqlQuery(insertSql)
  //              .setParameter("objectJson", objectJson)
  //              .findOne()
  //              .getLong("id");
  //
  //      transaction.commit();
  //    }
  //
  //    assertThat(id).isGreaterThan(0);
  //
  //    ConvertAddressServiceAreaToArrayJob job =
  //        new ConvertAddressServiceAreaToArrayJob(
  //            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()));
  //
  //    job.run();
  //
  //    // Load the program and verify the changes work in the context of the pojo data model
  //    ApplicantModel applicantModel =
  // database.find(ApplicantModel.class).where().idEq(id).findOne();
  //
  //    assertThat(applicantModel).isNotNull();
  //    assertThat(applicantModel.id).isEqualTo(id);
  //    assertThat(
  //            applicantModel
  //                .getApplicantData()
  //                .hasPath(Path.create("applicant.household_address.service_area")))
  //        .isTrue();
  //
  //    assertThat(
  //            applicantModel
  //                .getApplicantData()
  //                .hasPath(Path.create("applicant.household_address.service_area")))
  //        .isTrue();
  //
  //    assertThat(
  //            applicantModel
  //                .getApplicantData()
  //                .readString(
  //                    Path.create("applicant.household_address.service_area[0].serviceAreaId"))
  //                .isPresent())
  //        .isTrue();
  //  }
}
