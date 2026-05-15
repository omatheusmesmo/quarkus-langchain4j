package io.quarkiverse.langchain4j.runtime.aiservice;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import io.quarkiverse.langchain4j.ChatHistoryRecorder;

class RecordingTokenStream implements TokenStream {

    private static final Logger log = Logger.getLogger(RecordingTokenStream.class);

    private final TokenStream delegate;
    private final ChatHistoryRecorder chatHistoryRecorder;
    private final Object memoryId;
    private final StringBuilder buffer = new StringBuilder();

    private Consumer<String> partialResponseHandler;
    private BiConsumer<PartialResponse, PartialResponseContext> partialResponseWithContextHandler;
    private Consumer<PartialThinking> partialThinkingHandler;
    private BiConsumer<PartialThinking, PartialThinkingContext> partialThinkingWithContextHandler;
    private Consumer<PartialToolCall> partialToolCallHandler;
    private BiConsumer<PartialToolCall, PartialToolCallContext> partialToolCallWithContextHandler;
    private Consumer<List<Content>> contentsHandler;
    private Consumer<ChatResponse> intermediateResponseHandler;
    private Consumer<BeforeToolExecution> beforeToolExecutionHandler;
    private Consumer<ToolExecution> toolExecutionHandler;
    private Consumer<ChatResponse> completeResponseHandler;
    private Consumer<Throwable> errorHandler;

    RecordingTokenStream(TokenStream delegate, ChatHistoryRecorder chatHistoryRecorder, Object memoryId) {
        this.delegate = delegate;
        this.chatHistoryRecorder = chatHistoryRecorder;
        this.memoryId = memoryId;
    }

    @Override
    public TokenStream onPartialResponse(Consumer<String> handler) {
        this.partialResponseHandler = chunk -> {
            buffer.append(chunk);
            handler.accept(chunk);
        };
        return this;
    }

    @Override
    public TokenStream onPartialResponseWithContext(
            BiConsumer<PartialResponse, PartialResponseContext> handler) {
        this.partialResponseWithContextHandler = (partialResponse, context) -> {
            buffer.append(partialResponse.text());
            handler.accept(partialResponse, context);
        };
        return this;
    }

    @Override
    public TokenStream onPartialThinking(Consumer<PartialThinking> handler) {
        this.partialThinkingHandler = handler;
        return this;
    }

    @Override
    public TokenStream onPartialThinkingWithContext(
            BiConsumer<PartialThinking, PartialThinkingContext> handler) {
        this.partialThinkingWithContextHandler = handler;
        return this;
    }

    @Override
    public TokenStream onPartialToolCall(Consumer<PartialToolCall> handler) {
        this.partialToolCallHandler = handler;
        return this;
    }

    @Override
    public TokenStream onPartialToolCallWithContext(
            BiConsumer<PartialToolCall, PartialToolCallContext> handler) {
        this.partialToolCallWithContextHandler = handler;
        return this;
    }

    @Override
    public TokenStream onRetrieved(Consumer<List<Content>> handler) {
        this.contentsHandler = handler;
        return this;
    }

    @Override
    public TokenStream onIntermediateResponse(Consumer<ChatResponse> handler) {
        this.intermediateResponseHandler = handler;
        return this;
    }

    @Override
    public TokenStream beforeToolExecution(Consumer<BeforeToolExecution> handler) {
        this.beforeToolExecutionHandler = handler;
        return this;
    }

    @Override
    public TokenStream onToolExecuted(Consumer<ToolExecution> handler) {
        this.toolExecutionHandler = handler;
        return this;
    }

    @Override
    public TokenStream onCompleteResponse(Consumer<ChatResponse> handler) {
        this.completeResponseHandler = response -> {
            try {
                chatHistoryRecorder.onAgentMessage(memoryId, buffer.toString());
                chatHistoryRecorder.onCompleted(memoryId);
            } catch (RuntimeException e) {
                log.warnf(e,
                        "Failed to record agent message for memoryId=%s, continuing without recording.",
                        memoryId);
            }
            handler.accept(response);
        };
        return this;
    }

    @Override
    public TokenStream onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
        return this;
    }

    @Override
    public TokenStream ignoreErrors() {
        this.errorHandler = null;
        return this;
    }

    @Override
    public void start() {
        if (partialResponseHandler != null) {
            delegate.onPartialResponse(partialResponseHandler);
        }
        if (partialResponseWithContextHandler != null) {
            delegate.onPartialResponseWithContext(partialResponseWithContextHandler);
        }
        if (partialThinkingHandler != null) {
            delegate.onPartialThinking(partialThinkingHandler);
        }
        if (partialThinkingWithContextHandler != null) {
            delegate.onPartialThinkingWithContext(partialThinkingWithContextHandler);
        }
        if (partialToolCallHandler != null) {
            delegate.onPartialToolCall(partialToolCallHandler);
        }
        if (partialToolCallWithContextHandler != null) {
            delegate.onPartialToolCallWithContext(partialToolCallWithContextHandler);
        }
        if (contentsHandler != null) {
            delegate.onRetrieved(contentsHandler);
        }
        if (intermediateResponseHandler != null) {
            delegate.onIntermediateResponse(intermediateResponseHandler);
        }
        if (beforeToolExecutionHandler != null) {
            delegate.beforeToolExecution(beforeToolExecutionHandler);
        }
        if (toolExecutionHandler != null) {
            delegate.onToolExecuted(toolExecutionHandler);
        }
        if (completeResponseHandler != null) {
            delegate.onCompleteResponse(completeResponseHandler);
        }
        if (errorHandler != null) {
            delegate.onError(errorHandler);
        } else {
            delegate.ignoreErrors();
        }
        delegate.start();
    }
}
