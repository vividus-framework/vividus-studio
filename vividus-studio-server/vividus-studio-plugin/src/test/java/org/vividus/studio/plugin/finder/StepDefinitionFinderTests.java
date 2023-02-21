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

package org.vividus.studio.plugin.finder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.studio.plugin.factory.StepDefinitionFactory;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.model.StepType;

@ExtendWith(MockitoExtension.class)
class StepDefinitionFinderTests
{
    private static final String GIVEN_FULL_NAME = "Given I param $param1";
    private static final String WHEN_FULL_NAME = "When I param $param1 and $param2";
    private static final String THEN_FULL_NAME = "Then I param $param1 and $param2 and $param3";

    private static final String COMPOSITE_JAVADOC = "Given I perform system initialization\nWhen I shutdown system\n"
            + "Then the system is inactive";
    private static final String MODULE_NAME = "module-name";

    private final StepDefinitionFinder finder = new StepDefinitionFinder(new StepDefinitionFactory());

    @Mock private IJavaProject root;

    @BeforeEach
    void beforeEach()
    {
        IProject project = mock();
        when(root.getProject()).thenReturn(project);
        when(project.getName()).thenReturn("name");
        IPath location = mock();
        when(project.getLocation()).thenReturn(location);
        when(location.toString()).thenReturn(getClass().getResource("/project").getFile());
    }

    @Test
    void testFind() throws IOException, CoreException
    {
        JarPackageFragmentRoot module = createStepsModule();
        IPackageFragment packageFragment = mock(IPackageFragment.class);

        // Mock java steps
        ClassFileMock stepsClass = createStepsClass();
        IMember classMember = mock(IMember.class);
        IJavaElement given = createStep("Given", "I param $param1", stepsClass.getClassFileBuffer(), 1, 2, true);
        IJavaElement when = createStep("When", "I param $param1 and $param2", stepsClass.getClassFileBuffer(), 2, 3,
                false);
        IJavaElement then = createStep("Then", "I param $param1 and $param2 and $param3",
                stepsClass.getClassFileBuffer(), 3, 4, false);

        when(root.getChildren()).thenReturn(new IJavaElement[] { module });
        when(module.getChildren()).thenReturn(new IJavaElement[] { packageFragment });
        when(packageFragment.getChildren()).thenReturn(new IJavaElement[] { stepsClass.getClassFile() });
        when(stepsClass.getClassFile().getChildren()).thenReturn(new IJavaElement[] { classMember });
        when(classMember.getChildren()).thenReturn(new IJavaElement[] { given, when, then });

        // Mock composite steps
        IJarEntryResource resource = mock(IJarEntryResource.class);
        IPackageFragment resourceFragment = mock(IPackageFragment.class);

        when(resource.getName()).thenReturn("composite.steps");
        when(packageFragment.getNonJavaResources()).thenReturn(new Object[] { resource });
        when(resource.getParent()).thenReturn(resourceFragment);
        mockModuleName(resourceFragment);

        InputStream inputStream = getClass().getResource("composite.steps").openStream();
        when(resource.getContents()).thenReturn(inputStream);

        List<StepDefinition> definitions = new ArrayList<>(finder.find(root));
        assertThat(definitions, hasSize(7));

        // assert java steps
        asserJavaStepDefinition(definitions.get(0), MODULE_NAME, StepType.GIVEN, GIVEN_FULL_NAME,
                "Javadoc for Given I param $param1", List.of(new Parameter(1, "$param1", 14)), true);
        asserJavaStepDefinition(definitions.get(1), MODULE_NAME, StepType.WHEN, WHEN_FULL_NAME,
                "Javadoc for When I param $param1 and $param2",
                List.of(new Parameter(1, "$param1",  13), new Parameter(2, "$param2", 25)), false);
        asserJavaStepDefinition(definitions.get(2), MODULE_NAME, StepType.THEN, THEN_FULL_NAME,
                "Javadoc for Then I param $param1 and $param2 and $param3", List.of(new Parameter(1, "$param1", 13),
                        new Parameter(2, "$param2", 25), new Parameter(3, "$param3", 37)),
                false);

        // assert static composite steps
        asserCompositeStepDefinition(definitions.get(3), MODULE_NAME, StepType.GIVEN, GIVEN_FULL_NAME, COMPOSITE_JAVADOC,
                List.of(new Parameter(1, "$param1", 14)), false);
        asserCompositeStepDefinition(definitions.get(4), MODULE_NAME, StepType.WHEN, WHEN_FULL_NAME, COMPOSITE_JAVADOC,
                List.of(new Parameter(1, "$param1", 13), new Parameter(2, "$param2", 25)), false);
        asserCompositeStepDefinition(definitions.get(5), MODULE_NAME, StepType.THEN, THEN_FULL_NAME, COMPOSITE_JAVADOC,
                List.of(new Parameter(1, "$param1", 13), new Parameter(2, "$param2", 25), new Parameter(3, "$param3", 37)),
                false);

        // assert static composite steps
        asserCompositeStepDefinition(definitions.get(6), "composite/composite.steps", StepType.THEN, THEN_FULL_NAME,
                COMPOSITE_JAVADOC, List
                .of(new Parameter(1, "$param1", 13), new Parameter(2, "$param2", 25), new Parameter(3, "$param3", 37)),
                true);
    }

    private void asserJavaStepDefinition(StepDefinition definition, String module, StepType type, String fullName,
            String javadoc, List<Parameter> parameters, boolean deprecated)
    {
        assertEquals(deprecated, definition.isDeprecated());
        assertFalse(definition.isComposite());
        assertFalse(definition.isDynamic());
        asserStepDefinition(definition, module, type, fullName, javadoc, parameters);
    }

    private void asserCompositeStepDefinition(StepDefinition definition, String module, StepType type, String fullName,
            String javadoc, List<Parameter> parameters, boolean dynamic)
    {
        assertTrue(definition.isComposite());
        assertEquals(dynamic, definition.isDynamic());
        asserStepDefinition(definition, module, type, fullName, javadoc, parameters);
    }

    private void asserStepDefinition(StepDefinition definition, String module, StepType type, String fullName,
            String javadoc, List<Parameter> parameters)
    {
        assertAll(() -> assertEquals(type, definition.getStepType()),
                () -> assertEquals(fullName, definition.getStepAsString()),
                () -> assertEquals(module, definition.getModule()),
                () -> assertEquals(javadoc, definition.getDocumentation()));
        List<Parameter> actualParameters = definition.getParameters();
        assertThat(actualParameters, hasSize(parameters.size()));
        IntStream.range(0, parameters.size()).forEach(index ->
        {
            Parameter actual = actualParameters.get(index);
            Parameter expected = parameters.get(index);
            assertAll(() -> assertEquals(expected.getIndex(), actual.getIndex()),
                    () -> assertEquals(expected.getStartAt(), actual.getStartAt()),
                    () -> assertEquals(expected.getName(), actual.getName()));
        });
    }

    private JarPackageFragmentRoot createStepsModule()
    {
        JarPackageFragmentRoot module = mock(JarPackageFragmentRoot.class);
        Manifest manifest = mock(Manifest.class);
        Attributes attributes = mock(Attributes.class);

        when(module.getManifest()).thenReturn(manifest);
        when(manifest.getMainAttributes()).thenReturn(attributes);
        when(attributes.getValue("Automatic-Module-Name")).thenReturn("org.vividus.module");

        return module;
    }

    private ClassFileMock createStepsClass() throws JavaModelException
    {
        IPackageFragment stepsClass = mock(IPackageFragment.class, withSettings().extraInterfaces(IClassFile.class));
        IClassFile classFile = (IClassFile) stepsClass;
        IJavaElement packageElement = mock(IJavaElement.class);
        IOpenable openable = mock(IOpenable.class);
        IBuffer buffer = mock(IBuffer.class);

        when(stepsClass.getElementType()).thenReturn(IJavaElement.CLASS_FILE);
        when(stepsClass.getElementName()).thenReturn("ClassWithSteps");
        when(stepsClass.getParent()).thenReturn(packageElement);
        mockModuleName(packageElement);
        when(classFile.getOpenable()).thenReturn(openable);
        when(openable.getBuffer()).thenReturn(buffer);

        return new ClassFileMock(stepsClass, buffer);
    }

    private void mockModuleName(IJavaElement packageElementMock)
    {
        IJavaElement moduleElement = mock(IJavaElement.class);
        when(packageElementMock.getParent()).thenReturn(moduleElement);
        when(moduleElement.getElementName()).thenReturn("module-name");
    }

    private IJavaElement createStep(String type, String naming, IBuffer classBufer, int from, int to,
            boolean deprecated) throws JavaModelException
    {
        IJavaElement javaElement = mock(IJavaElement.class, withSettings().extraInterfaces(IMethod.class));
        IMethod method = (IMethod) javaElement;
        IAnnotation annotation = createAnnotation("org.jbehave.core.annotations." + type);
        IAnnotation[] annotations = deprecated
                ? new IAnnotation[] { annotation, createAnnotation(Deprecated.class.getCanonicalName()) }
                : new IAnnotation[] { annotation };
        IMemberValuePair pair = mock(IMemberValuePair.class);
        ISourceRange sourceRange = mock(ISourceRange.class);

        when(javaElement.getElementType()).thenReturn(IJavaElement.METHOD);
        when(method.getAnnotations()).thenReturn(annotations);
        when(annotation.getMemberValuePairs()).thenReturn(new IMemberValuePair[] { pair });
        when(pair.getMemberName()).thenReturn("value");
        when(pair.getValue()).thenReturn(naming);
        when(method.getJavadocRange()).thenReturn(sourceRange);
        when(sourceRange.getOffset()).thenReturn(from);
        when(sourceRange.getLength()).thenReturn(to);
        when(classBufer.getText(from, to)).thenReturn(String.format("Javadoc for %s %s", type, naming));

        return javaElement;
    }

    private IAnnotation createAnnotation(String name)
    {
        IAnnotation annotation = mock(IAnnotation.class);
        when(annotation.getElementName()).thenReturn(name);
        return annotation;
    }

    private static class ClassFileMock
    {
        private final IPackageFragment classFile;
        private final IBuffer classFileBuffer;

        public ClassFileMock(IPackageFragment classFile, IBuffer classFileBuffer)
        {
            this.classFile = classFile;
            this.classFileBuffer = classFileBuffer;
        }

        public IPackageFragment getClassFile()
        {
            return classFile;
        }

        public IBuffer getClassFileBuffer()
        {
            return classFileBuffer;
        }
    }
}
