# Technical Analysis: NULL_QUESTION Cache Bug

## Executive Summary

The NULL_QUESTION bug is caused by missing cache invalidation in the version publishing mechanism. When a new version is published, the `questionsByVersionCache` and `programsByVersionCache` are not cleared, leading to stale cache entries that can cause `NullQuestionDefinition` objects to be created when programs reference questions that don't exist in the cached version.

## Architecture Overview

### Cache Structure

**NamedCaches in VersionRepository**:
- `questionsByVersionCache` (name: "version-questions")
  - Key: `String` (version ID as string, e.g., "10")
  - Value: `ImmutableList<QuestionModel>`
- `programsByVersionCache` (name: "version-programs")
  - Key: `String` (version ID as string)
  - Value: `ImmutableList<ProgramModel>`

**Cache Location**: `server/app/repository/VersionRepository.java`
- Injected via constructor (lines 98-99)
- Used in `getQuestionsForVersion()` (line 574-581)
- Used in `getProgramsForVersion()` (line 639-646)

### Version Lifecycle

```
DRAFT → publish → ACTIVE → new publish → OBSOLETE
  ↓                 ↓                        ↓
Version 1        Version 2                Version 1
(not cached)     (cached)              (still cached!)
```

**Key insight**: When Version 2 becomes ACTIVE, Version 1 transitions to OBSOLETE, but **Version 1's cache entry remains valid** because:
1. Cache condition: `version.id <= getActiveVersion().id` (line 576, 641)
2. Version 1 ID (10) ≤ Active version ID (11) → TRUE
3. Cache entry for "10" is still returned

## The Bug Flow

### Scenario: Two Servers, Version Publishing

**Initial State**:
- Server A: Running, has no cached data
- Server B: Running, has no cached data
- Database: Version 10 is ACTIVE with Program P1 and Questions Q1, Q2, Q3

**Step 1: Server A caches Version 10**
```java
// Server A calls getQuestionsForVersion(version10)
// Line 577-578 in VersionRepository.java
questionsByVersionCache.getOrElseUpdate("10", version10::getQuestions)
// Cache now contains: "10" → [Q1, Q2, Q3]
```

**Step 2: Server B caches Version 10**
```java
// Server B calls getQuestionsForVersion(version10)
// Cache now contains: "10" → [Q1, Q2, Q3]
```

**Step 3: Admin creates Draft Version 11**
- New question Q4 added to Program P1
- Database now has:
  - Version 10: ACTIVE, questions [Q1, Q2, Q3]
  - Version 11: DRAFT, questions [Q1, Q2, Q3, Q4]

**Step 4: Admin publishes Version 11**
```java
// Line 137-263 in VersionRepository.java: publishNewSynchronizedVersion()
draft.setLifecycleStage(LifecycleStage.ACTIVE);   // Version 11 → ACTIVE
active.setLifecycleStage(LifecycleStage.OBSOLETE); // Version 10 → OBSOLETE
draft.save();
active.save();
transaction.commit();
// ⚠️ NO CACHE INVALIDATION HERE!
```

**Step 5: Server A loads Program P1**
```java
// Server A queries DB for active version → gets Version 11
VersionModel activeVersion = getActiveVersion(); // Returns version 11

// Server A tries to sync program with questions
// ProgramService.syncProgramAssociations() calls:
// ProgramService.syncProgramDefinitionQuestions(programDef, version11)
// This creates ReadOnlyVersionedQuestionServiceImpl(version11, versionRepository)

// Line 28-30 in ReadOnlyVersionedQuestionServiceImpl.java
questionsById = versionRepository.getQuestionDefinitionsForVersion(version11)
    .stream()
    .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));

// This calls getQuestionsForVersion(version11)
// Line 577-578 in VersionRepository.java
questionsByVersionCache.getOrElseUpdate("11", version11::getQuestions)
// Cache now contains: "11" → [Q1, Q2, Q3, Q4] ✓ CORRECT
```

**Step 6: The Bug - Server B with stale cache**

Now here's where it gets tricky. The bug can manifest in several ways:

**Variant A: Server B has stale program definition in cache**

If Server B cached a full `ProgramDefinition` that includes references to the old version:
```java
// Program P1's blocks contain question IDs: [Q1.id, Q2.id, Q3.id]
// But the program was updated to include [Q1.id, Q2.id, Q3.id, Q4.id]

// Server B loads the program from cache
ProgramDefinition cachedProgram = programDefCache.get("programId");

// The program still thinks it's using Version 10's questions
// When it tries to sync, it creates ReadOnlyVersionedQuestionServiceImpl
// with the OLD version object that was in the cache
```

**Variant B: Cross-version reference**

More subtle - when a program definition is built:
```java
// Server B has a program that was built using Version 10 question references
// The program stores question IDs in its blocks
// When syncing, it creates ReadOnlyVersionedQuestionServiceImpl with Version 11
// But some internal code paths still use the cached Version 10 data

// Example: ProgramRepository.setFullProgramDefinitionCache() checks for null questions
// Line 199-224 in ProgramRepository.java
BlockDefinition block = programDefinition.blockDefinitions().get(0);
ProgramQuestionDefinition pqd = block.programQuestionDefinitions().get(0);
QuestionDefinition qd = pqd.getQuestionDefinition();
// If this question came from a cached Version 10 object but should be Q4 from Version 11
// And Q4 doesn't exist in Version 10's cached question list
// Then getQuestionDefinition(Q4.id) returns NullQuestionDefinition
```

## The Actual Error Path

Based on the stack trace, here's the exact path:

1. **Request**: Admin downloads JSON export of applications
   ```
   GET /admin/programs/{id}/applications
   AdminApplicationController.downloadAllJson()
   ```

2. **Export Processing**: 
   ```java
   // Line 73 in JsonExporterService.java
   export() calls exportPage() for each application
   
   // Line 114 in JsonExporterService.java  
   exportPage() calls QuestionJsonPresenter.create()
   ```

3. **Question Presenter Creation**:
   ```java
   // Line 181 in QuestionJsonPresenter.java
   // Tries to create presenter for question
   // Question has type NULL_QUESTION (because it wasn't found in cache)
   // Throws: "Unrecognized questionType NULL_QUESTION"
   ```

4. **Preceding Logs**:
   ```
   "Question not found for ID: 3190" 
   (from ReadOnlyVersionedQuestionServiceImpl line 70)
   
   "Program fresh-bucks with ID 3011 has the following null question ID(s): 3479, 3480"
   (from ProgramRepository line 216-223)
   ```

## Why Database is Fine But Cache is Wrong

**Database State** (always correct):
```sql
-- Version 11 (ACTIVE)
SELECT * FROM versions WHERE lifecycle_stage = 'active';
-- Returns version ID 11

-- Questions in Version 11
SELECT q.* FROM questions q
JOIN versions_questions vq ON q.id = vq.questions_id
WHERE vq.versions_id = 11;
-- Returns Q1, Q2, Q3, Q4

-- Programs in Version 11  
SELECT p.* FROM programs p
JOIN versions_programs vp ON p.id = vp.programs_id
WHERE vp.versions_id = 11;
-- Returns updated Program P1
```

**Cache State** (stale on some servers):
```
Server A:
  questionsByVersionCache: { "11" → [Q1, Q2, Q3, Q4] } ✓ Fresh
  
Server B:
  questionsByVersionCache: { "10" → [Q1, Q2, Q3] }   ✗ Stale
  programsByVersionCache: { "10" → [P1_old_version] } ✗ Stale
```

When Server B's code path uses the old cached program or question list, it can't find new questions, resulting in NULL_QUESTION errors.

## Race Conditions

### Multi-Server Race
```
Time  Server A                           Server B
----  --------------------------------   --------------------------------
T0    Load program, cache Version 10    Load program, cache Version 10
T1    Admin publishes Version 11        
T2    Cache has Version 11              Cache still has Version 10
T3    Works fine with Version 11        ⚠️ ERROR when accessing Version 10
```

### Multi-Tab Admin Race
```
Time  Tab 1 (Admin A)                   Tab 2 (Admin B)
----  --------------------------------   --------------------------------
T0    Open program editor               
T1    Add new question Q4               
T2    Publish Version 11                
T3                                       Open program in other tab
T4                                       Tab still has old page state
T5                                       Submit changes
T6                                       ⚠️ ERROR: references old version
```

## Why It's Intermittent

The bug is intermittent because it requires specific conditions:
1. **Cache must be enabled** (it is by default)
2. **Timing**: Request must happen after publish but before cache expires (cache doesn't expire)
3. **Multiple servers**: More likely with multiple servers in different states
4. **Code path**: Only certain code paths trigger the error (e.g., JSON export, certain admin operations)

## Why Restart Fixes It

Restarting the server:
1. Clears all in-memory caches (Play Framework cache is in-memory by default)
2. Forces fresh DB queries for all subsequent requests
3. All servers reload current active version from database

## Evidence from Problem Statement

The reported error shows:
```
"Question not found for ID: 3190"
"Program fresh-bucks with ID 3011 has the following null question ID(s): 3479, 3480"
"Unrecognized questionType NULL_QUESTION"
```

This matches the flow:
1. Question 3190 wasn't found in the cached version's question list
2. Program 3011 has references to questions 3479, 3480 that don't exist in the cached version
3. NullQuestionDefinition was created with type NULL_QUESTION
4. When JSON export tried to process it, it threw exception

## Solution (Not Implemented in This PR)

The fix would be to invalidate caches when publishing:

```java
// In publishNewSynchronizedVersion() after line 249
questionsByVersionCache.remove(String.valueOf(active.id));
programsByVersionCache.remove(String.valueOf(active.id));

// Also invalidate program definition cache
programDefCache.removeAll(); // Or selectively remove affected programs
```

However, this PR only provides reproduction steps, not the fix.
