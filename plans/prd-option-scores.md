# PRD: Optional Score on Multi-Option Question Options

## Problem Statement

CiviForm admins configuring radio button, checkbox, and dropdown questions can only define an admin ID and option text per answer option. There is no way to attach a numeric weight to an option, so programs that want to score applications (e.g. ranking or prioritizing applicants based on their answers) must do that scoring manually outside of CiviForm after exporting application data.

## Solution

Add an optional numeric score field to each answer option on radio button, checkbox, and dropdown question types, editable in the legacy (j2html) admin question create and edit pages. The score is part of the question configuration and versions with the question like any other option attribute. Each program declares via a yes/no radio option on the program create/edit form whether it uses scoring; only when a program has scoring enabled are scores applied to its applications. When an application is submitted to a scoring-enabled program, the scores of the selected options and the resulting application total are persisted atomically into the submitted application's private JSON snapshot. Admin exports surface the scores: the admin version of the application PDF shows each answer's score and a total score for the whole application at the top; the JSON/API export includes per-selection scores and an application-level total; the CSV export includes a per-question score column.

Scores are an admin-only concept. Applicants can never see score values anywhere, including in the applicant-downloadable version of the application PDF.

## User Stories

1. As a CiviForm admin, I want to enter an optional integer score next to each option when creating a radio, checkbox, or dropdown question, so that answer options carry a weight for scoring applications.
2. As a CiviForm admin, I want to add, change, or remove an option's score when editing an existing question (even though the option's admin ID is read-only), so that scoring can be tuned after the question is created.
3. As a CiviForm admin, I want options without a score to remain valid, so that scoring is opt-in per option and existing questions keep working unchanged.
4. As a CiviForm admin, I want the score saved with the question configuration when I save the question, so that the scores version and publish with the question like other option attributes.
5. As a program admin reviewing a submitted application PDF, I want each selected option's score displayed next to the answer when the option has one, so that I can see how each answer contributed.
6. As a program admin reviewing a submitted application PDF, I want a total of all score values across all questions at the top of the PDF, so that I can see the application's overall score at a glance.
7. As a program admin exporting applications to CSV, I want a score column per scored question (blank when there is no score), so that I can sort and filter applications by score in a spreadsheet.
8. As an API consumer or program admin using the JSON export, I want per-selection score values (null when an option has no score) and an application-level total, so that downstream systems can consume scores programmatically.
9. As a program admin viewing applications, I want an application's exported scores to reflect the option scores at the time the application was submitted, so that later edits to a question's scores don't retroactively change already-submitted applications.
10. As an applicant, I can never see option scores anywhere - not in the applicant-facing UI, page markup, applicant-downloadable PDF, or any applicant-accessible response - so that scoring stays an internal administrative concern.
11. As a CiviForm operator, I want the feature behind a feature flag, so that it can roll out deployment by deployment.
12. As a CiviForm admin, I want to select per program, via a yes/no radio option on the program create/edit form, whether the program uses scoring, so that scores only apply to applications for programs that actually score.
13. As a program admin viewing applications for a program with scoring set to "no", I want application PDFs and exports to contain no score information even when questions in the program have scored options, so that shared question scores don't leak into programs that don't score.
14. As a CiviForm admin migrating a program between environments, I want option scores and the program's scoring setting included in the program export JSON and applied on import, so that a scored program doesn't have to be manually re-scored in the target environment.
15. As a CiviForm admin importing a program export produced before this feature existed, I want the import to succeed with no scores and the scoring setting defaulted to "no", so that old exports remain usable.

## Implementation Decisions

### Scope
- Question types: radio button, checkbox, and dropdown only. Yes/No questions (which share the multi-option machinery but use a separate hidden-field renderer) do not get scores.
- Admin UI: the legacy j2html question create/edit pages only (`QuestionEditView` / `QuestionConfig` and the option-row template cloned by the multi-option TypeScript), plus a new program-level setting on the program create/edit form. The new Thymeleaf/North Star question pages are not touched.
- The whole feature — admin inputs (question score fields and the program setting), PDF output, JSON/CSV/API fields — is gated behind a new feature flag.
- Request-aware feature-flag state is resolved at controller boundaries and passed explicitly into form rendering, submission, exports, samples, and schema generation. Import and deserialization remain ungated so stored configuration is preserved while the feature is off.
- Applicants can never see score values, ever. No score data may appear in any applicant-facing page, response, or markup (scores must not be rendered into applicant-facing HTML/attributes even invisibly). Scores surface only in admin exports (PDF, CSV, JSON/API).
- Application PDFs use a shared exporter for admins and applicants. Score rendering in that exporter must require all of: the feature flag is on, scoring was applied when the application was submitted, and the export was requested in admin mode. Score values must not be added to shared answer-text objects used by applicant views or PDFs.
- All scoring code must use an explicit supported-type set containing only checkbox, dropdown, and radio button. It must not use a generic multi-option check because that would also include Yes/No questions.

### Program-level scoring setting
- A new boolean setting on the program, "does this program use scoring", presented as a yes/no radio button pair on the program create/edit form, following the existing eligibility-is-gating setting precedent (boolean on the program model, bound through the program form, versioned with the program definition).
- The setting is stored in a new non-null program database column with a database default of `false`. The database evolution backfills existing program rows to `false`. The program model, program definition, form, create/update services, and model/definition conversion paths all carry the value.
- Defaults to "no" for new and existing programs. The program-definition builder also explicitly defaults the value to `false` so program exports created before this feature can be deserialized.
- When "no": applications submitted to the program get no score entries in their JSON, the PDF shows no per-answer scores and no total-score line, and the JSON/API export carries null score fields. Their CSV rows have blank score cells if another represented scoring-enabled version causes score columns to exist; no score columns are generated when every represented version has scoring disabled. This applies even if the program contains questions whose options have scores because question scores are shared configuration and the program setting decides whether they are applied.
- When "yes": scoring behaves as described in the sections below.
- The setting is evaluated at submit time from the exact program version associated with the submitted application. If an existing draft points to an older program version, submission must reconcile the draft's program association before resolving scores and saving the snapshot.
- When the feature flag is off, a normal edit to an existing program preserves its stored scoring setting even though the radio input is hidden. A crafted form post cannot enable scoring while the flag is off, and a newly created program is forced to `false`.

### Score semantics
- An option score is an optional signed 32-bit integer. Negative values are allowed. Checkbox aggregates and application totals are calculated and persisted as signed 64-bit integers so valid option scores cannot overflow a 32-bit total.
- Blank input means "no score" (absent), which is distinct from an explicit 0.
- Scores are not localized and do not appear in the question translation UI.
- The score is editable on both new and existing options; unlike the option admin ID it is never read-only.

### Question configuration persistence
- The score is a new optional attribute on `QuestionOption`, which is serialized into the question row's options JSON, following the existing `displayInAnswerOptions` precedent. An optional field is backward compatible with previously stored options and requires no question-table schema migration.
- `QuestionOption`'s custom JSON creator, builder, and all factory overloads must read, default, and preserve the score. The score is not added to the applicant-facing localized option value object; admin form and scoring code resolve it from the underlying `QuestionOption` by option ID.
- The multi-option form binds mutable parallel `List<String>` score lists for existing and new options (mirroring the existing options / newOptions split). Binding as strings preserves the distinction between blank, explicit `0`, and invalid input.
- Before building options, server-side validation verifies that every parallel list has the expected cardinality and then converts each non-blank score with checked 32-bit integer parsing. Decimal, exponent, overflow, malformed, and mismatched-list submissions return a form validation error rather than throwing. The HTML input uses `type="number"` and `step="1"`, with no minimum or maximum.
- The localization-preservation path in the admin question controller carries the submitted score through both existing-option merge branches, the same way it carries `displayInAnswerOptions`. Translation-only edits preserve scores without exposing them in the translation UI.
- When the feature flag is off, question edits preserve existing scores and ignore crafted score fields; new questions cannot acquire scores. This also preserves inert scores imported while the flag was off.
- The option-row template used by the add-option TypeScript includes the score input so dynamically added rows get it without TypeScript changes beyond what row cloning already provides.

### Applicant/application JSON
- Applicant answers remain stored as option IDs. At application submission - only when both the feature flag and the submitted program version's scoring setting are on - the scores of the selected options are resolved from that exact program version and written into the application's JSON snapshot alongside the selections:
  - Single-select (radio/dropdown): one nullable `score` value per question.
  - Multi-select (checkbox): a nullable `scores` numeric list parallel to the canonicalized `selections` list, with null entries for selected options that have no score. The private application snapshot canonicalizes checkbox selections into option-definition order and removes duplicate IDs before persisting both arrays, so a selected option contributes at most once and the arrays remain aligned.
- Submission also persists an application-level `total_score` metadata value as a signed 64-bit integer. Its presence is the authoritative marker that scoring was applied. A scoring-enabled submission with no contributing scores persists `0`; applications submitted before the feature, with the flag off, or with program scoring off have no `total_score` metadata.
- The submission path creates a private copy of the applicant's live answer JSON, enriches that copy with per-question scores and `total_score`, associates it with the exact submitted program version, and saves and activates the application in the same database transaction. Score metadata is never written to the applicant's shared `ApplicantData`, and the application cannot be committed active without its complete score snapshot.
- Writing happens at submission, not at per-block save, so the values cannot go stale if a CiviForm admin adds or changes scores while an application is in draft.
- Score metadata does not participate in duplicate-application comparison. Before comparing the live applicant answers with a previous submitted snapshot, all per-question score keys and application-level scoring metadata are removed from the comparison copies, just as answer timestamps are ignored today. Checkbox selections in both comparison copies are also normalized to the same definition order with duplicate IDs removed, matching submit-time snapshot normalization.
- Score keys are reserved submission-only metadata. They may be appended to storage or API path enums where required by append-only contracts, but they must not be returned from normal question scalar sets, included in applicant question paths, exposed to predicates or API bridges, or accepted by applicant update endpoints.
- The application JSON helper and export writer gain explicit read/write support for signed 64-bit numeric values and nullable numeric arrays. Null array positions are preserved; numeric arrays are not represented with Guava immutable collections because those collections reject null elements.

### Program export/import (environment migration)
- Option scores ride along in the question definitions' option lists in program export JSON. The `QuestionOption` JSON creator and builder explicitly round-trip the optional field; exports without the field deserialize to "no score".
- The program's scoring setting is exported and imported with the program definition, like the existing eligibility-is-gating setting. It is program configuration, not environment-specific, so the export-preparation step must not strip it. The program-definition builder's explicit `false` default allows old exports without the field to import as "no".
- Duplicate-question handling on import keeps its existing semantics, with scores following the option data: overwrite adopts the imported options' scores, create-duplicate carries the imported scores on the new question, and reuse keeps the target environment's existing question (and its scores) untouched.
- Import validation accepts only a JSON integer in the signed 32-bit range or an absent/null score. Floating-point, string-coerced, and out-of-range scores are rejected. Scores on Yes/No options are rejected during import validation.
- Importing a scored program into an environment where the feature flag is off preserves the scores and the setting as stored configuration — they are simply inert (not shown in the admin UI, not applied at submit) until the flag is enabled.

### PDF export
- Per answer: in an admin PDF, when a selected option has a persisted score, the score is rendered with that option's answer text (for checkbox, per selected option line). Answers whose options have no score render exactly as today. Checkbox labels and scores are paired by the canonical selection order stored at submission.
- Top of the admin PDF: a "Total score" line reads the persisted application `total_score`. The total is the submit-time sum of all persisted per-selection scores; options without a score contribute nothing.
- The per-answer scores and total line appear only when the flag is on, the PDF is being generated in admin mode, and `total_score` is present. A scoring-enabled application where nothing contributed persists and displays `0`.
- Applicant-generated PDFs never include per-answer scores or a total, regardless of the feature flag, program setting, or stored score metadata.

### CSV export
- One new score column per scored question, adjacent to the question's existing column(s), with the score suffix in the header following the existing header format:
  - Radio/dropdown: the selected option's score.
  - Checkbox: the 64-bit sum of the selected options that have scores. A mix of scored and unscored selections returns the sum of the scored selections; explicit `0`, including scores that cancel to zero, renders as `0`.
- Blank cell when the question is unanswered, all selected options have no score, `total_score` is absent, or the application predates scoring.
- No application-level total column in the CSV.
- Column generation follows the CSV export's existing application-driven behavior: contextualized question paths come from the filtered applications, not from every stored program version. A score column is included when at least one program version represented by those applications has scoring enabled and that version of the question has at least one scored option. A checkbox score column appears once after all of that question's existing per-option columns.
- Applications submitted while scoring was off or before the feature export blank score cells even when another represented version causes the column to exist.
- Score-column creation receives an explicit export context or `includeScores` argument. Program application CSV enables it when the feature flag is on; demographic CSV always disables it.

### JSON/API export
- Single-select questions gain a nullable `score` field next to the existing `selection` field; it is null when unanswered, the selected option has no score, or scoring was not applied to the application.
- Multi-select questions gain a nullable `scores` list parallel to the existing `selections` list, emitted in the same canonical option-definition order. It is null when scoring was not applied to the application, empty when scoring was applied and the question is unanswered, and contains null entries for selected unscored options.
- The top-level application object gains a nullable 64-bit `total_score` field. It is the persisted `total_score`, including explicit `0`, and is null when the application predates scoring or was submitted while the flag or program scoring setting was off.
- Values come from the score entries persisted in the application JSON at submit time (not re-resolved from current question versions), so exports are stable across later score edits.
- When the feature flag is off, score and total properties are omitted from JSON/API responses, samples, and generated schemas. When the flag is on, the properties are present with the null/empty/value semantics above.
- The JSON export writer explicitly handles signed 64-bit values and nullable numeric lists. API samples exercise non-null, null, empty, negative, and zero values.
- OpenAPI v2 and v3 schemas define single-selection scores as nullable 32-bit integers, checkbox scores as nullable arrays whose items are nullable 32-bit integers, and total score as a nullable 64-bit integer. Score schema properties are added explicitly rather than by making scores normal applicant scalars.

## Out of Scope

- Yes/No question scoring.
- The new Thymeleaf/North Star admin question pages.
- Any applicant-facing display of scores.
- Score localization or the question translation view.
- Using scores in eligibility or visibility predicates.
- An application-level total score column in the CSV export.
- A score column in the demographic CSV export.
- Backfilling scores onto applications submitted before the feature.
- Admin UI for viewing/sorting applications by score (scores are consumed via PDF and exports only).

## Further Notes

- Exports resolve everything else about an option (admin name, text) at export time from the submit-time program version; persisting scores into the application JSON is a deliberate denormalization the user asked for, with precedent in the map question's denormalized selection attributes. Because the snapshot is written at submit time from the submit-time definitions, both approaches would agree — the stored values are authoritative for exports.
- The CSV already handles options that don't exist at a given program version (`NOT_AN_OPTION_AT_PROGRAM_VERSION`); score columns just render blank in those cases.
- If an application-level total is later wanted in the CSV, it can be added as a metadata-style column without reworking the per-question columns.
