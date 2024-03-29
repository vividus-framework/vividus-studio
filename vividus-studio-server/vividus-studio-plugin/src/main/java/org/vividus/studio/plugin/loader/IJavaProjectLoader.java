/*-
 * *
 * *
 * Copyright (C) 2020 the original author or authors.
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

package org.vividus.studio.plugin.loader;

import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;

public interface IJavaProjectLoader
{
    Optional<IJavaProject> load(String uri, Consumer<String> onInfo, Consumer<String> onLoad, Consumer<String> onError);

    void reload(IProject project, Consumer<String> onInfo, Consumer<String> onError) throws Exception;

    enum Event
    {
        LOADED, CORRUPTED, INFO
    }
}
