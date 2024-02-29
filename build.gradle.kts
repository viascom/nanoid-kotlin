import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    id("maven-publish")
    signing
    id("com.android.library") version "8.2.2"
    kotlin("multiplatform") version "1.9.22"
}

val major by extra { 1 }
val minor by extra { 0 }
val patch by extra { 1 }
val isCiServer by extra { System.getenv("GITHUB_ACTIONS") != null || System.getProperty("GITHUB_ACTIONS") != null }

group = "io.viascom.nanoid"
version = "$major.$minor.$patch${if (isCiServer) "" else "-SNAPSHOT"}"
project.logger.lifecycle("Version of this build: $version")

android {
    namespace = "io.viascom.nanoid"
    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvm()
    js()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()
    androidTarget()
    linuxX64()
    mingwX64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("dev.whyoleg.cryptography:cryptography-core:0.3.0")
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
}

publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            name = "OSSRH"
            url = if (isCiServer) releaseRepo else snapshotRepo
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }

    publications {
        withType<MavenPublication> {
            pom {
                groupId = "io.viascom.nanoid"
                name = "nanoid"
                description = "."
                url = "https://github.com/viascom/nanoid-kotlin"
                packaging = "jar"

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
                    developer {
                        id.set("illyan")
                        name.set("Balázs Püspök-Kiss")
                        email.set("pkblazsak@gmail.com")
                        organizationUrl.set("https://github.com/HLCaptain")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf { isCiServer }
}