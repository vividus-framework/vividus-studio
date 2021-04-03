package org.vividus.studio.plugin.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.vividus.studio.plugin.match.TokenMatcher.MatchOutcome;

class TokenMatcherTests
{
    private static final List<String> TOKENS = List.of(
        "When I capture HTTP ",
        " request with URL pattern `",
        "` and save URL query to ",
        " variable `",
        "`"
    );

    static Stream<Arguments> samples()
    {
        return Stream.of(
            arguments("When I capture",                                                                                                      true,  0,  "When I capture HTTP "),
            arguments("When I capture HTTP ",                                                                                                true,  0,  "When I capture HTTP "),
            arguments("When I capture HTTP GET request with URL",                                                                            true,  1,  " request with URL"),
            arguments("When I capture HTTP GET req request with URL",                                                                        true,  1,  " request with URL"),
            arguments("When I capture HTTP GET request with URL pattern `.*wiki.*` and save URL query to scenario variable `req`",           true,  4,  ""),
            arguments("When I capture WS traffic",                                                                                           false, -1, null),
            arguments("When I capture HTTP GET data",                                                                                        false, -1, null),
            arguments("Given I initialize data",                                                                                             false, -1, null),
            arguments("When I capture HTTP GET request with URL pattern `.*wiki.*` and save URL query to scenario variable `req` and print", false, -1, null)
        );
    }

    @ParameterizedTest
    @MethodSource("samples")
    void shouldMatch(String sample, boolean expected, int index, String token)
    {
        MatchOutcome outcome = TokenMatcher.match(sample, TOKENS);
        assertEquals(expected, outcome.isMatch());
        assertEquals(index, outcome.getTokenIndex());
        assertEquals(token, outcome.getSubToken());
    }
}
