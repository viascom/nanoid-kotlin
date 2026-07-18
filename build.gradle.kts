import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.4.10"
    id("com.vanniktech.maven.publish") version "0.37.0"
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

    macosX64()
    macosArm64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()
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
