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

package io.viascom.nanoid

import io.viascom.nanoid.NanoId.calculateMask
import io.viascom.nanoid.NanoId.calculateStep
import io.viascom.nanoid.NanoId.generate
import io.viascom.nanoid.NanoId.generateOptimized
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import java.security.SecureRandom
import java.util.*
import kotlin.test.Test

internal class NanoIdTest {

    companion object {
        private const val DEFAULT_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    @Test
    fun testGenerate_Defaults() {
        val id = generate()
        assertThat(id).hasSize(21)
    }

    @Test
    fun testGenerate_CustomSize() {
        var id = generate(10)
        assertThat(id).hasSize(10)

        id = generate(50)
        assertThat(id).hasSize(50)
    }

    @Test
    fun testGenerate_MinimumSize() {
        val id = generate(1)
        assertThat(id).hasSize(1)
    }

    @Test
    fun testGenerate_MaximumAlphabet() {
        val alphabet = (0..254).map { it.toChar() }.joinToString("")
        val id = generate(10, alphabet)
        assertThat(id).matches("^[${Regex.escape(alphabet)}]+$")
    }

    @Test
    fun testGenerate_UsesOnlyAlphabetChars() {
        val id = generate(100, "abc123")
        assertThat(id).matches("^[abc123]+$")
    }

    @Test
    fun testGenerate_ThrowsOnInvalidSize() {
        assertThrows(IllegalArgumentException::class.java) { generate(0) }
        assertThrows(IllegalArgumentException::class.java) { generate(-5) }
    }

    @Test
    fun testGenerate_ThrowsOnInvalidAlphabet() {
        assertThrows(IllegalArgumentException::class.java) { generate(10, "") }
        assertThrows(IllegalArgumentException::class.java) { generate(10, "a".repeat(256)) } // Too long
    }

    @Test
    fun testGenerate_WithCustomRandom() {
        val random = Random(1234)
        val id1 = generate(10, DEFAULT_ALPHABET, 1.6, random)
        val id2 = generate(10, DEFAULT_ALPHABET, 1.6, random)

        assertThat(id1).isNotEqualTo(id2) // Custom random should still generate different IDs
    }

    @Test
    fun testCalculateMask_SingleCharAlphabet() {
        assertThat(calculateMask("a")).isEqualTo(0) // 2^0 - 1 = 0
    }

    @Test
    fun testCalculateMask_PowerOfTwoAlphabet() {
        assertThat(calculateMask("ab")).isEqualTo(1) // 2^1 - 1 = 1
        assertThat(calculateMask("abcd")).isEqualTo(3) // 2^2 - 1 = 3
        assertThat(calculateMask("abcdefgh")).isEqualTo(7) // 2^3 - 1 = 7
        assertThat(calculateMask("abcdefghijklmno")).isEqualTo(15) // 2^4 - 1 = 15
    }

    @Test
    fun testCalculateStep() {
        val mask = calculateMask("abcdef")
        assertThat(calculateStep(10, "abcdef", 1.6, mask)).isGreaterThan(0)
    }

    @Test
    fun testGenerateOptimized_ReturnsCorrectSize() {
        val mask = calculateMask(DEFAULT_ALPHABET)
        val step = calculateStep(21, DEFAULT_ALPHABET, 1.6, mask)
        val id = generateOptimized(21, DEFAULT_ALPHABET, mask, step, SecureRandom())

        assertThat(id).hasSize(21)
    }

    @Test
    fun testGenerateOptimized_UsesOnlyAlphabetChars() {
        val mask = calculateMask("xyz")
        val step = calculateStep(15, "xyz", 1.6, mask)
        val id = generateOptimized(15, "xyz", mask, step, Random())

        assertThat(id).matches("^[xyz]+$")
    }

    @Test
    fun testGenerateOptimized_LargeSize() {
        val mask = calculateMask(DEFAULT_ALPHABET)
        val step = calculateStep(1000, DEFAULT_ALPHABET, 1.6, mask)
        val id = generateOptimized(1000, DEFAULT_ALPHABET, mask, step, SecureRandom())

        assertThat(id).hasSize(1000)
    }

    @Test
    fun testGenerateOptimized_ThrowsOnZeroSize() {
        val mask = calculateMask(DEFAULT_ALPHABET)
        val step = calculateStep(0, DEFAULT_ALPHABET, 1.6, mask)
        assertThrows(IllegalArgumentException::class.java) { generateOptimized(0, DEFAULT_ALPHABET, mask, step, SecureRandom()) }
    }
}
