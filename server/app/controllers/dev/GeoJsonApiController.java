package controllers.dev;

import static j2html.TagCreator.div;
import static play.mvc.Results.ok;

import auth.Authorizers;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.geojson.GeoJsonClient;
import services.geojson.GeoJsonProcessingException;

public final class GeoJsonApiController {
  private static final Logger logger = LoggerFactory.getLogger(GeoJsonApiController.class);

  private final FormFactory formFactory;
  private final GeoJsonClient geoJsonClient;

  @Inject
  GeoJsonApiController(FormFactory formFactory, GeoJsonClient geoJsonClient) {
    this.formFactory = formFactory;
    this.geoJsonClient = geoJsonClient;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxGetData(Http.Request request) {
    DynamicForm formData = formFactory.form().bindFromRequest(request);
    String geoJsonEndpoint = formData.get("geoJsonEndpoint");

    return geoJsonClient
        .fetchGeoJson(geoJsonEndpoint)
        .thenApplyAsync(
            geoJsonResponse -> {
              // TODO(#11001): Parse GeoJSON upon response to populate question settings.
              return ok(div("Success!").toString()).as(Http.MimeTypes.HTML);
            })
        .exceptionally(
            ex -> {
              logger.error("An error occurred trying to retrieve GeoJSON", ex);
              String errorMessage = "An error occurred trying to retrieve GeoJSON";
              if (ex.getCause() instanceof GeoJsonProcessingException) {
                errorMessage += ": " + ex.getCause().getMessage();
              }

              // TODO(#11125): Implement error state.
              return ok(div(errorMessage).withClass("text-red-500").toString())
                  .as(Http.MimeTypes.HTML);
            });
  }
}
