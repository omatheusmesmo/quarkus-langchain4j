package io.quarkiverse.langchain4j;

/**
 * SPI that users implement to record the conversation history
 * of AI service methods annotated with {@link RecordChatHistory}.
 *
 * The recorder is called with the resolved memory id
 * (the value of the {@code @MemoryId} parameter, or the default)
 * and the user/agent message content.
 *
 * For streaming responses the implementation does <em>not</em>
 * receive individual tokens; the full agent message is delivered
 * via {@link #onAgentMessage(Object, String)} only after the
 * stream completes successfully.
 */
public interface ChatHistoryRecorder {

    /**
     * Called before the AI service method is invoked, with the
     * user message that will be sent to the LLM.
     *
     * @param memoryId the conversation / memory identifier
     * @param userMessage the text of the user message
     */
    void onUserMessage(Object memoryId, String userMessage);

    /**
     * Called after the AI service method returns, with the
     * full agent response text.
     *
     * @param memoryId the conversation / memory identifier
     * @param agentMessage the text of the agent response
     */
    void onAgentMessage(Object memoryId, String agentMessage);

    /**
     * Called after the agent response has been recorded,
     * signalling that the conversation turn is complete.
     *
     * @param memoryId the conversation / memory identifier
     */
    void onCompleted(Object memoryId);
}
