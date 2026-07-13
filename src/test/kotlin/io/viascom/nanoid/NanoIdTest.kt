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

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NanoIdTest {

    private val defaultAlphabet = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    // ---------------------------------------------------------------------
    // Length
    // ---------------------------------------------------------------------

    @Test
    fun `generate returns default length of 21`() {
        assertEquals(21, NanoId.generate().length)
    }

    @Test
    fun `generate returns the requested length`() {
        for (size in intArrayOf(1, 5, 10, 21, 40, 255)) {
            assertEquals(size, NanoId.generate(size = size).length, "size $size")
        }
    }

    // ---------------------------------------------------------------------
    // Alphabet
    // ---------------------------------------------------------------------

    @Test
    fun `generate only uses characters from the given alphabet`() {
        val alphabet = "ABC123"
        val id = NanoId.generate(size = 200, alphabet = alphabet)
        assertTrue(id.all { it in alphabet }, "unexpected character in $id")
    }

    @Test
    fun `generate eventually uses every character of the alphabet`() {
        val alphabet = "ABC123"
        val seen = HashSet<Char>()
        repeat(100) { seen.addAll(NanoId.generate(size = 100, alphabet = alphabet).toSet()) }
        assertEquals(alphabet.toSet(), seen)
    }

    // ---------------------------------------------------------------------
    // Uniqueness / distribution
    // ---------------------------------------------------------------------

    @Test
    fun `generate produces distinct ids`() {
        val ids = HashSet<String>()
        repeat(10_000) { ids.add(NanoId.generate()) }
        assertEquals(10_000, ids.size)
    }

    // ---------------------------------------------------------------------
    // Determinism (guards the SecureRandom-reuse and power-of-two refactors)
    // ---------------------------------------------------------------------

    @Test
    fun `generate with the same seeded random is deterministic`() {
        val a = NanoId.generate(random = Random(42))
        val b = NanoId.generate(random = Random(42))
        assertEquals(a, b)
    }

    @Test
    fun `generate with a seeded random and the default alphabet is stable`() {
        assertEquals(GOLDEN_DEFAULT, NanoId.generate(random = Random(42)))
    }

    @Test
    fun `generate with a seeded random and a non-power-of-two alphabet is stable`() {
        assertEquals(GOLDEN_ABC123, NanoId.generate(alphabet = "ABC123", random = Random(42)))
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    @Test
    fun `generate rejects an empty alphabet`() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = "") }
    }

    @Test
    fun `generate accepts a 256-character alphabet`() {
        val alphabet = (0 until 256).joinToString("") { it.toChar().toString() }
        val id = NanoId.generate(size = 500, alphabet = alphabet)
        assertEquals(500, id.length)
        assertTrue(id.all { it in alphabet }, "unexpected character in generated id")
    }

    @Test
    fun `generate rejects an alphabet larger than 256 characters`() {
        val alphabet = (0..256).joinToString("") { it.toChar().toString() }
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = alphabet) }
    }

    @Test
    fun `generate rejects a non-positive size`() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(size = 0) }
        assertFailsWith<IllegalArgumentException> { NanoId.generate(size = -1) }
    }

    @Test
    fun `generate rejects an additionalBytesFactor below one`() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(additionalBytesFactor = 0.9) }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    @Test
    fun `calculateMask for the default alphabet is 63`() {
        assertEquals(63, NanoId.calculateMask(defaultAlphabet))
    }

    @Test
    fun `calculateMask for a single-character alphabet is 1`() {
        assertEquals(1, NanoId.calculateMask("x"))
    }

    // ---------------------------------------------------------------------
    // Single-character alphabet (regression: mask overflow crash)
    // ---------------------------------------------------------------------

    @Test
    fun `generate with a single-character alphabet returns the repeated character`() {
        assertEquals("xxxxxxxxxx", NanoId.generate(size = 10, alphabet = "x"))
    }

    companion object {
        // Pinned outputs captured from the pre-refactor implementation. They must stay
        // identical through the shared-SecureRandom and power-of-two fast-path changes.
        private const val GOLDEN_DEFAULT = "Pr-UR8YbvVBIC_3ayW6d9"
        private const val GOLDEN_ABC123 = "33BCC3B1AA3222A111B13"
    }
}
