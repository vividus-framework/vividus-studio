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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.studio.plugin.exception.VividusStudioException;
import org.vividus.studio.plugin.model.Parameter;
import org.vividus.studio.plugin.model.StepDefinition;
import org.vividus.studio.plugin.util.RuntimeWrapper;

@Singleton
@SuppressWarnings("restriction")
public class StepDefinitionFinder implements IStepDefinitionFinder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StepDefinitionFinder.class);
    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("\\$\\w+");
    private static final Pattern STEP_ANNOTATION_PATTERN = Pattern
            .compile("^org\\.jbehave\\.core\\.annotations\\.(When|Then|Given)$");
    private static final Pattern COMPOSITE_STEP_TOKENIZER = Pattern
            .compile("^(?:Composite:(.+?)$)(.*?)(?=^Composite:|\\Z)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final int COMPOSITE_NAME = 1;
    private static final int COMPOSITE_BODY = 2;

    @Override
    public Collection<StepDefinition> find(IJavaProject root)
    {
        LOGGER.info("Scanning project {}", root.getProject().getName());
        List<IPackageFragment> fragments = children(root).parallel()
                                                        .filter(JarPackageFragmentRoot.class::isInstance)
                                                        .map(JarPackageFragmentRoot.class::cast)
                                                        .filter(StepDefinitionFinder::isStepDefinitionScanCandidate)
                                                        .flatMap(StepDefinitionFinder::children)
                                                        .filter(IPackageFragment.class::isInstance)
                                                        .map(IPackageFragment.class::cast)
                                                        .collect(Collectors.toList());

        List<StepDefinition> javaStepDefinitions = findJavaSteps(fragments);
        LOGGER.info("Found {} java steps", javaStepDefinitions.size());

        List<StepDefinition> compositeStepDefinitions = findCompositeSteps(fragments);
        LOGGER.info("Found {} composite steps", compositeStepDefinitions.size());

        List<StepDefinition> stepDefinitions = new ArrayList<>();
        stepDefinitions.addAll(javaStepDefinitions);
        stepDefinitions.addAll(compositeStepDefinitions);

        return stepDefinitions;
    }

    private List<StepDefinition> findCompositeSteps(List<IPackageFragment> fragments)
    {
        return fragments.parallelStream()
                        .map(pf -> RuntimeWrapper.wrapMono(pf::getNonJavaResources,
                                error("non java resources", pf.getElementName())))
                        .flatMap(Arrays::stream)
                        .filter(IJarEntryResource.class::isInstance)
                        .map(IJarEntryResource.class::cast)
                        .filter(e -> e.getName().endsWith(".steps"))
                        .map(r ->
                        {
                            IPackageFragment parent = (IPackageFragment) r.getParent();
                            String module = parent.getParent().getElementName();
                            try (InputStream inputStream = RuntimeWrapper.wrapMono(r::getContents,
                                    error("content", "resource")))
                            {
                                String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                                Matcher compositeStepMatcher = COMPOSITE_STEP_TOKENIZER.matcher(content);
                                List<StepDefinition> definitions = new ArrayList<>();
                                while (compositeStepMatcher.find())
                                {
                                    String stepAsString = compositeStepMatcher.group(COMPOSITE_NAME).strip();
                                    String documentation = compositeStepMatcher.group(COMPOSITE_BODY).strip();
                                    definitions.add(createStepDefinition(module, stepAsString, documentation));
                                }
                                return definitions;
                            }
                            catch (IOException e)
                            {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
    }

    private List<StepDefinition> findJavaSteps(List<IPackageFragment> fragments)
    {
        return fragments.parallelStream()
                        .flatMap(StepDefinitionFinder::children)
                        .filter(e -> IJavaElement.CLASS_FILE == e.getElementType())
                        .filter(e -> StringUtils.contains(e.getElementName(), "Steps"))
                        .map(IClassFile.class::cast)
                        .flatMap(this::findStepDefinitions)
                        .collect(Collectors.toList());
    }

    private Stream<StepDefinition> findStepDefinitions(IClassFile classFile)
    {
        String module = classFile.getParent().getParent().getElementName();
        IOpenable openable = classFile.getOpenable();
        IBuffer buffer = RuntimeWrapper.wrapMono(openable::getBuffer, error("buffer", classFile.getElementName()));

        return children(classFile)
            .map(IMember.class::cast)
            .flatMap(StepDefinitionFinder::children)
            .filter(m -> m.getElementType() == IJavaElement.METHOD)
            .map(IMethod.class::cast)
            .map(m -> getStepDefinition(m, buffer, module))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private Optional<StepDefinition> getStepDefinition(IMethod method, IBuffer buffer, String module)
    {
        List<IAnnotation> annotations = RuntimeWrapper
                .wrapStream(method::getAnnotations, error("annotations", method.getElementName()))
                .collect(Collectors.toList());
        return getStepAsString(annotations)
            .map(stepAsString ->
            {
                ISourceRange range = RuntimeWrapper.wrapMono(method::getJavadocRange,
                        error("javadoc range", method.getElementName()));
                String documentation = range != null ? buffer.getText(range.getOffset(), range.getLength())
                        : "No documentation available";
                StepDefinition definition = createStepDefinition(module, stepAsString, documentation);
                definition.setDeprecated(isDeprecated(annotations));
                return definition;
            });
    }

    private boolean isDeprecated(List<IAnnotation> annotations)
    {
        String deprecated = Deprecated.class.getCanonicalName();
        return annotations.stream()
                          .map(IAnnotation::getElementName)
                          .anyMatch(deprecated::equals);
    }

    private static List<IMemberValuePair> getAnnotationPairs(IAnnotation annotation)
    {
        return RuntimeWrapper.wrapStream(annotation::getMemberValuePairs, error("pairs", "annotation"))
                .collect(Collectors.toList());
    }

    private static Optional<String> getStepAsString(List<IAnnotation> annotations)
    {
        return annotations.stream()
                         .filter(a -> STEP_ANNOTATION_PATTERN.matcher(a.getElementName()).matches())
                         .map(a ->
                         {
                             String elementName = a.getElementName();
                             String key = elementName.substring(elementName.lastIndexOf('.') + 1);
                             String value = getAnnotationPairs(a).stream()
                                                                 .filter(m -> m.getMemberName().equals("value"))
                                                                 .map(IMemberValuePair::getValue)
                                                                 .findFirst()
                                                                 .map(String.class::cast)
                                                                 .get();
                             return key + " " + value;
                         })
                         .findFirst();
    }

    private static boolean isStepDefinitionScanCandidate(JarPackageFragmentRoot jar)
    {
        return Optional.ofNullable(jar.getManifest())
                .map(Manifest::getMainAttributes)
                .map(attrs -> attrs.getValue("Automatic-Module-Name"))
                .filter(amn -> amn.startsWith("org.vividus"))
                .isPresent();
    }

    private StepDefinition createStepDefinition(String module, String stepAsString, String documentation)
    {
        List<Parameter> parameters = new ArrayList<>();
        Matcher parameterMatcher = PARAMETER_NAME_PATTERN.matcher(stepAsString);

        List<Integer> tokenIndices = new ArrayList<>();
        tokenIndices.add(0);

        int matchIndex = 1;
        while (parameterMatcher.find())
        {
            int start = parameterMatcher.start();
            String group = parameterMatcher.group();

            Parameter parameter = new Parameter(matchIndex++, group, start);
            parameters.add(parameter);

            tokenIndices.add(start);
            tokenIndices.add(start + group.length());
        }

        tokenIndices.add(stepAsString.length());

        List<String> matchTokens = new ArrayList<>(tokenIndices.size() / 2);
        for (int index = 0; index < tokenIndices.size(); index += 2)
        {
            String token = stepAsString.substring(tokenIndices.get(index), tokenIndices.get(index + 1));
            matchTokens.add(token);
        }

        return new StepDefinition(module, stepAsString, documentation, parameters, matchTokens);
    }

    @SuppressWarnings("PreferMethodReference")
    private static <T extends IJavaElement & IParent> Stream<? extends IJavaElement> children(T element)
    {
        return RuntimeWrapper.wrapStream(() -> element.getChildren(), error("children", element.getElementName()));
    }

    private static String getElementErrorMessage(String target, String parent)
    {
        return String.format("Exception occured while getting '%s' of the '%s' element", target, parent);
    }

    private static Function<Exception, ? extends RuntimeException> error(String target, String parent)
    {
        return e -> new VividusStudioException(getElementErrorMessage(target, parent), e);
    }
}
