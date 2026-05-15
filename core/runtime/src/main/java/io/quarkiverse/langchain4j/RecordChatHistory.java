package io.quarkiverse.langchain4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When placed on a {@link RegisterAiService} class (or one of its methods),
 * the invocation of each such method will be recorded by the
 * {@link ChatHistoryRecorder} CDI bean if one is available.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RecordChatHistory {
}
