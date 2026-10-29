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

import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round
import kotlin.random.Random

/**
 * NanoId is a utility object providing functions for generating secure, URL-friendly, unique identifiers.
 *
 * The object offers methods for generating random strings with adjustable parameters like size, alphabet,
 * overhead factor, and a custom random number generator.
 *
 * Example usage:
 * ```
 * val id = NanoId.generate()
 * ```
 */
object NanoId {

    private const val DEFAULT_SIZE = 21
    private const val DEFAULT_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DEFAULT_ADDITIONAL_BYTES_FACTOR = 1.6

    /**
     * Generates a random string based on specified or default parameters.
     *
     * @param size The desired length of the generated string. Default is 21.
     * @param alphabet The set of characters to choose from for generating the string. Default includes alphanumeric characters along with "_" and "-".
     * @param additionalBytesFactor The additional bytes factor used for calculating the step size. Default is 1.6.
     * @param random The random number generator to use. Default is the platform's secure random via [CryptographyRandom].
     * @return The generated random string.
     * @throws IllegalArgumentException if the alphabet is empty, larger than 256 symbols or contains an unpaired surrogate, if the size
     * is not greater than zero, or if `additionalBytesFactor` is below 1 or not finite.
     */
    @JvmStatic
    @JvmOverloads
    fun generate(
        size: Int = DEFAULT_SIZE,
        alphabet: String = DEFAULT_ALPHABET,
        additionalBytesFactor: Double = DEFAULT_ADDITIONAL_BYTES_FACTOR,
        random: Random = CryptographyRandom.Default,
    ): String {
        require(size > 0) { "size must be greater than zero." }
        require(additionalBytesFactor >= 1 && additionalBytesFactor.isFinite()) { "additionalBytesFactor must be a finite number greater or equal 1." }

        val symbols = symbolsOf(alphabet)
        val symbolCount = symbols?.size ?: alphabet.length
        val mask = maskFor(symbolCount)
        return buildId(size, alphabet, symbols, symbolCount, mask, stepFor(size, symbolCount, mask, additionalBytesFactor), random)
    }

    /**
     * Generates a random string like [generate] and prepends a fixed prefix, for typed IDs such as `usr_W9SPTSD4dQk2N5S39mLnT`.
     *
     * @param prefix The text prepended to the generated string. It does not count towards `size`.
     * @param size The desired length of the generated string, without the prefix. Default is 21.
     * @param alphabet The set of characters to choose from for generating the string. Default includes alphanumeric characters along with "_" and "-".
     * @param additionalBytesFactor The additional bytes factor used for calculating the step size. Default is 1.6.
     * @param random The random number generator to use. Default is the platform's secure random via [CryptographyRandom].
     * @return The prefix followed by the generated random string.
     * @throws IllegalArgumentException under the same conditions as [generate].
     */
    @JvmStatic
    @JvmOverloads
    fun generatePrefixed(
        prefix: String,
        size: Int = DEFAULT_SIZE,
        alphabet: String = DEFAULT_ALPHABET,
        additionalBytesFactor: Double = DEFAULT_ADDITIONAL_BYTES_FACTOR,
        random: Random = CryptographyRandom.Default,
    ): String = prefix + generate(size, alphabet, additionalBytesFactor, random)

    /**
     * Generates an optimized random string of a specified size using the given alphabet, mask, and step.
     * Optionally, you can specify a custom random number generator. This optimized version is designed for
     * higher performance and lower memory overhead.
     *
     * @param size The desired length of the generated string.
     * @param alphabet The set of characters to choose from for generating the string.
     * @param mask The mask used for mapping random bytes to alphabet indices. Must be within 0..255 and contain every bit up to the
     * highest alphabet index, so every symbol can be produced. [calculateMask] returns the smallest such mask.
     * @param step The number of random bytes to generate in each iteration. A larger value may speed up the function but increase memory usage.
     * @param random The random number generator. Default is the platform's secure random via [CryptographyRandom].
     * @return The generated optimized string.
     * @throws IllegalArgumentException if `size` is not greater than zero, the alphabet is empty, larger than 256 symbols or contains an
     * unpaired surrogate, `mask` does not meet the rule above, or `step` is not greater than zero.
     */
    @JvmStatic
    @JvmOverloads
    fun generateOptimized(size: Int, alphabet: String, mask: Int, step: Int, random: Random = CryptographyRandom.Default): String {
        require(size > 0) { "size must be greater than zero." }
        val symbols = symbolsOf(alphabet)
        val symbolCount = symbols?.size ?: alphabet.length
        val requiredMaskBits = if (symbolCount == 1) 0 else maskFor(symbolCount)
        require(mask in 0..255 && (mask and requiredMaskBits) == requiredMaskBits) {
            "mask must be within 0..255 and contain the bits of $requiredMaskBits, or some alphabet symbols can never be produced."
        }
        require(step > 0) { "step must be greater than zero." }
        return buildId(size, alphabet, symbols, symbolCount, mask, step, random)
    }

    /**
     * Calculates the optimal additional bytes factor needed for the generation of the step size, which is used to generate random bytes in each iteration.
     *
     * @param alphabet The set of characters to use for generating the string.
     * @return The additional bytes factor, rounded to two decimal places.
     * @throws IllegalArgumentException if the alphabet is empty or larger than 256 characters.
     */
    @JvmStatic
    fun calculateAdditionalBytesFactor(alphabet: String): Double {
        val symbolCount = symbolCount(alphabet)
        val mask = maskFor(symbolCount)
        return round((1 + abs((mask - symbolCount.toDouble()) / symbolCount)) * 100) / 100
    }

    /**
     * Calculates the mask used to map random bytes to indices in the alphabet.
     *
     * @param alphabet The set of characters to use for generating the string.
     * @return The calculated mask value.
     * @throws IllegalArgumentException if the alphabet is empty or larger than 256 characters.
     */
    @JvmStatic
    fun calculateMask(alphabet: String): Int = maskFor(symbolCount(alphabet))

    /**
     * Calculates the number of random bytes to generate in each iteration for a given size and alphabet.
     *
     * @param size The length of the generated string.
     * @param alphabet The set of characters to use for generating the string.
     * @param additionalBytesFactor The additional bytes factor. Default value is calculated using `calculateAdditionalBytesFactor()`.
     * @return The number of random bytes to generate in each iteration.
     * @throws IllegalArgumentException if the alphabet is empty or larger than 256 characters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStep(size: Int, alphabet: String, additionalBytesFactor: Double = calculateAdditionalBytesFactor(alphabet)): Int {
        val symbolCount = symbolCount(alphabet)
        return stepFor(size, symbolCount, maskFor(symbolCount), additionalBytesFactor)
    }

    private fun buildId(size: Int, alphabet: String, symbols: List<String>?, symbolCount: Int, mask: Int, step: Int, random: Random): String {
        // Fast path: when every masked byte is guaranteed to be a valid index (mask < symbol count,
        // which is the case for a power-of-two alphabet under calculateMask), no byte is ever
        // rejected, so exactly `size` random bytes are mapped in a single pass.
        if (symbols == null && mask < symbolCount) {
            val bytes = random.nextBytes(size)
            return CharArray(size) { alphabet[bytes[it].toInt() and mask] }.concatToString()
        }

        val idBuilder = StringBuilder(size)
        val bytes = ByteArray(step)
        var symbolsAdded = 0
        while (true) {
            random.nextBytes(bytes)
            for (i in bytes.indices) {
                val index = bytes[i].toInt() and mask
                if (index < symbolCount) {
                    if (symbols == null) idBuilder.append(alphabet[index]) else idBuilder.append(symbols[index])
                    if (++symbolsAdded == size) {
                        return idBuilder.toString()
                    }
                }
            }
        }
    }

    // One string per symbol when the alphabet contains surrogate pairs (emoji and other symbols outside
    // the BMP), or null when every symbol is a single Char.
    private fun symbolsOf(alphabet: String): List<String>? {
        if (alphabet.none { it.isSurrogate() }) return null
        val symbols = ArrayList<String>(alphabet.length)
        var i = 0
        while (i < alphabet.length) {
            val isPair = alphabet[i].isHighSurrogate() && i + 1 < alphabet.length && alphabet[i + 1].isLowSurrogate()
            require(isPair || !alphabet[i].isSurrogate()) { "alphabet must not contain unpaired surrogates." }
            val width = if (isPair) 2 else 1
            symbols.add(alphabet.substring(i, i + width))
            i += width
        }
        return symbols
    }

    private fun symbolCount(alphabet: String): Int = symbolsOf(alphabet)?.size ?: alphabet.length

    private fun maskFor(symbolCount: Int): Int {
        require(symbolCount in 1..256) { "alphabet must contain between 1 and 256 symbols." }
        // `or 1` keeps the argument to countLeadingZeroBits non-zero for a single-symbol alphabet
        // (where symbolCount - 1 == 0). Without it, countLeadingZeroBits() == 32 makes the shift
        // amount negative and the mask overflows to -1. For two or more symbols the highest set bit
        // is unchanged, so this has no effect on the result.
        return (2 shl (Int.SIZE_BITS - 1 - ((symbolCount - 1) or 1).countLeadingZeroBits())) - 1
    }

    private fun stepFor(size: Int, symbolCount: Int, mask: Int, additionalBytesFactor: Double): Int =
        ceil(additionalBytesFactor * mask * size / symbolCount).toInt()
}
