# Contributing to nanoid-kotlin

Contributions are welcome. For anything bigger than a small fix, open an issue first so we can agree on the
approach before you write code. Report security problems privately as described in [SECURITY.md](SECURITY.md),
not in a public issue. Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## Building

You need JDK 17 or newer; Gradle downloads the toolchain it needs.

```shell
./gradlew jvmTest   # quick check on the JVM
./gradlew build     # all tests this machine can run, plus the ABI check
```

The Apple test binaries only link with a full Xcode installation. Without it, the build still compiles every
Apple target and CI runs their tests on macOS.

## Rules for changes

- **Public API.** The library uses explicit API mode, so every public declaration needs a visibility modifier
  and KDoc. After a deliberate API change, run `./gradlew updateKotlinAbi` and commit the updated files in
  `api/`. `./gradlew build` fails while the dump and the code disagree. Removing or changing public API needs a
  major version.
- **Generated IDs.** The golden tests in `NanoIdTest` pin the IDs produced from a seeded `Random`. Changing them
  changes output for users who pin IDs in their own tests, so it also needs a major version.
- **Tests.** Every behaviour change or fix comes with a test in `src/commonTest`, so it runs on every target.
- **Upstream parity.** Changes that follow [ai/nanoid](https://github.com/ai/nanoid) should link the upstream
  commit or release in the pull request.
- **Commits.** Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `build:`,
  `ci:`, `docs:`, `test:`, `chore:`), and add `!` for breaking changes.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
