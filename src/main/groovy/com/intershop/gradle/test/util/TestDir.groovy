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
package com.intershop.gradle.test.util

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import org.spockframework.runtime.extension.ExtensionAnnotation

/**
 * Extension creating a separate test directory for each test method.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
@ExtensionAnnotation( TestDirExtension )
@interface TestDir {
    String  baseDir() default ''
    boolean clean()   default true
    boolean overwrite() default false
    boolean useTempDirAsBase() default false
    
    /**
     * <p>If set the test directory is expected to be large and is cleaned using OS commands.</p> 
     * <p><strong>ATTENTION: This does not work for long directories on Windows.</strong></p>
     * @return
     */
    boolean large() default false
}
