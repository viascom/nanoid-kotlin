/*
 * Copyright 2025 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.nanoid;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NanoIdJavaTest {

    @Test
    void testGenerate_Default() {
        String id = NanoId.generate();
        assertEquals(21, id.length()); // Default size
    }

    @Test
    void testGenerate_WithoutOptionalParameters() {
        String id = NanoId.generate(21, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 1.6, new Random());
        assertEquals(21, id.length());
    }

    @Test
    void testGenerate_WithCustomSize() {
        String id = NanoId.generate(50);
        assertEquals(50, id.length());
    }

    @Test
    void testGenerate_UsesOnlyAlphabetChars() {
        String id = NanoId.generate(100, "abc123");
        assertTrue(id.matches("^[abc123]+$"));
    }

    @Test
    void testGenerate_ThrowsOnNullAlphabet() {
        // Kotlin enforces non-null, so Java will throw NullPointerException
        assertThrows(NullPointerException.class, () -> NanoId.generate(10, null));
    }

    @Test
    void testGenerate_ThrowsOnInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.generate(0));
        assertThrows(IllegalArgumentException.class, () -> NanoId.generate(-5));
    }

    @Test
    void testGenerate_ThrowsOnInvalidAlphabet() {
        String longAlphabet = new String(new char[256]).replace("\0", "a");

        assertThrows(IllegalArgumentException.class, () -> NanoId.generate(10, ""));
        assertThrows(IllegalArgumentException.class, () -> NanoId.generate(10, longAlphabet)); // Too long
    }


    @Test
    void testGenerate_WithCustomRandom() {
        Random random = new Random(1234);
        String id1 = NanoId.generate(10, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 1.6, random);
        String id2 = NanoId.generate(10, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 1.6, random);

        assertNotEquals(id1, id2); // Should still generate different values
    }

    @Test
    void testCalculateMask() {
        assertEquals(3, NanoId.calculateMask("abc")); // 2^2 - 1 = 3
        assertEquals(7, NanoId.calculateMask("abcdefgh")); // 2^3 - 1 = 7
        assertEquals(15, NanoId.calculateMask("abcdefghijklmno")); // 2^4 - 1 = 15
    }

    @Test
    void testCalculateStep() {
        int mask = NanoId.calculateMask("abcdef");
        assertTrue(NanoId.calculateStep(10, "abcdef", 1.6, mask) > 0);
    }

    @Test
    void testGenerateOptimized_ReturnsCorrectSize() {
        int mask = NanoId.calculateMask("_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        int step = NanoId.calculateStep(21, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 1.6, mask);

        String id = NanoId.generateOptimized(21, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", mask, step, new SecureRandom());

        assertEquals(21, id.length());
    }

    @Test
    void testGenerateOptimized_UsesOnlyAlphabetChars() {
        int mask = NanoId.calculateMask("xyz");
        int step = NanoId.calculateStep(15, "xyz", 1.6, mask);
        String id = NanoId.generateOptimized(15, "xyz", mask, step, new Random());

        assertTrue(id.matches("^[xyz]+$"));
    }

    @Test
    void testGenerateOptimized_LargeSize() {
        int mask = NanoId.calculateMask("_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        int step = NanoId.calculateStep(1000, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 1.6, mask);
        String id = NanoId.generateOptimized(1000, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", mask, step, new SecureRandom());

        assertEquals(1000, id.length());
    }

    @Test
    void testGenerateOptimized_ThrowsOnZeroSize() {
        int mask = NanoId.calculateMask("_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        int step = NanoId.calculateStep(0, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 1.6, mask);

        assertThrows(IllegalArgumentException.class, () -> NanoId.generateOptimized(0, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", mask, step, new SecureRandom()));
    }

    @Test
    void testJvmStatic_Usage() {
        // Ensures @JvmStatic allows direct Java calls
        String id = NanoId.generate();
        assertEquals(21, id.length());
    }

    @Test
    void testGenerate_LargeSize() {
        String id = NanoId.generate(5000);
        assertEquals(5000, id.length());
    }
}
