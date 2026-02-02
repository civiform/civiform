# Reproduction Steps for NULL_QUESTION Cache Bug

## Overview
This document describes how to reproduce the NULL_QUESTION bug that occurs due to stale cache data when multiple CiviForm servers are running and versions are published.

## Root Cause
The bug occurs because:
1. `questionsByVersionCache` and `programsByVersionCache` in `VersionRepository` cache questions/programs by version ID
2. When a new version is published (draft → active, old active → obsolete), **the cache is never invalidated**
3. Different servers can have different cached states for the same version ID
4. When a program references a question that exists in the new active version but not in the cached old version, it creates a `NullQuestionDefinition`, leading to `NULL_QUESTION` errors

## Prerequisites
- Two CiviForm server containers running locally
- Both servers connected to the same database
- Cache enabled in settings (default: enabled)
- Debugger attached to at least one server (optional but helpful)

## Step-by-Step Reproduction

### Phase 1: Setup Initial State (Server A & B)

1. **Start both servers** (Server A and Server B) connected to the same database
2. **On Server A**: Create a program with 2 blocks and a few questions
   - Example: Program "test-program" with questions Q1, Q2, Q3
3. **On Server A**: Publish the version
   - This creates Version 1 (ACTIVE) with the program and questions
4. **On Server A**: Load the program admin page
   - This caches Version 1 questions in Server A's `questionsByVersionCache`
5. **On Server B**: Load the same program admin page  
   - This caches Version 1 questions in Server B's `questionsByVersionCache`
6. **Verify**: Both servers now have Version 1 cached

### Phase 2: Create Version Mismatch (Server A)

7. **On Server A**: Create a new draft of the program
8. **On Server A**: Add a new question Q4 to the program (create new question)
9. **On Server A**: Publish the new version
   - Version 2 becomes ACTIVE
   - Version 1 becomes OBSOLETE
   - **Critical**: Server A's cache for Version 1 is NOT cleared
   - **Critical**: Server B's cache for Version 1 is NOT cleared

### Phase 3: Trigger the Bug (Server B)

10. **On Server B**: Navigate to the program page (or reload)
    - Server B queries DB for "active version" → gets Version 2
    - BUT if Server B loads a program that was cached with references to Version 1 questions...

11. **On Server B**: Try to access program data that requires syncing questions
    - Example: Download JSON export of applications (`AdminApplicationController.downloadAllJson`)
    - Example: View program details that sync question definitions

### Expected Behavior vs Actual Behavior

**Expected**: Server B should load fresh question data from Version 2

**Actual**: Server B may load stale cached question data from Version 1, causing:
- `"Question not found for ID: XXXX"` log messages (line 70 in `ReadOnlyVersionedQuestionServiceImpl`)
- Creation of `NullQuestionDefinition` objects
- `"Unrecognized questionType NULL_QUESTION"` exceptions when the null question is accessed

## Key Code Locations

### Where Cache is Populated
**File**: `server/app/repository/VersionRepository.java`
- **Line 574-581**: `getQuestionsForVersion()` method
- **Line 639-646**: `getProgramsForVersion()` method
- Uses `cache.getOrElseUpdate(String.valueOf(version.id), ...)`

### Where Version is Published (Cache SHOULD be cleared but ISN'T)
**File**: `server/app/repository/VersionRepository.java`
- **Line 137-263**: `publishNewSynchronizedVersion(PublishMode)` method
- **Line 289-366**: `publishNewSynchronizedVersion(String programAdminName)` method
- **No cache invalidation exists in either method**

### Where NULL_QUESTION is Created
**File**: `server/app/services/question/ReadOnlyVersionedQuestionServiceImpl.java`
- **Line 65-72**: `getQuestionDefinition(long id)` method
- Returns `new NullQuestionDefinition(id)` when question not found in cached map

### Where NULL_QUESTION Causes Exceptions
**File**: `server/app/services/export/QuestionJsonPresenter.java` (mentioned in stack trace)
- Line ~181: Throws exception for unrecognized question type `NULL_QUESTION`

## Debugger Breakpoints

Set breakpoints at these locations to observe the bug:

1. **VersionRepository.java:577** - When cache is accessed
   - Check `version.id` and `String.valueOf(version.id)` cache key
   - Check what's returned from cache vs database

2. **ReadOnlyVersionedQuestionServiceImpl.java:28-30** - Constructor
   - Check which version ID is being loaded
   - Check how many questions are in the `questionsById` map

3. **ReadOnlyVersionedQuestionServiceImpl.java:70** - Where NULL question is logged
   - Check the ID that's not found
   - Check the contents of `questionsById` map

4. **VersionRepository.java:244-249** - After version publish commits
   - Check that old version cache is NOT cleared
   - Check `draft.id` (new active) and `active.id` (now obsolete)

## Simplified Reproduction (Single Server with Cache Poisoning)

You can also reproduce this on a single server using a debugger:

1. Set breakpoint at `VersionRepository.java:578` (inside `getQuestionsForVersion`)
2. Create a program with questions, publish it (Version 1 active)
3. Access the program to populate cache for Version 1
4. Create new draft with additional question, publish it (Version 2 active)
5. When code calls `getQuestionsForVersion(version2)`:
   - Use debugger to force it to call `getQuestionsForVersion(version1)` instead
   - Or manually inject old cached data for the new version ID
6. Try to export program data → NULL_QUESTION error

## Verification

You've successfully reproduced the bug when you see:

1. **Log message**: `"Question not found for ID: XXXX"` 
2. **Log message**: `"Program {name} with ID {id} has the following null question ID(s): ..."` (from `ProgramRepository.java:216-223`)
3. **Exception**: `"Unrecognized questionType NULL_QUESTION"`
4. **Stack trace** showing path through:
   - `QuestionJsonPresenter.create()` → RuntimeException
   - `JsonExporterService.export()` 
   - `AdminApplicationController.downloadAllJson()`

## Notes

- The bug is **intermittent** because it depends on cache state across servers
- The bug is **sticky** until server restart because cache persists in memory
- The bug is **resolved by restart** because cache is cleared
- The bug **doesn't affect database** - data is always correct, only cache is wrong
- The bug is more likely with **multiple servers** or **multiple admin tabs/windows**
