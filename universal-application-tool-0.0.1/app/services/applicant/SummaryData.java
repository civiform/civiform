package services.applicant;

public class SummaryData {
    /** The question text */
    public String questionText;

    /** The applicant's response to the question. */
    public String answerText;

    /** The block id where this question resides. */
    public String blockId; 

    /** The timestamp of when the answer was saved. */
    public Long timestamp;

    /** Whether the question was answered for another program. */
    public boolean isPreviousResponse;
    
    public SummaryData(String questionText, String answerText, String blockId, Long timestamp, boolean isPreviousResponse) {
        this.questionText = questionText;
        this.answerText = answerText;
        this.blockId = blockId;
        this.timestamp = timestamp;
        this.isPreviousResponse = isPreviousResponse;
    }
}