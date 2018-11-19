import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.asciidoctor.gradle.AsciidoctorTask
import org.asciidoctor.gradle.AsciidoctorExtension
import java.util.Date

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.asciidoctor:asciidoctor-gradle-plugin:1.5.9.2")
    }
}

plugins {
    // project plugins
    groovy
    // test coverage
    jacoco

    // ide plugin
    idea

    // plugin for documentation
    id("org.asciidoctor.convert") version "1.5.9.2"

    // publish plugin
    `maven-publish`

    // plugin for publishing to jcenter
    id("com.jfrog.bintray") version "1.8.4"
}

group = "com.intershop.gradle.test"
description = "Gradle test library - test extension for Gradle plugin builds"

version = "3.3.0-SNAPSHOT"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}



tasks.withType<Test>().configureEach {
    // Gradle versions for test
    if (System.getProperty("GRADLETEST_VERSION").isNullOrEmpty()) {
        systemProperty("intershop.gradle.versions", System.getProperty("GRADLETEST_VERSION"))
    } else {
        systemProperty("intershop.gradle.versions", "4.9,5.0-rc-3")
    }

    systemProperty("intershop.test.base.dir", (File(project.buildDir, "test-working")).absolutePath)
}

task("copyAsciiDoc") {

    val outputDir = file("${buildDir}/tmp/asciidoctorSrc")
    val inputFiles = fileTree(mapOf("dir" to rootDir, "include" to listOf("**/*.asciidoc")))

    inputs.files.plus( inputFiles )
    outputs.dir( outputDir )

    doLast {
        outputDir.mkdir()

        project.copy {
            from(inputFiles)
            into(outputDir)
        }
    }
}

configure<AsciidoctorExtension> {
    noDefaultRepositories = true
}

tasks {
    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        sourceDir = file("${buildDir}/tmp/asciidoctorSrc")
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        backends("html5", "docbook")
        options = mapOf( "doctype" to "article",
                         "ruby"    to "erubis")
        attributes = mapOf(
                "latestRevision"        to  project.version,
                "toc"                   to "left",
                "toclevels"             to "2",
                "source-highlighter"    to "coderay",
                "icons"                 to "font",
                "setanchors"            to "true",
                "idprefix"              to "asciidoc",
                "idseparator"           to "-",
                "docinfo1"              to "true")
    }

    withType<JacocoReport> {
        reports {
            xml.isEnabled = true
            html.isEnabled = true

            html.destination = File(project.buildDir, "jacocoHtml")
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    withType<Test> {
        dependsOn("jar")
    }

    getByName("bintrayUpload")?.dependsOn("asciidoctor")
    getByName("publishToMavenLocal")?.dependsOn("asciidoctor")
}

val sourcesJar = task<Jar>("sourceJar") {
    description = "Creates a JAR that contains the source code."

    from(sourceSets.getByName("main").allSource)
    classifier = "sources"
}

val groovydocJar = task<Jar>("javadocJar") {
    dependsOn("groovydoc")
    description = "Creates a JAR that contains the javadocs."

    from(tasks.getByName("groovydoc"))
    classifier = "javadoc"
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])
            artifact(sourcesJar)
            artifact(groovydocJar)

            artifact(File(buildDir, "asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom.withXml {
                val root = asNode()
                root.appendNode("name", project.name)
                root.appendNode("description", project.description)
                root.appendNode("url", "https:/gitlab.intershop.de/IntershopGradlePlugins/${project.name}")

                val scm = root.appendNode( "scm" )
                scm.appendNode( "url", "https://gitlab.intershop.de/IntershopGradlePlugins/${project.name}")
                scm.appendNode( "connection", "scm:git:https://gitlab.intershop.de/IntershopGradlePlugins/${project.name}.git")

                val org = root.appendNode( "organization" )
                org.appendNode( "name", "Intershop Communications" )
                org.appendNode( "url", "http://intershop.com" )

                val license = root.appendNode( "licenses" ).appendNode( "license" );
                license.appendNode( "name", "Apache License, Version 2.0" )
                license.appendNode( "url", "http://www.apache.org/licenses/LICENSE-2.0" )
                license.appendNode( "distribution", "repo" )
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    setPublications("intershopMvn")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {

        repo = "GradlePlugins"
        name = project.name
        userOrg = "intershopcommunicationsag"

        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"

        desc = project.description
        websiteUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        issueTrackerUrl = "https://github.com/IntershopCommunicationsAG/${project.name}/issues"

        setLabels("intershop", "gradle", "plugin", "test")
        publicDownloadNumbers = true

        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = "${project.description} ${project.version}"
            released  = Date().toString()
            vcsTag = project.version.toString()
        })
    })
}

repositories {
    jcenter()
}

dependencies {
    compile("org.spockframework:spock-core:1.2-groovy-2.5") {
        exclude(group = "org.codehaus.groovy")
    }

    compile("commons-io:commons-io:2.2")
    compile("com.sun.xml.bind:jaxb-impl:2.2.3")

    compile(gradleTestKit())
}

