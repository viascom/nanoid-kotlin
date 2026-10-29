# Contributing to nanoid-kotlin

Contributions are welcome. Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

- **Questions** belong in [Discussions](https://github.com/viascom/nanoid-kotlin/discussions).
- **Bugs and feature ideas** go into an [issue](https://github.com/viascom/nanoid-kotlin/issues/new/choose). For
  anything bigger than a small fix, agree on the approach there before you write code.
- **Security problems** must not go into a public issue; see [SECURITY.md](SECURITY.md).

## Development setup

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
- **Parity.** Keep the behaviour aligned with [ai/nanoid](https://github.com/ai/nanoid) and with the sibling
  projects [nanoid-postgres](https://github.com/viascom/nanoid-postgres) and
  [nanoid-mysql-mariadb](https://github.com/viascom/nanoid-mysql-mariadb) where it fits Kotlin. Link the upstream
  commit or release when you port something, and say why when a change only fits this library.
- **Documentation.** Update `README.md` for any user-visible change and add an entry under *Unreleased* in
  `CHANGELOG.md`.

## Commits and pull requests

- Open or pick an issue first, then branch as `<type>/<issue>-<short-name>`, e.g. `fix/12-negative-size`.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `build:`,
  `ci:`, `docs:`, `test:`, `chore:`, with `!` for a breaking change (`feat!:`). Reference the issue in the body with
  `Refs #12`.
- The pull request body says what changed and how you tested it, and ends with `Closes #12`.

## Releases (maintainers)

1. Move the *Unreleased* entries in `CHANGELOG.md` under the new version.
2. Set `major`, `minor` and `patch` in `build.gradle.kts` and the version in the `README.md` install snippets.
3. Merge to `main` and wait for the build to pass.
4. Publish a GitHub release with the tag and title `X.Y.Z` (no `v` prefix) and notes in the shape of the earlier
   releases. Publishing runs `publish.yml`, which uploads the artifacts to the Maven Central Portal.
5. Release the uploaded deployment on [central.sonatype.com](https://central.sonatype.com/); that step is manual.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
