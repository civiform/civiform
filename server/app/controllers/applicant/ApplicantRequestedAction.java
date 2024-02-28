package controllers.applicant;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;

/**
 * An enum representing what page an applicant would like to see after completing their current
 * block.
 */
public enum ApplicantRequestedAction {
  /**
   * The applicant wants to see the next block in the form.
   *
   * <p>Note that "next" can mean either the next *visible* block in the form, or the next
   * *incomplete* block in the form. See {@link
   * ApplicantProgramBlocksController#updateWithApplicantId} for more details.
   */
  NEXT_BLOCK,
  /** The applicant wants to see the previous block in the form. */
  PREVIOUS_BLOCK,
  /** The applicant wants to see the review page with all the questions. */
  REVIEW_PAGE;

  public static final ApplicantRequestedAction DEFAULT_ACTION = NEXT_BLOCK;

  /**
   * Removes the applicant-requested action from the end of the path in the provided {@code uri} and
   * returns the stripped URL, or returns the original URL if it doesn't end in an
   * applicant-requested action.
   *
   * @param url the url to strip. Must represent a legal URI.
   */
  public static String stripActionFromEndOfUrl(String url) {
    final String path = URI.create(url).getPath();
    for (ApplicantRequestedAction action : ApplicantRequestedAction.values()) {
      String actionString = "/" + action.name();
      if (path.endsWith(actionString)) {
        try {
          String strippedPath = path.substring(0, path.indexOf(actionString));
          return new URIBuilder(url).setPath(strippedPath).build().toString();
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return url;
  }
}
