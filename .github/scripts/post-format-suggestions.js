// Posts the fixes that bin/fmt and bin/fmt-sbt left in the working tree
// back onto the pull request as one-click suggested-change review
// comments. Invoked from .github/workflows/format.yaml via
// actions/github-script. The working tree is left untouched, so the
// diff check that follows still fails.

// GitHub rejects comment bodies over this size, and one bad comment
// rejects the whole review.
const MAX_BODY_LENGTH = 65536

// The review API caps out well before this, and a wall of suggestions
// stops being helpful anyway; the review body discloses the rest.
const MAX_COMMENTS = 50

// Parses `git diff -U0` output into {path, hunks} entries, where each
// hunk carries its old-file start line and count plus the replacement
// (lines) and replaced (oldLines) content. Zero context lines matter:
// each suggestion must anchor on exactly the lines it replaces. Any
// wider range sweeps in the PR diff's deleted lines, and GitHub refuses
// to apply suggestions whose range includes deleted lines.
function parseDiff(diff) {
  const files = []
  let file = null
  let inHeader = false
  for (const line of diff.split('\n')) {
    // With -U0 every content line starts with '+', '-', or '\', so a
    // bare 'diff --git ' line is always a real file boundary.
    // '+++ b/...' alone is not: an added content line beginning
    // '++ b/...' renders exactly the same way, so header lines only
    // count between 'diff --git ' and the file's first '@@ '.
    if (line.startsWith('diff --git ')) {
      inHeader = true
      file = null
    } else if (inHeader && line.startsWith('+++ b/')) {
      // Headers for paths with special characters arrive quoted
      // ('+++ "b/..."') and fail this match, skipping the file; the
      // API couldn't anchor on the quoted form anyway. A path
      // containing spaces carries a literal trailing tab.
      file = {path: line.slice(6).replace(/\t$/, ''), hunks: []}
      files.push(file)
    } else if (line.startsWith('@@ ')) {
      inHeader = false
      if (file) {
        const match = line.match(/^@@ -(\d+)(?:,(\d+))? /)
        file.hunks.push({
          start: Number(match[1]),
          count: match[2] === undefined ? 1 : Number(match[2]),
          lines: [],
          oldLines: [],
        })
      }
    } else if (file && !inHeader && file.hunks.length > 0) {
      const hunk = file.hunks[file.hunks.length - 1]
      if (line.startsWith('+')) {
        hunk.lines.push(line.slice(1))
      } else if (line.startsWith('-')) {
        hunk.oldLines.push(line.slice(1))
      }
    }
  }
  return files
}

// Turns parsed hunks into createReview comment payloads. Returns the
// comments plus a count of fixes that exist in the diff but can't be
// expressed as an applyable suggestion.
function buildComments(files) {
  const comments = []
  let skipped = 0
  for (const {path, hunks} of files) {
    for (const {start, count, lines, oldLines} of hunks) {
      // A pure insertion has no existing line to anchor on, and a hunk
      // whose only change is the trailing-newline marker yields a
      // suggestion identical to the existing text, which GitHub can't
      // apply.
      if (count === 0 || lines.join('\n') === oldLines.join('\n')) {
        skipped++
        continue
      }
      // The fence must be longer than any backtick run in the content,
      // or a ``` line (common in Markdown files) would close the
      // suggestion block early. No replacement lines means delete the
      // range; an empty suggestion block does that, a blank line would
      // not.
      const runs = lines.join('\n').match(/`+/g) || []
      const fence = '`'.repeat(
        runs.reduce((max, run) => Math.max(max, run.length + 1), 3),
      )
      const body =
        fence + 'suggestion\n' + lines.map((l) => l + '\n').join('') + fence
      if (body.length > MAX_BODY_LENGTH) {
        skipped++
        continue
      }
      comments.push({
        path,
        line: start + count - 1,
        side: 'RIGHT',
        ...(count > 1 && {start_line: start, start_side: 'RIGHT'}),
        body,
      })
    }
  }
  return {comments, skipped}
}

async function postSuggestions({github, context, core, exec}) {
  const {stdout: diff} = await exec.getExecOutput('git', ['diff', '-U0'], {
    silent: true,
  })
  if (!diff.trim()) return

  const {comments, skipped} = buildComments(parseDiff(diff))
  if (comments.length === 0 && skipped === 0) return

  const capped = comments.slice(0, MAX_COMMENTS)
  const pr = context.payload.pull_request
  const parts = []
  if (capped.length > 0) {
    parts.push(
      'bin/fmt would make these changes. Apply them with "Commit suggestion", or run bin/fmt locally.',
    )
    if (comments.length > MAX_COMMENTS) {
      parts.push(`(Showing ${MAX_COMMENTS} of ${comments.length} fixes.)`)
    }
    if (skipped > 0) {
      parts.push(
        `${skipped} more can't be shown as suggestions; the failing check's diff has everything.`,
      )
    }
  } else {
    parts.push(
      "bin/fmt would make changes that can't be shown as suggestions; run bin/fmt locally or see the failing check's diff.",
    )
  }
  try {
    await github.rest.pulls.createReview({
      owner: context.repo.owner,
      repo: context.repo.repo,
      pull_number: pr.number,
      commit_id: pr.head.sha,
      event: 'COMMENT',
      body: parts.join(' '),
      ...(capped.length > 0 && {comments: capped}),
    })
  } catch (error) {
    // A 422 means GitHub refused a specific comment, usually a hunk
    // outside the PR's own diff, which rejects the whole review; retry
    // one comment at a time and drop the ones GitHub refuses. Anything
    // else (rate limit, permissions, network) would doom the
    // per-comment retries too, so don't compound the failure with up to
    // 50 more calls.
    if (error.status !== 422) {
      core.warning(`Could not post formatter suggestions: ${error.message}`)
      return
    }
    let posted = 0
    for (const comment of capped) {
      try {
        await github.rest.pulls.createReviewComment({
          owner: context.repo.owner,
          repo: context.repo.repo,
          pull_number: pr.number,
          commit_id: pr.head.sha,
          ...comment,
        })
        posted++
      } catch (err) {
        // A refused comment sits on lines the PR didn't touch; the
        // diff step's output still shows it. Any other failure applies
        // to the remaining calls too.
        if (err.status !== 422) break
      }
    }
    core.warning(
      `Posted ${posted} of ${capped.length} formatter suggestions individually: ${error.message}`,
    )
  }
}

module.exports = {parseDiff, buildComments, postSuggestions}
