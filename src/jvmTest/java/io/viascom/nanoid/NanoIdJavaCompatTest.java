package io.viascom.nanoid;

import kotlin.random.PlatformRandomKt;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compile-time and runtime guard that the 1.x default-parameter call forms stay statically
 * callable from Java ({@code @JvmStatic}/{@code @JvmOverloads}), and that the documented
 * java.util.Random migration path works.
 */
class NanoIdJavaCompatTest {

    private static final String ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Test
    void defaultCallFormsAreStaticallyCallable() {
        assertEquals(21, NanoId.generate().length());
        assertEquals(10, NanoId.generate(10).length());
        assertEquals(21, NanoId.generate(21, "ABC123").length());
        assertEquals(21, NanoId.generate(21, ALPHABET, 1.6).length());
        assertEquals(10, NanoId.generateOptimized(10, ALPHABET, 63, 16).length());
    }

    @Test
    void helpersAreStaticallyCallable() {
        assertEquals(63, NanoId.calculateMask(ALPHABET));
        assertEquals(34, NanoId.calculateStep(21, ALPHABET, 1.6));
        assertEquals(22, NanoId.calculateStep(21, ALPHABET));
        assertEquals(1.02, NanoId.calculateAdditionalBytesFactor(ALPHABET), 0.0);
    }

    @Test
    void javaUtilRandomMigrationPathWorks() {
        String id = NanoId.generate(21, ALPHABET, 1.6, PlatformRandomKt.asKotlinRandom(new SecureRandom()));
        assertEquals(21, id.length());
    }
}
