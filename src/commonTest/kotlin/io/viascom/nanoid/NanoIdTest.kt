/*
 * Copyright 2026 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.nanoid

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NanoIdTest {

    private val defaultAlphabet = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val base58Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    // ------------------------------------------------------------------
    // Length
    // ------------------------------------------------------------------

    @Test
    fun generateReturnsDefaultLengthOf21() {
        assertEquals(21, NanoId.generate().length)
    }

    @Test
    fun generateReturnsTheRequestedLength() {
        for (size in intArrayOf(1, 5, 10, 21, 40, 255)) {
            assertEquals(size, NanoId.generate(size = size).length, "size $size")
        }
    }

    // ------------------------------------------------------------------
    // Alphabet
    // ------------------------------------------------------------------

    @Test
    fun generateOnlyUsesCharactersFromTheGivenAlphabet() {
        val alphabet = "ABC123"
        val id = NanoId.generate(size = 200, alphabet = alphabet)
        assertTrue(id.all { it in alphabet }, "unexpected character in $id")
    }

    @Test
    fun generateEventuallyUsesEveryCharacterOfTheAlphabet() {
        val alphabet = "ABC123"
        val seen = HashSet<Char>()
        repeat(100) { seen.addAll(NanoId.generate(size = 100, alphabet = alphabet).toSet()) }
        assertEquals(alphabet.toSet(), seen)
    }

    // ------------------------------------------------------------------
    // Uniqueness / distribution
    // ------------------------------------------------------------------

    @Test
    fun generateProducesDistinctIds() {
        val ids = HashSet<String>()
        repeat(10_000) { ids.add(NanoId.generate()) }
        assertEquals(10_000, ids.size)
    }

    // ------------------------------------------------------------------
    // Determinism (kotlin.random.Random is XorWow, spec-stable everywhere)
    // ------------------------------------------------------------------

    @Test
    fun generateWithTheSameSeededRandomIsDeterministic() {
        val a = NanoId.generate(random = Random(42))
        val b = NanoId.generate(random = Random(42))
        assertEquals(a, b)
    }

    @Test
    fun generateWithASeededRandomAndTheDefaultAlphabetIsStable() {
        assertEquals(GOLDEN_DEFAULT, NanoId.generate(random = Random(42)))
    }

    @Test
    fun generateWithASeededRandomAndANonPowerOfTwoAlphabetIsStable() {
        assertEquals(GOLDEN_ABC123, NanoId.generate(alphabet = "ABC123", random = Random(42)))
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    @Test
    fun generateRejectsAnEmptyAlphabet() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = "") }
    }

    @Test
    fun generateAcceptsA256CharacterAlphabet() {
        val alphabet = (0 until 256).map { it.toChar() }.joinToString("")
        val id = NanoId.generate(size = 500, alphabet = alphabet)
        assertEquals(500, id.length)
        assertTrue(id.all { it in alphabet }, "unexpected character in generated id")
    }

    @Test
    fun generateRejectsAnAlphabetLargerThan256Characters() {
        val alphabet = (0..256).map { it.toChar() }.joinToString("")
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = alphabet) }
    }

    @Test
    fun generateRejectsANonPositiveSize() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(size = 0) }
        assertFailsWith<IllegalArgumentException> { NanoId.generate(size = -1) }
    }

    @Test
    fun generateRejectsAnAdditionalBytesFactorBelowOne() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(additionalBytesFactor = 0.9) }
    }

    // ------------------------------------------------------------------
    // Single-character alphabet (regression: mask overflow crash)
    // ------------------------------------------------------------------

    @Test
    fun generateWithASingleCharacterAlphabetReturnsTheRepeatedCharacter() {
        assertEquals("xxxxxxxxxx", NanoId.generate(size = 10, alphabet = "x"))
    }

    // ------------------------------------------------------------------
    // Helper exactness (spec section 6 values)
    // ------------------------------------------------------------------

    @Test
    fun calculateMaskForTheDefaultAlphabetIs63() {
        assertEquals(63, NanoId.calculateMask(defaultAlphabet))
    }

    @Test
    fun calculateMaskForTheBase58AlphabetIs63() {
        assertEquals(63, NanoId.calculateMask(base58Alphabet))
    }

    @Test
    fun calculateMaskForASingleCharacterAlphabetIs1() {
        assertEquals(1, NanoId.calculateMask("x"))
    }

    @Test
    fun calculateAdditionalBytesFactorForTheDefaultAlphabet() {
        assertEquals(1.02, NanoId.calculateAdditionalBytesFactor(defaultAlphabet))
    }

    @Test
    fun calculateAdditionalBytesFactorForTheBase58Alphabet() {
        assertEquals(1.09, NanoId.calculateAdditionalBytesFactor(base58Alphabet))
    }

    @Test
    fun calculateStepForTheDefaultAlphabet() {
        assertEquals(34, NanoId.calculateStep(21, defaultAlphabet, 1.6))
        assertEquals(22, NanoId.calculateStep(21, defaultAlphabet))
    }

    @Test
    fun calculateStepForTheBase58Alphabet() {
        assertEquals(37, NanoId.calculateStep(21, base58Alphabet, 1.6))
        assertEquals(25, NanoId.calculateStep(21, base58Alphabet))
    }

    // ------------------------------------------------------------------
    // CSPRNG smoke (runs on every target, exercising each platform CSPRNG)
    // ------------------------------------------------------------------

    @Test
    fun generateOptimizedWithTheDefaultSecureRandomProducesAWellFormedId() {
        val id = NanoId.generateOptimized(10, defaultAlphabet, 63, 16)
        assertEquals(10, id.length)
        assertTrue(id.all { it in defaultAlphabet })
    }

    companion object {
        // Pinned outputs for kotlin.random.Random(42). Generated once on the JVM
        // (Task 3, Step 4) and asserted identically on every target, which is the
        // cross-platform determinism proof. The 1.x java.util.Random vectors cannot
        // survive: seeded java randoms are part of the API dropped in 2.0.0.
        private const val GOLDEN_DEFAULT = "oaJT_uZB8ZFBqL5TFLc3A"
        private const val GOLDEN_ABC123 = "C2BAAC12BB1B3AC3CBCAA"
    }
}
