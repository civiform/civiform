package services.applicant;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ApplicantService_ApplicationPrograms
    extends ApplicantService.ApplicationPrograms {

  private final Optional<ApplicantService.ApplicantProgramData> commonIntakeForm;

  private final ImmutableList<ApplicantService.ApplicantProgramData> inProgress;

  private final ImmutableList<ApplicantService.ApplicantProgramData> submitted;

  private final ImmutableList<ApplicantService.ApplicantProgramData> unapplied;

  private AutoValue_ApplicantService_ApplicationPrograms(
      Optional<ApplicantService.ApplicantProgramData> commonIntakeForm,
      ImmutableList<ApplicantService.ApplicantProgramData> inProgress,
      ImmutableList<ApplicantService.ApplicantProgramData> submitted,
      ImmutableList<ApplicantService.ApplicantProgramData> unapplied) {
    this.commonIntakeForm = commonIntakeForm;
    this.inProgress = inProgress;
    this.submitted = submitted;
    this.unapplied = unapplied;
  }

  @Override
  public Optional<ApplicantService.ApplicantProgramData> commonIntakeForm() {
    return commonIntakeForm;
  }

  @Override
  public ImmutableList<ApplicantService.ApplicantProgramData> inProgress() {
    return inProgress;
  }

  @Override
  public ImmutableList<ApplicantService.ApplicantProgramData> submitted() {
    return submitted;
  }

  @Override
  public ImmutableList<ApplicantService.ApplicantProgramData> unapplied() {
    return unapplied;
  }

  @Override
  public String toString() {
    return "ApplicationPrograms{"
        + "commonIntakeForm="
        + commonIntakeForm
        + ", "
        + "inProgress="
        + inProgress
        + ", "
        + "submitted="
        + submitted
        + ", "
        + "unapplied="
        + unapplied
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ApplicantService.ApplicationPrograms) {
      ApplicantService.ApplicationPrograms that = (ApplicantService.ApplicationPrograms) o;
      return this.commonIntakeForm.equals(that.commonIntakeForm())
          && this.inProgress.equals(that.inProgress())
          && this.submitted.equals(that.submitted())
          && this.unapplied.equals(that.unapplied());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= commonIntakeForm.hashCode();
    h$ *= 1000003;
    h$ ^= inProgress.hashCode();
    h$ *= 1000003;
    h$ ^= submitted.hashCode();
    h$ *= 1000003;
    h$ ^= unapplied.hashCode();
    return h$;
  }

  static final class Builder extends ApplicantService.ApplicationPrograms.Builder {
    private Optional<ApplicantService.ApplicantProgramData> commonIntakeForm = Optional.empty();
    private ImmutableList<ApplicantService.ApplicantProgramData> inProgress;
    private ImmutableList<ApplicantService.ApplicantProgramData> submitted;
    private ImmutableList<ApplicantService.ApplicantProgramData> unapplied;

    Builder() {}

    @Override
    ApplicantService.ApplicationPrograms.Builder setCommonIntakeForm(
        ApplicantService.ApplicantProgramData commonIntakeForm) {
      this.commonIntakeForm = Optional.of(commonIntakeForm);
      return this;
    }

    @Override
    ApplicantService.ApplicationPrograms.Builder setInProgress(
        ImmutableList<ApplicantService.ApplicantProgramData> inProgress) {
      if (inProgress == null) {
        throw new NullPointerException("Null inProgress");
      }
      this.inProgress = inProgress;
      return this;
    }

    @Override
    ApplicantService.ApplicationPrograms.Builder setSubmitted(
        ImmutableList<ApplicantService.ApplicantProgramData> submitted) {
      if (submitted == null) {
        throw new NullPointerException("Null submitted");
      }
      this.submitted = submitted;
      return this;
    }

    @Override
    ApplicantService.ApplicationPrograms.Builder setUnapplied(
        ImmutableList<ApplicantService.ApplicantProgramData> unapplied) {
      if (unapplied == null) {
        throw new NullPointerException("Null unapplied");
      }
      this.unapplied = unapplied;
      return this;
    }

    @Override
    ApplicantService.ApplicationPrograms build() {
      if (this.inProgress == null || this.submitted == null || this.unapplied == null) {
        StringBuilder missing = new StringBuilder();
        if (this.inProgress == null) {
          missing.append(" inProgress");
        }
        if (this.submitted == null) {
          missing.append(" submitted");
        }
        if (this.unapplied == null) {
          missing.append(" unapplied");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ApplicantService_ApplicationPrograms(
          this.commonIntakeForm, this.inProgress, this.submitted, this.unapplied);
    }
  }
}
