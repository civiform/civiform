package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import java.util.List;
import views.style.StyleUtils;

/** Contains methods for rendering shared question bank UI components. */
public final class QuestionBank {

  public static final DivTag renderFilterAndSort(List<QuestionSortOption> sortOptions) {
    return div()
        .withClasses("flex", "items-end", "mb-2")
        .with(renderFilter(), renderQuestionSortSelect(sortOptions));
  }

  private static final DivTag renderFilter() {
    InputTag filterInput =
        input()
            .withId("question-bank-filter")
            .withType("text")
            .withName("questionFilter")
            .withPlaceholder("Search questions")
            .withClasses(
                "h-10",
                "px-10",
                "pr-5",
                "w-full",
                "rounded-full",
                "text-sm",
                "border",
                "border-gray-200",
                "shadow",
                StyleUtils.focus("outline-none"));

    SvgTag filterIcon = Icons.svg(Icons.SEARCH).withClasses("h-4", "w-4");
    DivTag filterIconDiv = div().withClasses("absolute", "ml-4", "mt-3", "mr-4").with(filterIcon);
    return div().withClasses("flex-grow", "mr-4", "relative").with(filterIconDiv, filterInput);
  }

  /**
   * Creates a dropdown containing options for how the questions should be sorted.
   *
   * @param sortOptions List of QuestionSortOption types to include in the dropdown. Options are
   *     displayed in the order provided, with the first option being used as the default displayed.
   *     Clients are responsible for ensuring that the default matches the order the questions are
   *     initially displayed in.
   * @return DivTag containing a SelectTag with the options for sorting questions.
   */
  private static final DivTag renderQuestionSortSelect(List<QuestionSortOption> sortOptions) {
    ImmutableList<SelectWithLabel.OptionValue> questionSortOptions =
        sortOptions.stream()
            .flatMap(sortOption -> sortOption.getSelectOptions().stream())
            .collect(ImmutableList.toImmutableList());

    SelectWithLabel questionSortSelect =
        new SelectWithLabel()
            .setId("question-bank-sort")
            .setValue(questionSortOptions.get(0).value()) // Default sort order.
            .setLabelText("Sort by:")
            .setOptionGroups(
                ImmutableList.of(
                    SelectWithLabel.OptionGroup.builder()
                        .setLabel("Sort by:")
                        .setOptions(questionSortOptions)
                        .build()));
    return questionSortSelect.getSelectTag().withClass("mb-0");
  }
}
