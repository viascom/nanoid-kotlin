import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    kotlin("multiplatform") version "2.4.10"
    id("com.vanniktech.maven.publish") version "0.37.0"
}

// Security floors for npm packages in the Kotlin/JS build toolchain
// (kotlin-js-store/yarn.lock). None of these reach the published artifact: the JS
// package declares no dependencies, and they only run webpack/karma/mocha at build
// time. They are pinned here rather than in the lockfile alone because
// `kotlinUpgradeYarnLock` keeps any entry that already satisfies its declared range,
// so a plain lock refresh leaves the vulnerable version in place.
//
// Some of these override the range their parent asks for (Dependabot's patched
// version is a major bump for the parent), so the yarn resolution is the only way
// to reach them without also upgrading the parent.
//
// NOTE: `rootPackageJson` does not treat resolutions as a task input, so after
// editing this map run `./gradlew kotlinUpgradeYarnLock --rerun-tasks`.
val npmSecurityFloors = mapOf(
    // host confusion; ajv <- schema-utils <- webpack. Parent asks ^3.0.1.
    // GHSA-7p8r-x3mc-p8w7 (CVE-2026-18446), GHSA-v2hh-gcrm-f6hx (CVE-2026-16221)
    "fast-uri" to "^3.1.5",
    // minimatch@^9 asks ^2.0.2; 2.1.3 is a patch bump. GHSA-mh99-v99m-4gvg
    "brace-expansion" to "^2.1.3",
    // mocha asks ^6.0.2, patched is 7.0.5. GHSA-5c6j-r48x-rmvq, GHSA-qj8w-gfj5-8c6v
    "serialize-javascript" to "^7.0.5",
    // mocha asks ^7.0.0, patched is 8.0.3. GHSA-73rr-hh4g-fpgx
    "diff" to "^8.0.3",
    // KGP pins webpack exactly (5.101.3); patched is 5.104.1. Held to the 5.104
    // minor on purpose: a caret floats to the latest 5.x, which is a much larger
    // drift from the version KGP is tested against than these two low-severity
    // advisories justify. GHSA-8fgc-7cc6-rx7x, GHSA-38r7-794h-5758
    "webpack" to "~5.104.1",
)

rootProject.plugins.withType<YarnPlugin> {
    val yarn = rootProject.the<YarnRootExtension>()
    npmSecurityFloors.forEach { (pkg, version) -> yarn.resolution(pkg, version) }
}

val major = 2
val minor = 0
val patch = 0

val isCiServer = System.getenv("GITHUB_ACTIONS") != null || System.getProperty("GITHUB_ACTIONS") != null

group = "io.viascom.nanoid"
version = "$major.$minor.$patch${if (isCiServer) "" else "-SNAPSHOT"}"
logger.lifecycle("Version of this build: $version")

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm()

    js {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs()
    }

    macosArm64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation("dev.whyoleg.cryptography:cryptography-random:0.6.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Implementation-Version"] = project.version
    }
}

mavenPublishing {
    // Uploads and validates on the Central Portal; releasing stays a manual
    // click on central.sonatype.com (deliberate: spec section 2.4).
    publishToMavenCentral()

    // GPG signing stays CI-only, driven by the signing.* properties the
    // publish workflow passes (secring.gpg based, unchanged mechanism).
    if (isCiServer) {
        signAllPublications()
    }

    coordinates("io.viascom.nanoid", "nanoid", version.toString())

    pom {
        name.set("nanoid")
        description.set("A tiny, secure, URL-friendly, unique string ID generator for Kotlin Multiplatform.")
        url.set("https://github.com/viascom/nanoid-kotlin")

        licenses {
            license {
                name.set("Apache-2.0 license")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        scm {
            url.set("https://github.com/viascom/nanoid-kotlin")
            connection.set("scm:git://github.com/viascom/nanoid-kotlin.git")
            developerConnection.set("scm:git://github.com/viascom/nanoid-kotlin.git")
        }

        developers {
            developer {
                id.set("itsmefox")
                name.set("Patrick Bösch")
                email.set("patrick.boesch@viascom.email")
                organizationUrl.set("https://viascom.io/")
            }
            developer {
                id.set("nik-sta")
                name.set("Nikola Stankovic")
                email.set("nikola.stankovic@viascom.email")
                organizationUrl.set("https://viascom.io/")
            }
        }
    }
}
