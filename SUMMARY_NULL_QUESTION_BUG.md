# Summary: NULL_QUESTION Cache Bug Investigation

## What You Asked For
You wanted to understand how to **reproduce** the NULL_QUESTION cache bug locally, NOT to fix it. This investigation provides:
1. ✅ Root cause analysis
2. ✅ Detailed reproduction steps  
3. ✅ Technical deep-dive into the bug mechanism
4. ❌ NO pull request with fixes (as requested)

## Quick Answer: How to Reproduce

### Prerequisites
- Two CiviForm servers running locally (connected to same database)
- Cache enabled (default setting)

### Reproduction Steps (Simplified)

1. **Server A & B**: Create and publish a program with 3 questions → **Version 1 becomes ACTIVE**
2. **Server A & B**: Access the program → **Both servers cache Version 1**
3. **Server A**: Create new draft, add 4th question, publish → **Version 2 becomes ACTIVE**
   - ⚠️ **Bug**: Cache is NOT invalidated on either server
4. **Server B**: Try to export program data or access program details
   - **Expected**: Load fresh data from Version 2
   - **Actual**: Uses stale cached data from Version 1
   - **Result**: `NULL_QUESTION` error when question 4 isn't found

### Even Simpler: Debugger Method

1. Create program with questions, publish (Version 1)
2. Access program → caches Version 1 questions
3. Add new question, publish (Version 2)
4. Set breakpoint at `VersionRepository.java:578`
5. When code requests Version 2 questions, use debugger to force it to return Version 1 cached data
6. Try to export → NULL_QUESTION error

## Root Cause

**Location**: `server/app/repository/VersionRepository.java`

**The Problem**:
```java
// Lines 574-581: getQuestionsForVersion()
public ImmutableList<QuestionModel> getQuestionsForVersion(VersionModel version) {
  // Cache is used for active and obsolete versions
  if (settingsManifest.getVersionCacheEnabled() && version.id <= getActiveVersion().id) {
    return questionsByVersionCache.getOrElseUpdate(
        String.valueOf(version.id), version::getQuestions);
  }
  return getQuestionsForVersionWithoutCache(version);
}
```

**Why It Fails**:
1. Cache key is version ID (e.g., "10", "11")
2. When Version 11 is published:
   - Version 10 (ACTIVE) → becomes OBSOLETE
   - Version 11 (DRAFT) → becomes ACTIVE
3. **Cache for Version 10 is NEVER cleared**
4. Condition `version.id <= getActiveVersion().id` remains true for Version 10 (10 ≤ 11)
5. Stale cached data for Version 10 persists

**Where Cache SHOULD Be Cleared But Isn't**:
```java
// Lines 137-263: publishNewSynchronizedVersion()
private Optional<PreviewPublishedVersion> publishNewSynchronizedVersion(PublishMode publishMode) {
  // ... version transitions happen here ...
  draft.setLifecycleStage(LifecycleStage.ACTIVE);   // Version 11 → ACTIVE
  active.setLifecycleStage(LifecycleStage.OBSOLETE); // Version 10 → OBSOLETE
  draft.save();
  active.save();
  transaction.commit();
  // ⚠️ NO CACHE INVALIDATION!
  // Missing: questionsByVersionCache.remove(String.valueOf(active.id));
  // Missing: programsByVersionCache.remove(String.valueOf(active.id));
}
```

## Error Flow

```
1. Admin downloads application JSON
   ↓
2. JsonExporterService.export()
   ↓
3. Tries to load question definitions for program
   ↓
4. ReadOnlyVersionedQuestionServiceImpl.getQuestionDefinition(questionId)
   ↓
5. Question not in cached version's question map
   ↓
6. Returns new NullQuestionDefinition(questionId)  [Line 71]
   ↓
7. QuestionJsonPresenter.create() receives NULL_QUESTION type
   ↓
8. Throws: "Unrecognized questionType NULL_QUESTION" [Line 181]
```

## Key Files Involved

1. **VersionRepository.java** (lines 574-581, 639-646, 137-263)
   - Cache population
   - Version publishing (WHERE FIX SHOULD GO)

2. **ReadOnlyVersionedQuestionServiceImpl.java** (lines 26-31, 65-72)
   - Builds question map from cached version
   - Creates NULL_QUESTION when not found

3. **ProgramRepository.java** (lines 189-224)
   - Detects and logs null questions
   - Won't set program in cache if null questions found

4. **QuestionJsonPresenter.java** (line ~181)
   - Throws exception for NULL_QUESTION type

## Why It Matches Your Symptoms

✅ **"Every once in a while"** - Depends on timing of cache state vs. publish events

✅ **"Bigger deployments with multiple admins"** - Multiple servers/tabs = more cache inconsistency

✅ **"Sticky until restart"** - Cache persists in memory until restart

✅ **"No problem with database"** - Database is always correct; cache is stale

✅ **"Works if you poison cache in debugger"** - Confirms it's a cache issue

## Documents Created

1. **REPRODUCE_NULL_QUESTION_BUG.md** - Step-by-step reproduction guide
2. **NULL_QUESTION_BUG_ANALYSIS.md** - Detailed technical analysis
3. **SUMMARY_NULL_QUESTION_BUG.md** - This summary

## What's NOT Included (As Requested)

❌ No code changes to fix the bug
❌ No pull request
❌ No tests for the fix
❌ No cache invalidation implementation

You now have everything you need to reproduce the bug and understand its mechanics!
