import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal question lifecycle', () => {
  it('create, update, publish, create a new version, and update all questions', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    const questions = await adminQuestions.addAllNonSingleBlockQuestionTypes('qlc-');
    const singleBlockQuestions = await adminQuestions.addAllSingleBlockQuestionTypes('qlc-');
    const repeatedQuestion = 'qlc-repeated-number';
    await adminQuestions.addNumberQuestion(
      repeatedQuestion, 'description', '$this\'s favorite number', '', 'qlc-enumerator');
    const allQuestions = questions.concat(singleBlockQuestions);
    allQuestions.push(repeatedQuestion);

    await adminQuestions.updateAllQuestions(allQuestions);

    const programName = 'program for question lifecycle';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'qlc program description', questions);
    for (const singleBlockQuestion of singleBlockQuestions) {
      const blockName = await adminPrograms.addProgramBlock(programName, 'single-block question', [singleBlockQuestion]);
      if (singleBlockQuestion == 'qlc-enumerator') {
        await adminPrograms.addProgramRepeatedBlock(programName, blockName, 'repeated block desc', [repeatedQuestion]);
      }
    }
    await adminPrograms.publishProgram(programName);

    await adminQuestions.expectActiveQuestions(allQuestions);

    await adminQuestions.createNewVersionForQuestions(allQuestions)

    await adminQuestions.updateAllQuestions(allQuestions);

    await adminPrograms.publishProgram(programName);

    await adminPrograms.createNewVersion(programName);

    await adminPrograms.publishProgram(programName);

    await adminQuestions.expectActiveQuestions(allQuestions);

    await endSession(browser);
  })
})
