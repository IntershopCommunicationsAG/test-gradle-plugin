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

import groovy.transform.CompileStatic

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FieldInfo

/**
 * {@link @TestDir} extension.
 */
@CompileStatic
class TestDirExtension extends AbstractAnnotationDrivenExtension<TestDir>
{
    @Override    
    void visitFieldAnnotation ( TestDir annotation, FieldInfo field )
    {
        String baseDir = annotation.baseDir();
        
        if (baseDir.length() == 0)
        {
            if (annotation.useTempDirAsBase()) {
                baseDir = System.properties['java.io.tmpdir']   
            } else {            
                baseDir = System.properties['intershop.test.base.dir'] ?: 'build/test-working'
            }            
        }
        
        final interceptor = new TestDirInterceptor(baseDir, annotation.clean(), field.name, field.shared, annotation.overwrite(), annotation.large())
        
        if (field.shared) {
            field.parent.addSetupSpecInterceptor( interceptor )
        } else {            
            field.parent.addSetupInterceptor( interceptor )
        }        
    }
}
