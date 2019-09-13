package com.quorum.tessera.reflect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectCallbackTest {

    @Test(expected = ReflectException.class)
    public void executeThrowsClassNotFoundException() {
        final ReflectCallback<String> callback = () -> {
            throw new ClassNotFoundException();
        };

        ReflectCallback.execute(callback);
    }

    @Test
    public void execute() {
        final ReflectCallback<String> callback = () -> "Expected value";

        final String result = ReflectCallback.execute(callback);

        assertThat(result).isEqualTo("Expected value");
    }

}
