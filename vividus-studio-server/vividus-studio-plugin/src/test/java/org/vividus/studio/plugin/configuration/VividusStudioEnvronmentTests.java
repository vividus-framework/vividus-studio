/*-
 * *
 * *
 * Copyright (C) 2020 - 2023 the original author or authors.
 * *
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
 * *
 */

package org.vividus.studio.plugin.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.jupiter.api.Test;

class VividusStudioEnvronmentTests
{
    @Test
    void shouldReturnProject()
    {
        IJavaProject javaProject = mock();
        IProject project = mock();
        when(javaProject.getProject()).thenReturn(project);

        VividusStudioEnvronment envronment = new VividusStudioEnvronment();
        envronment.setJavaProject(javaProject);

        assertEquals(javaProject, envronment.getJavaProject());
        assertEquals(project, envronment.getProject());
    }
}
