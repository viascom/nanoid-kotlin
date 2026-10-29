# Nano ID for Kotlin Multiplatform

<a href="https://github.com/viascom/nanoid-kotlin/releases"><img src="https://img.shields.io/maven-central/v/io.viascom.nanoid/nanoid" alt="Maven central"></a>
<a href="https://github.com/viascom/nanoid-kotlin/actions/workflows/build.yml"><img src="https://github.com/viascom/nanoid-kotlin/actions/workflows/build.yml/badge.svg" alt="Build"></a>
<img src="https://img.shields.io/badge/Kotlin-2.4.21-%238052ff?logo=kotlin" alt="Kotlin Version">
<img src="https://img.shields.io/badge/Multiplatform-JVM%20%7C%20JS%20%7C%20Wasm%20%7C%20iOS%20%7C%20macOS%20%7C%20tvOS%20%7C%20watchOS%20%7C%20Linux%20%7C%20Windows%20%7C%20Android%20Native-blue" alt="Kotlin Multiplatform">
<a href="http://www.apache.org/licenses/"><img src="https://img.shields.io/badge/license-Apache_2.0-blue.svg" alt="license Apache 2.0"></a>

_Inspired by the following parent project: [ai/nanoid](https://github.com/ai/nanoid)_

<img src="./logo.svg" align="right" alt="Nano ID logo by Anton Lovchikov" width="180" height="94">

A tiny, secure, URL-friendly, unique string ID generator for Kotlin Multiplatform.

> "An amazing level of senseless perfectionism, which is simply impossible not to respect."

* **Small.** Just a simple Kotlin object.
* **Safe.** Uses each platform's cryptographically secure random generator via [cryptography-random](https://github.com/whyoleg/cryptography-kotlin).
* **Multiplatform.** One artifact for JVM, JS (browser/Node), Wasm (JS and WASI), iOS, macOS, tvOS, watchOS, Linux, Windows and Android Native.
* **Short IDs.** It uses a larger alphabet than UUID (`A-Za-z0-9_-`). So ID size was reduced from 36 to 21 symbols.
* **Portable**. Nano ID was ported
  to [over 20 programming languages](https://github.com/ai/nanoid/blob/main/README.md#other-programming-languages).

## How to use

Add nanoid as a dependency to your project.

Gradle (Kotlin Multiplatform, in `commonMain`):
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.viascom.nanoid:nanoid:2.1.0")
        }
    }
}
```

Gradle (JVM project):
```kotlin
dependencies {
    implementation("io.viascom.nanoid:nanoid:2.1.0")
}
```

Maven:
```xml
<dependency>
    <groupId>io.viascom.nanoid</groupId>
    <artifactId>nanoid-jvm</artifactId>
    <version>2.1.0</version>
</dependency>
```

Start using it:

```kotlin
val id = NanoId.generate()
```

```kotlin
val idWithCustomLength = NanoId.generate(10)
val idWithCustomAlphabet = NanoId.generate(alphabet = "ABC123")
```

For typed IDs, `generatePrefixed()` puts a fixed marker in front, so anyone reading an ID can tell which entity it
belongs to. The prefix does not count towards `size`, so the ID is `prefix.length + size` characters long:

```kotlin
val userId = NanoId.generatePrefixed("usr_") // usr_W9SPTSD4dQk2N5S39mLnT
val orderId = NanoId.generatePrefixed("ord_", size = 10)
```

From Java, every function is a static method:

```java
String id = NanoId.generate();
String shortId = NanoId.generate(10, "ABC123");
String userId = NanoId.generatePrefixed("usr_");
```

The [API reference](https://javadoc.io/doc/io.viascom.nanoid/nanoid) lists every function and parameter.

## Migrating from 1.x

Version 2.0.0 is a Kotlin Multiplatform release. The API is unchanged for all default-parameter
call forms, from Kotlin and Java alike. One breaking change: the `random` parameter is now
`kotlin.random.Random` instead of `java.util.Random`, and the `java.util.Random` overloads are gone.
If you passed a custom random, wrap it:

```kotlin
// Kotlin
NanoId.generate(random = SecureRandom().asKotlinRandom())
```

```java
// Java
NanoId.generate(21, alphabet, 1.6, PlatformRandomKt.asKotlinRandom(new SecureRandom()));
```

Seeded `java.util.Random` streams from 1.x produce different ids than seeded
`kotlin.random.Random` streams; re-pin any golden values if you relied on them.

## Calculating the additional bytes factor for a custom alphabet

If you change the alphabet of the `NanoId.generate()` function, you could optimize the performance by calculating a new additional
bytes factor with the following function:

```kotlin
val bytesFactor = NanoId.calculateAdditionalBytesFactor("ABC123")
val id = NanoId.generate(21, "ABC123", bytesFactor)
```

Utilizing a custom-calculated additional bytes factor in `NanoId.generate()` enhances string generation performance. This factor
determines how many bytes are generated in a single batch, optimizing computational efficiency. Generating an optimal number
of bytes per batch minimizes redundant operations and conserves memory.

## Usage Guide: `NanoId.generateOptimized()`

The `NanoId.generateOptimized()` function is an advanced version of the `NanoId.generate()` function designed for higher performance and
lower memory overhead. While it provides a more efficient mechanism to generate unique identifiers, it assumes that you
know precisely how you want to use it.

🚫 **Warning**: `NanoId.generateOptimized()` skips the step calculation that `NanoId.generate()` does for you. `mask` must be
within 0..255 and contain every bit up to the highest alphabet index, or some symbols could never be produced;
`NanoId.calculateMask()` returns the smallest such mask. `step` must be greater than zero. It throws
`IllegalArgumentException` otherwise. A `size` of zero returns an empty string.

### Example Usage

Generate a NanoId String of length 10 using the default alphabet set:

```kotlin
val id = NanoId.generateOptimized(10, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 63, 16)
```

### Tips:

- **Performance**: This function is optimized for performance, so it's ideal for scenarios where high-speed ID
  generation is needed.
- **Alphabet Set**: The larger your alphabet set, the more unique your generated IDs will be, but also consider the mask
  and step parameters' adjustments.
- **Customization**: Feel free to adjust the parameters to suit your specific needs, but always be cautious about the
  values you're inputting.

By following this guide, you can seamlessly integrate the `NanoId.generateOptimized()` function into your projects and enjoy the
benefits of its optimized performance.

## Contributing

Bug reports, ideas and pull requests are welcome. [CONTRIBUTING.md](CONTRIBUTING.md) explains how to build the
project and what a change needs. Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md). Report
security problems privately as described in [SECURITY.md](SECURITY.md), and ask questions in
[Discussions](https://github.com/viascom/nanoid-kotlin/discussions). Changes per version are listed in the
[changelog](CHANGELOG.md).

## 🖥️ Authors

* **Patrick Bösch** - *Initial work* - [itsmefox](https://github.com/itsmefox)
* **Nikola Stanković** - *Initial work* - [nik-sta](https://github.com/nik-sta)

See also the list of [contributors](https://github.com/viascom/nanoid-kotlin/contributors) who participated in this
project. 💕

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
