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

import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification
/**
 * Create some basic tests that all plugins should pass
 */
abstract class AbstractProjectSpec extends Specification {

    /**
     * Project directory for tests
     */
    @TestDir
    File testProjectDir

    /**
     * Test name
     */
    @Rule
    TestName testName = new TestName()

    /**
     * Canonical name of the test name
     */
    protected String canonicalName

    /**
     * Test project
     */
    protected Project project

    /**
     * This method must return an instance of the plugin to be tested
     * @return  plugin instance of the new plugin
     */
    abstract Plugin getPlugin()

    /**
     * Setup method of this abstract spec
     * @return
     */
    def setup() {
        canonicalName = testName.getMethodName().replaceAll(' ', '-')
        project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()
    }

    def 'apply does not throw exceptions'() {
        when:
        plugin.apply(project)

        then:
        noExceptionThrown()
    }

    def 'apply is idempotent'() {
        when:
        plugin.apply(project)
        plugin.apply(project)

        then:
        noExceptionThrown()
    }

    def 'apply is fine on all levels of multiproject'() {
        given:
        def sub = createSubproject(project, 'sub')
        project.subprojects.add(sub)

        when:
        plugin.apply(project)
        plugin.apply(sub)

        then:
        noExceptionThrown()
    }

    def 'apply to multiple subprojects'() {
        given:
        def subprojectNames = ['sub1', 'sub2', 'sub3']

        subprojectNames.each { subprojectName ->
            def subproject = createSubproject(project, subprojectName)
            project.subprojects.add(subproject)
        }

        when:
        plugin.apply(project)

        subprojectNames.each { subprojectName ->
            def subproject = project.subprojects.find { it.name == subprojectName }
            plugin.apply(subproject)
        }

        then:
        noExceptionThrown()
    }

    /**
     * This method adds a sub project to the specified root/parent project.
     *
     * @param parentProject     parent or root project
     * @param name              name of the sub project
     * @return                  the project object of the sub project
     */
    protected Project createSubproject(Project parentProject, String name) {
        ProjectBuilder.builder().withName(name).withProjectDir(new File(testProjectDir, name)).withParent(parentProject).build()
    }
}
