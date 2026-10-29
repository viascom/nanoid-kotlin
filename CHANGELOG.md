# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/). Each release links to its full release notes.

## Unreleased

### Added

- `generatePrefixed()` for typed IDs such as `usr_W9SPTSD4dQk2N5S39mLnT`, matching the prefix support in
  nanoid-postgres and nanoid-mysql-mariadb. The prefix does not count towards `size`.
- Wasm/WASI and Android Native (`androidNativeArm32`, `androidNativeArm64`, `androidNativeX86`, `androidNativeX64`)
  targets.
- The published javadoc jars contain the generated API reference.
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, issue and pull request templates, and this changelog.

### Changed

- `generateOptimized()` validates its arguments and throws `IllegalArgumentException` for a negative `size`, an
  alphabet that is empty or longer than 256 symbols, a `step` of zero or less, or a `mask` outside 0..255 or missing a
  bit up to the highest alphabet index. A `size` of 0 returns an empty string.
- `calculateMask()`, `calculateStep()` and `calculateAdditionalBytesFactor()` throw `IllegalArgumentException` for an
  empty or over-256 alphabet, and `generate()` rejects a non-finite `additionalBytesFactor`.
- Built with Kotlin 2.4.21 and Gradle 9.8.2.
- The license header in every source file is now the standard Apache License 2.0 notice. The license itself is
  unchanged.

### Fixed

- `generateOptimized()` no longer hangs for a `size` of 0 or a `step` of 0, and no longer silently produces IDs from
  part of the alphabet when the mask is too small.

## [2.0.1] - 2026-08-09

### Security

- Raised the floors of five npm packages in the Kotlin/JS build toolchain. None of them reach the published artifact.

## [2.0.0] - 2026-07-18

### Breaking

- The `random` parameter is `kotlin.random.Random` instead of `java.util.Random`, and the `java.util.Random`
  overloads are gone. Seeded IDs differ from 1.x.

### Added

- Kotlin Multiplatform: JVM, JS, Wasm/JS, iOS, macOS, tvOS, watchOS, Linux and Windows from one artifact.
- Alphabets with 256 symbols.

### Changed

- The default random source is `CryptographyRandom.Default`, a CSPRNG on every platform.
- Faster generation for power-of-two alphabets.

### Fixed

- Single-character alphabets no longer crash.

## [1.0.1] - 2024-01-21

### Changed

- Better usage from Java through static methods and overloads.
- The JVM target is 17 instead of 19.

## [1.0.0] - 2023-11-02

- First release.

[2.0.1]: https://github.com/viascom/nanoid-kotlin/releases/tag/2.0.1
[2.0.0]: https://github.com/viascom/nanoid-kotlin/releases/tag/2.0.0
[1.0.1]: https://github.com/viascom/nanoid-kotlin/releases/tag/1.0.1
[1.0.0]: https://github.com/viascom/nanoid-kotlin/releases/tag/1.0.0
