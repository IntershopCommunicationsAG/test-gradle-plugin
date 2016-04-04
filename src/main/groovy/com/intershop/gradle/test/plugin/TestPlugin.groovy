/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.gradle.test.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test

/**
 * Plugin for special test infrastructure.
 */
class TestPlugin implements Plugin<Project> {

    public final static String CLASSPATHTASK_NAME = 'createClasspathManifest'

    public void apply(Project project) {
        // add task and dependencies
        project.plugins.withId('java') {
            Task task = project.getTasks().create(CLASSPATHTASK_NAME, ClasspathManifest.class).configure {
                inputFiles = project.sourceSets.main.runtimeClasspath
                outputDir = new File(project.buildDir, CLASSPATHTASK_NAME)
            }
            task.group = 'Intershop Test Plugin'

            File src = project.file(this.class.getProtectionDomain().codeSource.location)

            project.dependencies {
                testCompile project.files(src)
                testCompile gradleTestKit()
                testRuntime project.files(task)
            }
        }

        project.tasks.withType(Test) {
            systemProperties['intershop.test.base.dir'] = (new File(project.buildDir, 'test-working')).absolutePath
        }
    }
}
