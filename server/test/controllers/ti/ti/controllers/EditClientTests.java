package controllers.ti.ti.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;

import com.google.common.collect.ImmutableMap;
import controllers.ti.TrustedIntermediaryController;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

public class EditClientTests {
  private TrustedIntermediaryController tiController;

  @Before
  public void setup() {
    tiController = instanceOf(TrustedIntermediaryController.class);
  }

  @Test
  public void testEditClient_ReturnsNotFound() {

    AccountModel account = new AccountModel();
    account.setEmailAddress("test@ReturnsNotfound");
    account.save();
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            Helpers.fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "last",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "")));
    Http.Request request = requestBuilder.build();
    Result result = tiController.editClient(account.id, request);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
