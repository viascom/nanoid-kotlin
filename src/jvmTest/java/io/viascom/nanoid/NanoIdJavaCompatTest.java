/*
 * Copyright 2026 Viascom Ltd liab. Co
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        assertEquals(25, NanoId.generatePrefixed("usr_").length());
        assertEquals(14, NanoId.generatePrefixed("ord_", 10).length());
        assertEquals(14, NanoId.generatePrefixed("ord_", 10, "ABC123").length());
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
