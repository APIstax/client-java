package io.apistax.client;

import java.util.List;
import java.util.Objects;

public class APIstaxException extends RuntimeException {

    private List<String> messages;

    public APIstaxException(Throwable cause) {
        super(cause);
    }

    public APIstaxException(List<String> messages, Throwable cause) {
        super(String.join(",", Objects.requireNonNullElseGet(messages, List::of)), cause);
        this.messages = messages;
    }

    public APIstaxException(List<String> messages) {
        this(messages, null);
    }

    public List<String> getMessages() {
        return messages;
    }
}
