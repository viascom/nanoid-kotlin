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

import org.jetbrains.annotations.NotNull
import java.security.SecureRandom
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil

/**
 * NanoId is a utility object providing functions for generating secure, URL-friendly, unique identifiers.
 *
 * The object offers methods for generating random strings with adjustable parameters like size, alphabet,
 * overhead factor, and a custom random number generator.
 *
 * Example usage in Kotlin:
 * ```kotlin
 * val id = NanoId.generate()
 * ```
 *
 * Example usage in Java:
 * ```java
 * String id = NanoId.generate();
 * ```
 */
object NanoId {

    /**
     * Generates a random string based on specified or default parameters.
     *
     * @param size The desired length of the generated string. Default is 21.
     * @param alphabet The set of characters to choose from for generating the string. Default includes alphanumeric characters along with "_" and "-".
     * @param additionalBytesFactor The additional bytes factor used for calculating the step size. Default is 1.6.
     * @param random The random number generator to use. Default is `SecureRandom`.
     * @return The generated random string.
     * @throws IllegalArgumentException if the alphabet is empty or larger than 255 characters, or if the size is not greater than zero.
     */
    @JvmOverloads
    @JvmStatic
    fun generate(
        @NotNull size: Int = 21,
        @NotNull alphabet: String = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
        @NotNull additionalBytesFactor: Double = 1.6,
        @NotNull random: Random = SecureRandom()
    ): String {
        require(alphabet.isNotEmpty() && alphabet.length < 256) { "Alphabet must contain between 1 and 255 symbols." }
        require(size > 0) { "Size must be greater than zero." }
        require(additionalBytesFactor >= 1) { "additionalBytesFactor must be greater or equal to 1." }

        val mask = calculateMask(alphabet)
        val step = calculateStep(size, alphabet, additionalBytesFactor, mask)

        return generateOptimized(size, alphabet, mask, step, random)
    }

    /**
     * Generates an optimized random string of a specified size using the given alphabet, mask, and step.
     * Optionally, you can specify a custom random number generator. This optimized version is designed for
     * higher performance and lower memory overhead.
     *
     * @param size The desired length of the generated string.
     * @param alphabet The set of characters to choose from for generating the string.
     * @param mask The mask used for mapping random bytes to alphabet indices. Should be `(2^n) - 1` where `n` is a power of 2 less than or equal to the alphabet size.
     * @param step The number of random bytes to generate in each iteration. A larger value may speed up the function but increase memory usage.
     * @param random The random number generator. Default is `SecureRandom`.
     * @return The generated optimized string.
     */
    @JvmOverloads
    @JvmStatic
    fun generateOptimized(
        @NotNull size: Int,
        @NotNull alphabet: String,
        @NotNull mask: Int,
        @NotNull step: Int,
        @NotNull random: Random = SecureRandom()
    ): String {
        require(size > 0) { "Size must be greater than zero." }

        val idBuilder = StringBuilder(size)
        val bytes = ByteArray(step)
        while (true) {
            random.nextBytes(bytes)
            for (i in 0 until step) {
                val alphabetIndex = bytes[i].toInt() and mask
                if (alphabetIndex < alphabet.length) {
                    idBuilder.append(alphabet[alphabetIndex])
                    if (idBuilder.length == size) {
                        return idBuilder.toString()
                    }
                }
            }
        }
    }

    /**
     * Calculates the mask used to map random bytes to indices in the alphabet.
     *
     * @param alphabet The set of characters to use for generating the string.
     * @return The calculated mask value.
     */
    @JvmStatic
    fun calculateMask(@NotNull alphabet: String): Int = (1 shl (Integer.SIZE - Integer.numberOfLeadingZeros(alphabet.length - 1))) - 1

    /**
     * Calculates the optimal additional bytes factor needed for the generation of the step size, which is used to generate random bytes in each iteration.
     *
     * @param alphabet The set of characters to use for generating the string.
     * @return The additional bytes factor, rounded to two decimal places.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateAdditionalBytesFactor(@NotNull alphabet: String, @NotNull mask: Int = calculateMask(alphabet)): Double =
        (1 + abs((mask - alphabet.length.toDouble()) / alphabet.length)).round(2)

    /**
     * Calculates the number of random bytes to generate in each iteration for a given size and alphabet.
     *
     * @param size The length of the generated string.
     * @param alphabet The set of characters to use for generating the string.
     * @param additionalBytesFactor The additional bytes factor. Default value is calculated using `calculateAdditionalBytesFactor()`.
     * @return The number of random bytes to generate in each iteration.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStep(
        @NotNull size: Int,
        @NotNull alphabet: String,
        @NotNull additionalBytesFactor: Double = calculateAdditionalBytesFactor(alphabet),
        @NotNull mask: Int = calculateMask(alphabet)
    ): Int = ceil(additionalBytesFactor * mask * size / alphabet.length).toInt()

    @JvmSynthetic
    internal fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
