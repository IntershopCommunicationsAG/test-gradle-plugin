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
package com.intershop.gradle.test


import groovy.util.logging.Slf4j

/**
 * This abstract spec implements a special
 * method and a setup, needed for the Gradle test kit.
 * Furthermore it contains some helper methods for
 * Gradle plugin testing.
 */
@Slf4j
abstract class AbstractIntegrationGroovySpec extends AbstractIntegrationSpec {

    /**
     * Creates a classpath from plugin resources
     * and creates an empty build.gradle
     */
    def setup() {
        buildFile = file('build.gradle')
        settingsFile = file('settings.gradle')
    }

    /**
     * Test helper method creates a directory for a subproject with a defined project path.
     *
     * @param projectPath       eg. 'main_project:project1'
     * @param settingsGradle    settings gradle file object of the root project
     * @return                  the file object for the project folder of the subproject
     */
    protected File createSubProject(String projectPath, def buildFileContent) {
        File f = directory(projectPath.replaceAll(':','/'))

        if(buildFileContent) {
            file('build.gradle', f) << buildFileContent.stripIndent()
        }

        settingsFile << """
            include '${projectPath}'
        """.stripIndent()
        return f
    }
}
