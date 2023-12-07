import org.asciidoctor.gradle.jvm.AsciidoctorTask

/*
 * Copyright 2022 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    `java-library`
    // project plugins
    groovy
    // test coverage
    jacoco

    // ide plugin
    idea

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    // publish plugin
    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing
}

group = "com.intershop.gradle.test"
description = "Gradle test library - test extension for Gradle plugin builds"
// apply gradle property 'projectVersion' to project.version, default to 'LOCAL'
val projectVersion : String? by project
version = projectVersion ?: "LOCAL"

println("!!!kiese ${project.version}")

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

tasks {

    withType<Test>().configureEach {
        testLogging.showStandardStreams = true

        // Gradle versions for test
        systemProperty("intershop.gradle.versions", "8.4,8.5")
        systemProperty("intershop.test.base.dir", project.layout.buildDirectory.dir("test-working").get().asFile.absolutePath)

        useJUnitPlatform()
    }

    val asciidoctorSrc = project.layout.buildDirectory.dir("/tmp/asciidoctorSrc")

    register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val inputFiles = fileTree(rootDir) {
            include("**/*.asciidoc")
            exclude("build/**")
        }

        inputs.files.plus( inputFiles )
        outputs.dir( asciidoctorSrc )

        doFirst {
            asciidoctorSrc.get().asFile.mkdir()
        }

        from(inputFiles)
        into(asciidoctorSrc)
    }

    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        setSourceDir(asciidoctorSrc)
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        outputOptions {
            setBackends(listOf("html5", "docbook"))
        }

        options = mapOf(
            "doctype"               to "article",
            "ruby"                  to "erubis"
        )
        attributes = mapOf(
            "latestRevision"        to project.version,
            "toc"                   to "left",
            "toclevels"             to "2",
            "source-highlighter"    to "coderay",
            "icons"                 to "font",
            "setanchors"            to "true",
            "idprefix"              to "asciidoc",
            "idseparator"           to "-",
            "docinfo1"              to "true"
        )
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
            html.outputLocation.set(project.layout.buildDirectory.dir("jacocoHtml"))
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    getByName("jar").dependsOn("asciidoctor")
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])

            artifact(project.layout.buildDirectory.file("docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(project.layout.buildDirectory.file("docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                organization {
                    name.set("Intershop Communications AG")
                    url.set("http://intershop.com")
                }
                developers {
                    developer {
                        id.set("m-raab")
                        name.set("M. Raab")
                        email.set("mraab@intershop.de")
                    }
                }
                scm {
                    connection.set("https://github.com/IntershopCommunicationsAG/${project.name}.git")
                    developerConnection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform("org.spockframework:spock-bom:2.3-groovy-3.0"))
    api("org.spockframework:spock-core") {
        exclude(group = "org.codehaus.groovy")
    }
    api("org.spockframework:spock-junit4")
    api("commons-io:commons-io:2.14.0")
    api("com.sun.xml.bind:jaxb-impl:4.0.3")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.1")
    implementation("junit:junit:4.13.2")

    implementation(gradleTestKit())
}
