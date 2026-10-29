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
    fun generateWithTheDefaultAlphabetProducesUrlSafeIds() {
        val urlSafe = Regex("^[A-Za-z0-9_-]{21}$")
        repeat(100) {
            val id = NanoId.generate()
            assertTrue(urlSafe.matches(id), "not URL-safe: $id")
        }
    }

    @Test
    fun generateSupportsNonAsciiAlphabets() {
        val alphabet = "абвгд"
        repeat(100) {
            val id = NanoId.generate(size = 10, alphabet = alphabet)
            assertEquals(10, id.length)
            assertTrue(id.all { it in alphabet }, "unexpected character in $id")
        }
    }

    @Test
    fun generateSupportsEmojiAlphabets() {
        val alphabet = "😀😁🎉"
        val symbols = symbolsIn(NanoId.generate(size = 300, alphabet = alphabet, random = Random(5)))
        assertEquals(300, symbols.size)
        assertEquals(symbolsIn(alphabet).toSet(), symbols.toSet())
    }

    @Test
    fun generateSupportsAlphabetsMixingBmpAndEmojiSymbols() {
        val symbols = symbolsIn(NanoId.generate(size = 200, alphabet = "ab😀😁", random = Random(6)))
        assertEquals(200, symbols.size)
        assertEquals(setOf("a", "b", "😀", "😁"), symbols.toSet())
    }

    @Test
    fun generateCountsAnEmojiAsOneSymbolTowardsThe256Limit() {
        val bmpSymbols = (0 until 255).map { Char(0x100 + it) }.joinToString("")
        assertEquals(50, symbolsIn(NanoId.generate(size = 50, alphabet = bmpSymbols + "😀")).size)
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = bmpSymbols + "😀😁") }
    }

    @Test
    fun generateRejectsAnAlphabetWithAnUnpairedSurrogate() {
        for (alphabet in listOf("ab\uD83D", "\uDE00ab", "a\uDE00\uD83Db")) {
            val error = assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = alphabet) }
            assertEquals("alphabet must not contain unpaired surrogates.", error.message)
        }
    }

    @Test
    fun generateAcceptsAnAdditionalBytesFactorOfExactlyOne() {
        assertEquals(21, NanoId.generate(additionalBytesFactor = 1.0).length)
        assertEquals(21, NanoId.generate(alphabet = "ABC123", additionalBytesFactor = 1.0).length)
    }

    @Test
    fun generateAcceptsALargeAdditionalBytesFactor() {
        val id = NanoId.generate(alphabet = "ABC123", additionalBytesFactor = 1000.0)
        assertEquals(21, id.length)
        assertTrue(id.all { it in "ABC123" })
    }

    @Test
    fun generateAcceptsTheSmallestValues() {
        assertEquals("x", NanoId.generate(size = 1, alphabet = "x", additionalBytesFactor = 1.0))
    }

    @Test
    fun generateReachesEverySymbolForAlphabetsAroundPowersOfTwo() {
        for (length in intArrayOf(2, 16, 63, 64, 65)) {
            val alphabet = (0 until length).map { Char('!'.code + it) }.joinToString("")
            val seen = NanoId.generate(size = 5_000, alphabet = alphabet, random = Random(length)).toSet()
            assertEquals(alphabet.toSet(), seen, "alphabet length $length")
        }
    }

    @Test
    fun generateWeightsDuplicateSymbolsByHowOftenTheyAppear() {
        val id = NanoId.generate(size = 30_000, alphabet = "aab", random = Random(3))
        val shareOfA = id.count { it == 'a' }.toDouble() / id.length
        assertTrue(shareOfA in 0.64..0.69, "share of 'a' was $shareOfA")
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

    // Seeded, so the 0.05 bound from upstream is checked without flakiness on every target.
    @Test
    fun generateHasAFlatDistributionWithThePowerOfTwoDefaultAlphabet() {
        val random = Random(1)
        assertFlatDistribution(defaultAlphabet, List(50_000) { NanoId.generate(random = random) })
    }

    @Test
    fun generateHasAFlatDistributionWithANonPowerOfTwoAlphabet() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val random = Random(1)
        assertFlatDistribution(alphabet, List(50_000) { NanoId.generate(size = 30, alphabet = alphabet, random = random) })
    }

    @Test
    fun generateOptimizedHasAFlatDistributionWhenTheMaskIsLargerThanNeeded() {
        val random = Random(1)
        assertFlatDistribution(defaultAlphabet, List(50_000) { NanoId.generateOptimized(21, defaultAlphabet, 255, 100, random) })
    }

    // ------------------------------------------------------------------
    // Prefixed IDs (parity with nanoid-postgres and nanoid-mysql-mariadb)
    // ------------------------------------------------------------------

    @Test
    fun generatePrefixedPrependsThePrefixWithoutCountingItTowardsTheSize() {
        val id = NanoId.generatePrefixed("usr_")
        assertEquals(25, id.length)
        assertTrue(id.startsWith("usr_"))
        assertTrue(id.removePrefix("usr_").all { it in defaultAlphabet })
        assertEquals(14, NanoId.generatePrefixed("ord_", size = 10).length)
    }

    @Test
    fun generatePrefixedGeneratesTheSameIdAsGenerateForTheSameSeed() {
        assertEquals("usr_$GOLDEN_DEFAULT", NanoId.generatePrefixed("usr_", random = Random(42)))
        assertEquals("usr_$GOLDEN_ABC123", NanoId.generatePrefixed("usr_", alphabet = "ABC123", random = Random(42)))
        assertEquals(GOLDEN_DEFAULT, NanoId.generatePrefixed("", random = Random(42)))
    }

    @Test
    fun generatePrefixedValidatesLikeGenerate() {
        assertFailsWith<IllegalArgumentException> { NanoId.generatePrefixed("usr_", size = 0) }
        assertFailsWith<IllegalArgumentException> { NanoId.generatePrefixed("usr_", alphabet = "") }
        assertFailsWith<IllegalArgumentException> { NanoId.generatePrefixed("usr_", additionalBytesFactor = 0.9) }
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
        val error = assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = "") }
        assertEquals("alphabet must contain between 1 and 256 symbols.", error.message)
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
        val error = assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = alphabet) }
        assertEquals("alphabet must contain between 1 and 256 symbols.", error.message)
    }

    @Test
    fun generateRejectsANonPositiveSize() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(size = 0) }
        assertFailsWith<IllegalArgumentException> { NanoId.generate(size = -1) }
    }

    @Test
    fun generateRejectsANonFiniteAdditionalBytesFactor() {
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = "abc", additionalBytesFactor = Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { NanoId.generate(alphabet = "abc", additionalBytesFactor = Double.NaN) }
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
    fun helpersRejectAnInvalidAlphabet() {
        val tooLong = "a".repeat(257)
        for (alphabet in listOf("", tooLong)) {
            val message = "alphabet must contain between 1 and 256 symbols."
            assertEquals(message, assertFailsWith<IllegalArgumentException> { NanoId.calculateMask(alphabet) }.message)
            assertEquals(message, assertFailsWith<IllegalArgumentException> { NanoId.calculateStep(21, alphabet) }.message)
            assertEquals(message, assertFailsWith<IllegalArgumentException> { NanoId.calculateStep(21, alphabet, 1.6) }.message)
            assertEquals(message, assertFailsWith<IllegalArgumentException> { NanoId.calculateAdditionalBytesFactor(alphabet) }.message)
        }
    }

    @Test
    fun helpersCountAnEmojiAsOneSymbol() {
        assertEquals(1, NanoId.calculateMask("😀😁"))
        assertEquals(3, NanoId.calculateMask("😀😁🎉"))
        assertEquals(NanoId.calculateStep(21, "abc"), NanoId.calculateStep(21, "😀😁🎉"))
        assertEquals(NanoId.calculateAdditionalBytesFactor("abc"), NanoId.calculateAdditionalBytesFactor("😀😁🎉"))
    }

    @Test
    fun calculateMaskIsTheSmallestPowerOfTwoMinusOneCoveringTheAlphabet() {
        val expected = mapOf(2 to 1, 3 to 3, 4 to 3, 5 to 7, 63 to 63, 64 to 63, 65 to 127, 128 to 127, 129 to 255, 256 to 255)
        for ((length, mask) in expected) {
            assertEquals(mask, NanoId.calculateMask("a".repeat(length)), "alphabet length $length")
        }
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

    @Test
    fun generateOptimizedMatchesGenerateForTheSameSeed() {
        val alphabet = "ABC123"
        val step = NanoId.calculateStep(50, alphabet, 1.6)
        assertEquals(
            NanoId.generate(50, alphabet, 1.6, Random(7)),
            NanoId.generateOptimized(50, alphabet, NanoId.calculateMask(alphabet), step, Random(7)),
        )
    }

    // ------------------------------------------------------------------
    // Large IDs (over the 65536-byte Web Crypto limit per getRandomValues call)
    // ------------------------------------------------------------------

    @Test
    fun generateProducesLargeIdsWithAPowerOfTwoAlphabet() {
        val id = NanoId.generate(size = 70_000)
        assertEquals(70_000, id.length)
        assertTrue(id.all { it in defaultAlphabet })
    }

    @Test
    fun generateProducesLargeIdsWithANonPowerOfTwoAlphabet() {
        val id = NanoId.generate(size = 70_000, alphabet = "ABC123")
        assertEquals(70_000, id.length)
        assertTrue(id.all { it in "ABC123" })
    }

    // ------------------------------------------------------------------
    // Byte mapping with a fixed byte sequence (ported from upstream customRandom tests)
    // ------------------------------------------------------------------

    @Test
    fun generateRejectsMaskedBytesOutsideTheAlphabet() {
        val sequence = intArrayOf(2, 255, 3, 7, 7, 7, 7, 7, 0, 1)
        assertEquals("cdac", NanoId.generate(size = 4, alphabet = "abcde", random = SequenceRandom(*sequence)))
        assertEquals("cdabcdabcdabcdabcc", NanoId.generate(size = 18, alphabet = "abcde", random = SequenceRandom(*sequence)))
    }

    @Test
    fun generateMapsEveryByteForAPowerOfTwoAlphabet() {
        assertEquals("cddd", NanoId.generate(size = 4, alphabet = "abcd", random = SequenceRandom(2, 255, 3, 7)))
    }

    @Test
    fun generateOptimizedRejectsBytesAboveTheAlphabetWhenTheMaskIsLargerThanNeeded() {
        assertEquals("cdcd", NanoId.generateOptimized(4, "abcd", 7, 4, SequenceRandom(2, 255, 3, 7)))
    }

    // ------------------------------------------------------------------
    // generateOptimized validation
    // ------------------------------------------------------------------

    @Test
    fun generateOptimizedRejectsSizeZero() {
        val error = assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(0, "abc", 3, 5) }
        assertEquals("size must be greater than zero.", error.message)
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(0, defaultAlphabet, 63, 16) }
    }

    @Test
    fun generateOptimizedSupportsEmojiAlphabets() {
        val alphabet = "😀😁🎉"
        val symbols = symbolsIn(NanoId.generateOptimized(30, alphabet, NanoId.calculateMask(alphabet), 60))
        assertEquals(30, symbols.size)
        assertTrue(symbols.all { it in symbolsIn(alphabet) })
    }

    @Test
    fun generateOptimizedRejectsANegativeSize() {
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(-1, "abc", 3, 5) }
    }

    @Test
    fun generateOptimizedRejectsAnEmptyAlphabet() {
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "", 0, 5) }
    }

    @Test
    fun generateOptimizedRejectsAnAlphabetLargerThan256Characters() {
        val alphabet = (0 until 257).map { Char(it + 0x100) }.joinToString("")
        val error = assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, alphabet, 255, 5) }
        assertEquals("alphabet must contain between 1 and 256 symbols.", error.message)
    }

    @Test
    fun generateOptimizedRejectsAMaskOutsideTheByteRange() {
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abc", -1, 5) }
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abc", 511, 5) }
    }

    @Test
    fun generateOptimizedRejectsAMaskThatDoesNotCoverTheAlphabet() {
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(21, defaultAlphabet, 1, 32) }
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(21, defaultAlphabet, 15, 32) }
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abc", 1, 5) }
    }

    @Test
    fun generateOptimizedRejectsAMaskThatSkipsAlphabetIndices() {
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abc", 100, 5) }
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abcd", 5, 5) }
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(21, defaultAlphabet, 64, 32) }
    }

    @Test
    fun generateOptimizedAcceptsAnyMaskThatReachesEveryAlphabetIndex() {
        val id = NanoId.generateOptimized(100, "abc", 131, 40)
        assertEquals(100, id.length)
        assertEquals("abc".toSet(), id.toSet())
        assertEquals("xxxxx", NanoId.generateOptimized(5, "x", 0, 5))
    }

    @Test
    fun generateOptimizedAcceptsAMaskLargerThanNeeded() {
        val id = NanoId.generateOptimized(21, defaultAlphabet, 255, 100)
        assertEquals(21, id.length)
        assertTrue(id.all { it in defaultAlphabet })
    }

    @Test
    fun generateOptimizedRejectsANonPositiveStep() {
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abc", 3, 0) }
        assertFailsWith<IllegalArgumentException> { NanoId.generateOptimized(5, "abc", 3, -1) }
    }

    companion object {
        // Pinned outputs for kotlin.random.Random(42). Generated once on the JVM
        // (Task 3, Step 4) and asserted identically on every target, which is the
        // cross-platform determinism proof. The 1.x java.util.Random vectors cannot
        // survive: seeded java randoms are part of the API dropped in 2.0.0.
        private const val GOLDEN_DEFAULT = "oaJT_uZB8ZFBqL5TFLc3A"
        private const val GOLDEN_ABC123 = "C2BAAC12BB1B3AC3CBCAA"
    }

    private class SequenceRandom(vararg values: Int) : Random() {
        private val sequence = values.map { it.toByte() }

        override fun nextBits(bitCount: Int): Int = throw UnsupportedOperationException()

        override fun nextBytes(array: ByteArray): ByteArray {
            for (i in array.indices) array[i] = sequence[i % sequence.size]
            return array
        }
    }

    private fun symbolsIn(text: String): List<String> {
        val symbols = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val isPair = text[i].isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()
            assertTrue(isPair || !text[i].isSurrogate(), "unpaired surrogate at index $i")
            val width = if (isPair) 2 else 1
            symbols.add(text.substring(i, i + width))
            i += width
        }
        return symbols
    }

    private fun assertFlatDistribution(alphabet: String, ids: List<String>) {
        val counts = ids.flatMap { it.asIterable() }.groupingBy { it }.eachCount()
        assertEquals(alphabet.toSet(), counts.keys)
        val totalChars = ids.sumOf { it.length }
        val shares = counts.values.map { it.toDouble() * alphabet.length / totalChars }
        assertTrue(shares.max() - shares.min() <= 0.05, "distribution spread ${shares.max() - shares.min()}")
    }
}
