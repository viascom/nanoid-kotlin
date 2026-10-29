# Security

Only the newest release receives fixes. Report a vulnerability privately through
[GitHub's security advisories](https://github.com/viascom/nanoid-kotlin/security/advisories/new),
not in a public issue.

By default, IDs come from each platform's cryptographically secure random generator through
[cryptography-random](https://github.com/whyoleg/cryptography-kotlin). If you pass your own `random`, the IDs are
only as unpredictable as that generator. A seeded `kotlin.random.Random` is meant for tests, not for IDs that must
be hard to guess.
