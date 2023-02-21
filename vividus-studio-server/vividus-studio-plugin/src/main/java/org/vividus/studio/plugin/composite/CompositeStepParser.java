package org.vividus.studio.plugin.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CompositeStepParser
{
    private static final Pattern COMPOSITE_STEP_TOKENIZER = Pattern
            .compile("^(?:Composite:(.+?)$)(.*?)(?=^Composite:|\\Z)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final int COMPOSITE_NAME = 1;
    private static final int COMPOSITE_BODY = 2;

    private CompositeStepParser()
    {
    }

    public static Stream<CompositeStep> parse(String content)
    {
        List<CompositeStep> compositeSteps = new ArrayList<>();

        Matcher compositeStepMatcher = COMPOSITE_STEP_TOKENIZER.matcher(content);
        while (compositeStepMatcher.find())
        {
            String name = compositeStepMatcher.group(COMPOSITE_NAME).strip();
            String body = compositeStepMatcher.group(COMPOSITE_BODY).strip();

            compositeSteps.add(new CompositeStep(name, body));
        }

        return compositeSteps.stream();
    }
}
