package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.option;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.time.LocalDate;
import java.util.Optional;
import play.i18n.Messages;
import services.applicant.ValidationErrorMessage;

public class MemorableDateFieldWithLabel extends FieldWithLabel {
  private String dayQuery = "";
  private String monthQuery = "";
  private String yearQuery = "";

  public MemorableDateFieldWithLabel setLegend(String legend) {
    this.legend = legend;
    return this;
  }

  private String legend = "";

  public void setDayQuery(String dayQuery) {
    this.dayQuery = dayQuery;
  }

  public void setMonthQuery(String monthQuery) {
    this.monthQuery = monthQuery;
  }

  public void setYearQuery(String yearQuery) {
    this.yearQuery = yearQuery;
  }

  @Override
  public MemorableDateFieldWithLabel setFieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    super.setFieldErrors(messages, errors);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel addStyleClass(String styleClass) {
    super.addStyleClass(styleClass);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setFieldName(String fieldName) {
    super.setFieldName(fieldName);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setFormId(String formId) {
    super.setFormId(formId);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setId(String fieldId) {
    super.setId(fieldId);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setLabelText(String labelText) {
    super.setLabelText(labelText);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setPlaceholderText(String placeholder) {
    super.setPlaceholderText(placeholder);
    return this;
  }

  public MemorableDateFieldWithLabel setValue(
      String dayQuery, String monthQuery, String yearQuery) {
    this.dayQuery = dayQuery;
    this.monthQuery = monthQuery;
    this.yearQuery = yearQuery;
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setRequired(boolean isRequired) {
    super.setRequired(isRequired);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setAriaRequired(boolean isRequired) {
    super.setAriaRequired(isRequired);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel setAriaDescribedByIds(
      ImmutableList<String> ariaDescribedByIds) {
    super.setAriaDescribedByIds(ariaDescribedByIds);
    return this;
  }

  @Override
  public MemorableDateFieldWithLabel forceAriaInvalid() {
    super.forceAriaInvalid();
    return this;
  }

  public MemorableDateFieldWithLabel setLocalDateValue(Optional<LocalDate> date) {
    this.dayQuery = date.isPresent() ? String.valueOf(date.get().getDayOfMonth()) : "";
    this.monthQuery = date.isPresent() ? String.valueOf(date.get().getMonthValue()) : "";
    this.yearQuery = date.isPresent() ? String.valueOf(date.get().getYear()) : "";

    return this;
  }

  //  /**
  //   * Creates a USWDS Memorable Date component. This is to be used in place of a date picker
  // anytime
  //   * that the date is well-defined, such as a date of birth.
  //   * https://designsystem.digital.gov/components/memorable-date/
  //   *
  ////   * @param dayValue The default value which should appear in the "Day" input field
  ////   * @param monthValue The default option which should be selected in the "Month" dropdown
  ////   * @param yearValue The default value which should appear in the "Year" input field
  ////   * @param legend The label string for the date fields
  ////   * @param showError Whether an error message should appear
  //   * @return ContainerTag
  //   */
  public FieldsetTag makeMemorableDate() {
    // boolean showAdditionalError = errorMessage.isPresent();
    FieldsetTag dateFieldset =
        fieldset()
            .withClass("usa-fieldset")
            .with(
                legend(label(legend)).withClass("usa-legend"),
                span("For example: January 28 1986").withClass("usa-hint").withId("mdHint"),
                //          div()
                //            .condWith(showError, span("Error: Please enter month, day and year."))
                //            .withClasses("text-red-600 text-xs")
                //            .withId("memorable_date_error"),
                //          div()
                //            .condWith(showAdditionalError, span(errorMessage.orElse("")))
                //            .withClasses("text-red-600 text-xs")
                //            .withId("memorable_date_error"),
                div()
                    .withClasses("usa-memorable-date")
                    .with(getSelectFormGroup(), getDayFormGroup(), getYearFormGroup()));

    return dateFieldset;
  }

  /* Helper function for the Memorable Date */
  private DivTag getDayFormGroup() {

    InputTag dayTag = TagCreator.input();
    dayTag.withValue(dayQuery);
    dayTag.withClasses("usa-input");
    dayTag.withId("date_of_birth_day");
    dayTag.withName("dayQuery");
    dayTag.attr("aria-describedby", "mdHint");
    dayTag.attr("inputmode", "numeric");
    dayTag.withMaxlength("2");
    dayTag.withPattern("[0-9]*");
    return div()
        .withClass("usa-form-group usa-form-group--day")
        .with(
            label("Day").withClass("usa-label").withFor("date_of_birth_day"),
            applyAttrsAndGenLabel(dayTag));

    //    return div()
    //      .withClass("usa-form-group usa-form-group--day")
    //      .with(
    //        label("Day").withClass("usa-label").withFor("date_of_birth_day"),
    //        input()
    //          .addStyleClass("usa-input")
    //          //.withCondClass(dayQuery=="", "usa-input--error mt-2.5")
    //          .withId("date_of_birth_day")
    //          .withName("dayQuery")
    //          .attr("aria-describedby", "mdHint")
    //          .attr("inputmode", "numeric")
    //          .withMaxlength("2")
    //          .withPattern("[0-9]*")
    //          .withValue(dayQuery));
  }

  /* Helper function for the Memorable Date */
  private DivTag getYearFormGroup() {
    InputTag yearTag = TagCreator.input();
    yearTag.withValue(yearQuery);
    yearTag.withClass("usa-input");
    yearTag.withId("date_of_birth_year");
    yearTag.attr("aria-describedby", "mdHint");
    yearTag.attr("minlength", "4");
    yearTag.attr("inputmode", "numeric");
    yearTag.withName("yearQuery");
    yearTag.withMaxlength("4");
    yearTag.withPattern("[0-9]*");

    return div()
        .withClass("usa-form-group usa-form-group--year")
        .with(
            label("Year").withClass("usa-label").withFor("date_of_birth_year"),
            applyAttrsAndGenLabel(yearTag));
    //        input()
    //          .withClass("usa-input")
    //          .withCondClass(yearQuery =="", "usa-input--error mt-2.5")
    //          .withId("date_of_birth_year")
    //          .withName("yearQuery")
    //          .attr("aria-describedby", "mdHint")
    //          .attr("minlength", "4")
    //          .attr("inputmode", "numeric")
    //          .withMaxlength("4")
    //          .withPattern("[0-9]*")
    //          .withValue(yearQuery));
  }

  /* Helper function for the Memorable Date */
  private DivTag getSelectFormGroup() {
    SelectTag monthTag = TagCreator.select();
    OptionTag janOption = option("01 - January").withValue("01");
    OptionTag FebOption = option("02 - February").withValue("02");
    monthTag.with(janOption);
    monthTag.with(FebOption);

    //    monthTag.with(
    ////      option()
    ////        .withValue("")
    ////        .withText("- Select -")
    ////        .withCondSelected(monthQuery.equals("")),
    //      option()
    //        .withValue("01")
    //        .withText("01 - January")
    //        .withCondSelected(monthQuery.equals("01")),
    //      option()
    //        .withValue("02")
    //        .withText("02 - February")
    //        .withCondSelected(monthQuery.equals("02")),
    //      option()
    //        .withValue("03")
    //        .withText("03 - March")
    //        .withCondSelected(monthQuery.equals("03")),
    //      option()
    //        .withValue("04")
    //        .withText("04 - April")
    //        .withCondSelected(monthQuery.equals("04")),
    //      option()
    //        .withValue("05")
    //        .withText("05 - May")
    //        .withCondSelected(monthQuery.equals("05")),
    //      option()
    //        .withValue("06")
    //        .withText("06 - June")
    //        .withCondSelected(monthQuery.equals("06")),
    //      option()
    //        .withValue("07")
    //        .withText("07 - July")
    //        .withCondSelected(monthQuery.equals("07")),
    //      option()
    //        .withValue("08")
    //        .withText("08 - August")
    //        .withCondSelected(monthQuery.equals("08")),
    //      option()
    //        .withValue("09")
    //        .withText("09 - September")
    //        .withCondSelected(monthQuery.equals("09")),
    //      option()
    //        .withValue("10")
    //        .withText("10 - October")
    //        .withCondSelected(monthQuery.equals("10")),
    //      option()
    //        .withValue("11")
    //        .withText("11 - November")
    //        .withCondSelected(monthQuery.equals("11")),
    //      option()
    //        .withValue("12")
    //        .withText("12 - December")
    //        .withCondSelected(monthQuery.equals("12")));
    monthTag.withClass("usa-select");
    monthTag.withId("date_of_birth_month");
    monthTag.withName("monthQuery");
    monthTag.attr("aria-describedby", "mdHint");
    System.out.println("Month query = " + monthQuery);
    if (monthQuery.equals("")) {
      janOption.isSelected();
    }
    if (monthQuery.equals("02")) {
      FebOption.isSelected();
    }

    return div()
        .withClass("usa-form-group usa-form-group--month usa-form-group--select")
        .with(
            label("Month").withClass("usa-label").withFor("date_of_birth_month"),
            applyAttrsAndGenLabel(monthTag));
  }

  //    return div()
  //      .withClass("usa-form-group usa-form-group--month usa-form-group--select")
  //      .with(
  //        label("Month").withClass("usa-label").withFor("date_of_birth_month"),
  //        select()
  //          .withClass("usa-select")
  //          .withCondClass(monthQuery=="", "usa-input--error mt-2.5 py-1")
  //          .withId("date_of_birth_month")
  //          .withName("monthQuery")
  //          .attr("aria-describedby", "mdHint")
  //          .with(
  //            option()
  //              .withValue("")
  //              .withText("- Select -")
  //              .withCondSelected(monthQuery.equals("")),
  //            option()
  //              .withValue("01")
  //              .withText("01 - January")
  //              .withCondSelected(monthQuery.equals("01")),
  //            option()
  //              .withValue("02")
  //              .withText("02 - February")
  //              .withCondSelected(monthQuery.equals("02")),
  //            option()
  //              .withValue("03")
  //              .withText("03 - March")
  //              .withCondSelected(monthQuery.equals("03")),
  //            option()
  //              .withValue("04")
  //              .withText("04 - April")
  //              .withCondSelected(monthQuery.equals("04")),
  //            option()
  //              .withValue("05")
  //              .withText("05 - May")
  //              .withCondSelected(monthQuery.equals("05")),
  //            option()
  //              .withValue("06")
  //              .withText("06 - June")
  //              .withCondSelected(monthQuery.equals("06")),
  //            option()
  //              .withValue("07")
  //              .withText("07 - July")
  //              .withCondSelected(monthQuery.equals("07")),
  //            option()
  //              .withValue("08")
  //              .withText("08 - August")
  //              .withCondSelected(monthQuery.equals("08")),
  //            option()
  //              .withValue("09")
  //              .withText("09 - September")
  //              .withCondSelected(monthQuery.equals("09")),
  //            option()
  //              .withValue("10")
  //              .withText("10 - October")
  //              .withCondSelected(monthQuery.equals("10")),
  //            option()
  //              .withValue("11")
  //              .withText("11 - November")
  //              .withCondSelected(monthQuery.equals("11")),
  //            option()
  //              .withValue("12")
  //              .withText("12 - December")
  //              .withCondSelected(monthQuery.equals("12"))));
  //  }
}
