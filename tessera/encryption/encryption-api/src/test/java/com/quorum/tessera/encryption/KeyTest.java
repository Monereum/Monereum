package com.quorum.tessera.encryption;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class KeyTest {

    private final Class<? extends Key> type;

    private final Method factoryMethod;

    public KeyTest(Class<? extends Key> type) throws Exception {
        this.type = type;
        factoryMethod = type.getDeclaredMethod("from", byte[].class);
    }

    @Test
    public void doTests() throws Exception {

        byte[] data = "SOME_DATA".getBytes();

        String base64Value = Base64.getEncoder().encodeToString(data);

        Key key = (Key) factoryMethod.invoke(null, data);

        assertThat(key).isNotNull();

        assertThat(key.encodeToBase64()).isEqualTo(base64Value);

        assertThat(key.toString()).isEqualTo(type.getSimpleName() + "[" + base64Value + "]");

        assertThat(key.hashCode()).isEqualTo(Arrays.hashCode(data));

        Key secondKey = (Key) factoryMethod.invoke(null, "OTHERDATA".getBytes());

        assertThat(key).isNotEqualTo(secondKey);

        BogusKey bogusKey = new BogusKey(data);
        assertThat(key).isNotEqualTo(bogusKey);
        assertThat(key.encodeToBase64()).isEqualTo(bogusKey.encodeToBase64());

        Key otherKeyWithSameData = (Key) factoryMethod.invoke(null, data);
        assertThat(key).isEqualTo(otherKeyWithSameData);
    }

    @Parameterized.Parameters
    public static List<Class<? extends Key>> cases() {
        return Arrays.asList(MasterKey.class, SharedKey.class, PrivateKey.class, PublicKey.class);
    }

    static class BogusKey extends BaseKey {

        BogusKey(byte[] keyBytes) {
            super(keyBytes);
        }

    }

}
