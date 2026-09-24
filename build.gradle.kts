import org.asciidoctor.gradle.jvm.AsciidoctorTask
import io.gitee.pkmer.enums.PublishingType

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
    `jvm-test-suite`
    // project plugins
    groovy
    // test coverage
    jacoco

    // ide plugin
    idea

    // plugin for documentation
    // NOTE: 4.0.5 (Aug 2025) is the latest release; its internal 'grolifant' library still calls the
    // deprecated StartParameter.isConfigurationCacheRequested, which will be removed in Gradle 10.
    // There is no alternative plugin (the xbib fork is broken on Gradle 9, all other asciidoc
    // plugins are generators, not renderers). An org.asciidoctor 5.0.0-alpha.1 line exists since
    // Sep 2025, so a final 5.x is expected to be available by the time Gradle 10 is released -
    // upgrade to it then.
    id("org.asciidoctor.jvm.convert") version "4.0.5"

    // publish plugin
    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing

    id("io.gitee.pkmer.pkmerboot-central-publisher") version "1.1.1"
}

group = "com.intershop.gradle.test"
description = "Gradle test library - test extension for Gradle plugin builds"
// apply gradle property 'projectVersion' to project.version, default to 'LOCAL'
val projectVersion = project.findProperty("projectVersion") as String?
version = projectVersion ?: "LOCAL"

val sonatypeUsername = project.findProperty("sonatypeUsername") as String?
val sonatypePassword = project.findProperty("sonatypePassword") as String?

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // don't change -> will require all dependent plugins to also be changed
    }
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

/*
 * Gradle 9.7.1 bundles Groovy 4.0.32 and 'gradleTestKit()' puts the whole Gradle distribution -
 * including that bundled groovy jar - on the compile classpath. The Groovy plugin's automatic
 * groovyClasspath inference therefore picks up Groovy 4, which makes Spock's global AST transform
 * (spock-bom 2.4-groovy-5.0) abort with IncompatibleGroovyVersionException.
 *
 * Fix: use a dedicated, isolated configuration that contains *only* Groovy 5 and use it as the
 * compiler classpath, so the Groovy compiler and Spock's AST transform both see Groovy 5.
 */
val groovyCompiler: Configuration = configurations.create("groovyCompiler") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

tasks.withType<GroovyCompile>().configureEach {
    groovyClasspath = groovyCompiler
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useSpock()

            dependencies {
                implementation(gradleTestKit())
            }

            targets {
                all {
                    testTask.configure {
                        // Gradle versions for test
                        systemProperty("intershop.gradle.versions", "8.5,8.10.2,9.1.0,9.7.1")
                        options {
                            testLogging.showStandardStreams = true
                        }
                    }
                }
            }
        }
    }
}

tasks {
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

        setOptions(mapOf(
            "doctype"               to "article",
            "ruby"                  to "erubis"
        ))
        setAttributes(mapOf(
            "latestRevision"        to project.version,
            "toc"                   to "left",
            "toclevels"             to "2",
            "source-highlighter"    to "coderay",
            "icons"                 to "font",
            "setanchors"            to "true",
            "idprefix"              to "asciidoc",
            "idseparator"           to "-",
            "docinfo1"              to "true"
        ))
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
            html.outputLocation.set(project.layout.buildDirectory.dir("jacocoHtml"))
        }

        dependsOn(test)
    }

    getByName("jar").dependsOn("asciidoctor")
}

val stagingRepoDir = project.layout.buildDirectory.dir("stagingRepo")

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
        }
        withType<MavenPublication>().configureEach {
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
        repositories {
            maven {
                name = "LOCAL"
                url = stagingRepoDir.get().asFile.toURI()
            }
        }
    }
}

pkmerBoot {
    sonatypeMavenCentral{
        // the same with publishing.repositories.maven.url in the configuration.
        stagingRepository = stagingRepoDir

        /**
         * get username and password from
         * <a href="https://central.sonatype.com/account"> central sonatype account</a>
         */
        username = sonatypeUsername
        password = sonatypePassword

        // Optional the publishingType default value is PublishingType.AUTOMATIC
        publishingType = PublishingType.USER_MANAGED
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

// dependency versions
val groovyVersion = "5.1.2"
val spockVersion = "2.4-groovy-5.0"

dependencies {
    api(platform("org.spockframework:spock-bom:$spockVersion"))
    // groovy-bom aligns all groovy modules on one version - spock-bom would otherwise pull an older
    // Groovy transitively, which would not match the groovyCompiler classpath below
    api(platform("org.apache.groovy:groovy-bom:$groovyVersion"))
    api("org.apache.groovy:groovy")
    api("org.spockframework:spock-core")
    api("org.spockframework:spock-junit4")
    api("commons-io:commons-io:2.22.0")
    api("com.sun.xml.bind:jaxb-impl:4.0.9")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.5")
    implementation("org.junit.jupiter:junit-jupiter:6.1.3")

    implementation(gradleTestKit())

    // isolated Groovy compiler classpath - see the groovyCompiler configuration above
    groovyCompiler(platform("org.apache.groovy:groovy-bom:$groovyVersion"))
    groovyCompiler("org.apache.groovy:groovy")
    groovyCompiler("org.apache.groovy:groovy-ant")
    groovyCompiler("org.apache.groovy:groovy-json")
    groovyCompiler("org.apache.groovy:groovy-xml")
    groovyCompiler("org.apache.groovy:groovy-templates")
}

