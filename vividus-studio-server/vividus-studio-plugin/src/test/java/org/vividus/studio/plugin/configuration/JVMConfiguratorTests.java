/*-
 * *
 * *
 * Copyright (C) 2020 - 2021 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JVMConfiguratorTests
{
    @Mock private IVMInstall defaultVm;
    @InjectMocks private JVMConfigurator jvmConfigurator;

    @Test
    void shouldConfigureDefaultJvm() throws CoreException
    {
        IVMInstallType type = mock(IVMInstallType.class);
        try (MockedStatic<JavaRuntime> javaRuntime = mockStatic(JavaRuntime.class);
             MockedConstruction<VMStandin> vmConstruction = mockConstruction(VMStandin.class,
                (mock, ctx) -> when(mock.convertToRealVM()).thenReturn(defaultVm)))
        {
            javaRuntime.when(JavaRuntime::getDefaultVMInstall).thenReturn(null);
            javaRuntime.when(() -> JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE)).thenReturn(type);

            jvmConfigurator.configureDefaultJvm();

            javaRuntime.verify(JavaRuntime::getDefaultVMInstall);
            javaRuntime.verify(() -> JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE));
            javaRuntime.verify(() -> JavaRuntime.setDefaultVMInstall(defaultVm, null));
            javaRuntime.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldUseExistingDefaultJvm() throws CoreException
    {
        try (MockedStatic<JavaRuntime> javaRuntime = mockStatic(JavaRuntime.class))
        {
            javaRuntime.when(JavaRuntime::getDefaultVMInstall).thenReturn(defaultVm);

            jvmConfigurator.configureDefaultJvm();

            javaRuntime.verify(JavaRuntime::getDefaultVMInstall);
            javaRuntime.verifyNoMoreInteractions();
        }
    }
}
