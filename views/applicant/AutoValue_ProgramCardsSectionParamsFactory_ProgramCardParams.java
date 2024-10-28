package views.applicant;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ProgramCardsSectionParamsFactory_ProgramCardParams extends ProgramCardsSectionParamsFactory.ProgramCardParams {

  private final String title;

  private final String actionText;

  private final String body;

  private final String detailsUrl;

  private final String actionUrl;

  private final boolean isGuest;

  private final Optional<String> loginModalId;

  private final Optional<Boolean> eligible;

  private final Optional<String> eligibilityMessage;

  private final Optional<String> applicationStatus;

  private final Optional<String> imageSourceUrl;

  private final Optional<String> altText;

  private final ImmutableList<String> categories;

  private AutoValue_ProgramCardsSectionParamsFactory_ProgramCardParams(
      String title,
      String actionText,
      String body,
      String detailsUrl,
      String actionUrl,
      boolean isGuest,
      Optional<String> loginModalId,
      Optional<Boolean> eligible,
      Optional<String> eligibilityMessage,
      Optional<String> applicationStatus,
      Optional<String> imageSourceUrl,
      Optional<String> altText,
      ImmutableList<String> categories) {
    this.title = title;
    this.actionText = actionText;
    this.body = body;
    this.detailsUrl = detailsUrl;
    this.actionUrl = actionUrl;
    this.isGuest = isGuest;
    this.loginModalId = loginModalId;
    this.eligible = eligible;
    this.eligibilityMessage = eligibilityMessage;
    this.applicationStatus = applicationStatus;
    this.imageSourceUrl = imageSourceUrl;
    this.altText = altText;
    this.categories = categories;
  }

  @Override
  public String title() {
    return title;
  }

  @Override
  public String actionText() {
    return actionText;
  }

  @Override
  public String body() {
    return body;
  }

  @Override
  public String detailsUrl() {
    return detailsUrl;
  }

  @Override
  public String actionUrl() {
    return actionUrl;
  }

  @Override
  public boolean isGuest() {
    return isGuest;
  }

  @Override
  public Optional<String> loginModalId() {
    return loginModalId;
  }

  @Override
  public Optional<Boolean> eligible() {
    return eligible;
  }

  @Override
  public Optional<String> eligibilityMessage() {
    return eligibilityMessage;
  }

  @Override
  public Optional<String> applicationStatus() {
    return applicationStatus;
  }

  @Override
  public Optional<String> imageSourceUrl() {
    return imageSourceUrl;
  }

  @Override
  public Optional<String> altText() {
    return altText;
  }

  @Override
  public ImmutableList<String> categories() {
    return categories;
  }

  @Override
  public String toString() {
    return "ProgramCardParams{"
        + "title=" + title + ", "
        + "actionText=" + actionText + ", "
        + "body=" + body + ", "
        + "detailsUrl=" + detailsUrl + ", "
        + "actionUrl=" + actionUrl + ", "
        + "isGuest=" + isGuest + ", "
        + "loginModalId=" + loginModalId + ", "
        + "eligible=" + eligible + ", "
        + "eligibilityMessage=" + eligibilityMessage + ", "
        + "applicationStatus=" + applicationStatus + ", "
        + "imageSourceUrl=" + imageSourceUrl + ", "
        + "altText=" + altText + ", "
        + "categories=" + categories
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ProgramCardsSectionParamsFactory.ProgramCardParams) {
      ProgramCardsSectionParamsFactory.ProgramCardParams that = (ProgramCardsSectionParamsFactory.ProgramCardParams) o;
      return this.title.equals(that.title())
          && this.actionText.equals(that.actionText())
          && this.body.equals(that.body())
          && this.detailsUrl.equals(that.detailsUrl())
          && this.actionUrl.equals(that.actionUrl())
          && this.isGuest == that.isGuest()
          && this.loginModalId.equals(that.loginModalId())
          && this.eligible.equals(that.eligible())
          && this.eligibilityMessage.equals(that.eligibilityMessage())
          && this.applicationStatus.equals(that.applicationStatus())
          && this.imageSourceUrl.equals(that.imageSourceUrl())
          && this.altText.equals(that.altText())
          && this.categories.equals(that.categories());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= title.hashCode();
    h$ *= 1000003;
    h$ ^= actionText.hashCode();
    h$ *= 1000003;
    h$ ^= body.hashCode();
    h$ *= 1000003;
    h$ ^= detailsUrl.hashCode();
    h$ *= 1000003;
    h$ ^= actionUrl.hashCode();
    h$ *= 1000003;
    h$ ^= isGuest ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= loginModalId.hashCode();
    h$ *= 1000003;
    h$ ^= eligible.hashCode();
    h$ *= 1000003;
    h$ ^= eligibilityMessage.hashCode();
    h$ *= 1000003;
    h$ ^= applicationStatus.hashCode();
    h$ *= 1000003;
    h$ ^= imageSourceUrl.hashCode();
    h$ *= 1000003;
    h$ ^= altText.hashCode();
    h$ *= 1000003;
    h$ ^= categories.hashCode();
    return h$;
  }

  @Override
  public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends ProgramCardsSectionParamsFactory.ProgramCardParams.Builder {
    private String title;
    private String actionText;
    private String body;
    private String detailsUrl;
    private String actionUrl;
    private boolean isGuest;
    private Optional<String> loginModalId = Optional.empty();
    private Optional<Boolean> eligible = Optional.empty();
    private Optional<String> eligibilityMessage = Optional.empty();
    private Optional<String> applicationStatus = Optional.empty();
    private Optional<String> imageSourceUrl = Optional.empty();
    private Optional<String> altText = Optional.empty();
    private ImmutableList<String> categories;
    private byte set$0;
    Builder() {
    }
    private Builder(ProgramCardsSectionParamsFactory.ProgramCardParams source) {
      this.title = source.title();
      this.actionText = source.actionText();
      this.body = source.body();
      this.detailsUrl = source.detailsUrl();
      this.actionUrl = source.actionUrl();
      this.isGuest = source.isGuest();
      this.loginModalId = source.loginModalId();
      this.eligible = source.eligible();
      this.eligibilityMessage = source.eligibilityMessage();
      this.applicationStatus = source.applicationStatus();
      this.imageSourceUrl = source.imageSourceUrl();
      this.altText = source.altText();
      this.categories = source.categories();
      set$0 = (byte) 1;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setTitle(String title) {
      if (title == null) {
        throw new NullPointerException("Null title");
      }
      this.title = title;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setActionText(String actionText) {
      if (actionText == null) {
        throw new NullPointerException("Null actionText");
      }
      this.actionText = actionText;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setBody(String body) {
      if (body == null) {
        throw new NullPointerException("Null body");
      }
      this.body = body;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setDetailsUrl(String detailsUrl) {
      if (detailsUrl == null) {
        throw new NullPointerException("Null detailsUrl");
      }
      this.detailsUrl = detailsUrl;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setActionUrl(String actionUrl) {
      if (actionUrl == null) {
        throw new NullPointerException("Null actionUrl");
      }
      this.actionUrl = actionUrl;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setIsGuest(Boolean isGuest) {
      if (isGuest == null) {
        throw new NullPointerException("Null isGuest");
      }
      this.isGuest = isGuest;
      set$0 |= (byte) 1;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setLoginModalId(String loginModalId) {
      this.loginModalId = Optional.of(loginModalId);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setEligible(Boolean eligible) {
      this.eligible = Optional.of(eligible);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setEligibilityMessage(String eligibilityMessage) {
      this.eligibilityMessage = Optional.of(eligibilityMessage);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setApplicationStatus(String applicationStatus) {
      this.applicationStatus = Optional.of(applicationStatus);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setImageSourceUrl(String imageSourceUrl) {
      this.imageSourceUrl = Optional.of(imageSourceUrl);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setAltText(String altText) {
      this.altText = Optional.of(altText);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams.Builder setCategories(ImmutableList<String> categories) {
      if (categories == null) {
        throw new NullPointerException("Null categories");
      }
      this.categories = categories;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramCardParams build() {
      if (set$0 != 1
          || this.title == null
          || this.actionText == null
          || this.body == null
          || this.detailsUrl == null
          || this.actionUrl == null
          || this.categories == null) {
        StringBuilder missing = new StringBuilder();
        if (this.title == null) {
          missing.append(" title");
        }
        if (this.actionText == null) {
          missing.append(" actionText");
        }
        if (this.body == null) {
          missing.append(" body");
        }
        if (this.detailsUrl == null) {
          missing.append(" detailsUrl");
        }
        if (this.actionUrl == null) {
          missing.append(" actionUrl");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" isGuest");
        }
        if (this.categories == null) {
          missing.append(" categories");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ProgramCardsSectionParamsFactory_ProgramCardParams(
          this.title,
          this.actionText,
          this.body,
          this.detailsUrl,
          this.actionUrl,
          this.isGuest,
          this.loginModalId,
          this.eligible,
          this.eligibilityMessage,
          this.applicationStatus,
          this.imageSourceUrl,
          this.altText,
          this.categories);
    }
  }

}
