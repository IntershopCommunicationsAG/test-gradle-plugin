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

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Creates a file with all dependencies of the plugin classpath
 */
class ClasspathManifest extends DefaultTask {

    @InputFiles
    FileCollection inputFiles

    @OutputDirectory
    File outputDir

    @TaskAction
    createClassPathManifest() {
        outputDir.mkdirs()
        project.file("${outputDir}/plugin-classpath.txt").text = inputFiles.join('\n')
    }
}
