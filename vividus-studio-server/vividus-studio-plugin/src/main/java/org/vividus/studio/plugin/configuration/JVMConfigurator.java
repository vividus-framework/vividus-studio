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

import static java.lang.String.format;
import static java.lang.System.getProperty;

import java.io.File;

import com.google.inject.Singleton;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JVMConfigurator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JVMConfigurator.class);

    public void configureDefaultJvm() throws CoreException
    {
        IVMInstall defaultJvm = JavaRuntime.getDefaultVMInstall();

        if (defaultJvm == null)
        {
            IVMInstallType type = JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE);
            VMStandin vm = new VMStandin(type, "default-jvm-install");
            vm.setName(format("%s JDK %s", getProperty("java.vendor"), getProperty("java.version")));
            vm.setInstallLocation(new File(getProperty("java.home")));
            defaultJvm = vm.convertToRealVM();
            JavaRuntime.setDefaultVMInstall(defaultJvm, null);
        }

        LOGGER.atInfo().addArgument(defaultJvm::getName).log("Default JVM: {}");
    }
}
