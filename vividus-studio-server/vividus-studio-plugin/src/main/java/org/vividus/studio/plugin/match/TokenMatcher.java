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

package org.vividus.studio.plugin.match;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableInt;

public final class TokenMatcher
{
    private TokenMatcher()
    {
    }

    public static MatchOutcome match(String input, List<String> tokenStrings)
    {
        String headToken = tokenStrings.get(0);

        if (input.length() <= headToken.length())
        {
            return headToken.startsWith(input) ? MatchOutcome.passed(0, headToken, List.of())
                    : MatchOutcome.failed();
        }

        if (!input.startsWith(headToken))
        {
            return MatchOutcome.failed();
        }

        List<Token> tokens = tokenStrings.stream()
                                         .map(Token::new)
                                         .collect(Collectors.collectingAndThen(Collectors.toList(), list ->
                                         {
                                             list.get(list.size() - 1).setLast(true);
                                             return list;
                                         }));

        int headTokenLength = headToken.length();

        List<Integer> indices = new ArrayList<>();
        indices.add(headTokenLength);

        MutableInt position = new MutableInt(headTokenLength);

        for (int index = 1; index < tokens.size(); index++)
        {
            Token token = tokens.get(index);
            Outcome outcome = matchToken(token, input, position, indices);

            while (outcome == Outcome.PROCEED_NEXT_CHAR)
            {
                position.increment();
                outcome = matchToken(token, input, position, indices);
            }

            if (outcome == Outcome.COMPLETED)
            {
                String subToken = token.getToken().substring(0, position.getValue());
                return MatchOutcome.passed(index, subToken, indices);
            }

            if (outcome == Outcome.FAILED)
            {
                return MatchOutcome.failed();
            }
        }

        return MatchOutcome.passed(tokens.size() - 1, "", indices);
    }

    private static Outcome matchToken(Token token, String input, MutableInt position, List<Integer> indices)
    {
        String tokenAsString = token.getToken();
        int currentPos = position.intValue();

        char headChar = tokenAsString.charAt(0);
        int matchIndex = input.indexOf(headChar, currentPos);
        if (matchIndex == -1)
        {
            return Outcome.FAILED;
        }

        currentPos = matchIndex;

        int inputLength = input.length();
        int completedPosition = inputLength - 1;
        for (int tokenPos = 1; tokenPos < tokenAsString.length(); tokenPos++)
        {
            int nextPos = currentPos + tokenPos;
            if (nextPos > completedPosition)
            {
                indices.add(currentPos);
                position.setValue(tokenPos);
                return Outcome.COMPLETED;
            }

            char tokenChar = tokenAsString.charAt(tokenPos);
            char inputChar = input.charAt(currentPos + tokenPos);

            if (tokenChar != inputChar)
            {
                return Outcome.PROCEED_NEXT_CHAR;
            }
        }

        int nextPosition = currentPos + tokenAsString.length();
        position.setValue(nextPosition);

        if (token.isLast() && position.intValue() < inputLength)
        {
            return Outcome.FAILED;
        }

        indices.add(currentPos);
        if (inputLength != nextPosition)
        {
            indices.add(nextPosition);
        }
        return Outcome.PROCEED_NEXT_TOKEN;
    }

    public static final class MatchOutcome
    {
        private final boolean match;
        private final int tokenIndex;
        private final String subToken;
        private final List<Integer> argIndices;

        private MatchOutcome(boolean match, int tokenIndex, String subToken, List<Integer> argIndices)
        {
            this.match = match;
            this.tokenIndex = tokenIndex;
            this.subToken = subToken;
            this.argIndices = argIndices;
        }

        public boolean isMatch()
        {
            return match;
        }

        public int getTokenIndex()
        {
            return tokenIndex;
        }

        public String getSubToken()
        {
            return subToken;
        }

        public List<Integer> getArgIndices()
        {
            return argIndices;
        }

        private static MatchOutcome failed()
        {
            return new MatchOutcome(false, -1, null, List.of());
        }

        private static MatchOutcome passed(int tokenIndex, String subToken, List<Integer> matchIndices)
        {
            return new MatchOutcome(true, tokenIndex, subToken, matchIndices);
        }
    }

    private static final class Token
    {
        private final String token;
        private boolean last;

        private Token(String token)
        {
            this.token = token;
        }

        private boolean isLast()
        {
            return last;
        }

        private void setLast(boolean last)
        {
            this.last = last;
        }

        private String getToken()
        {
            return token;
        }
    }

    private enum Outcome
    {
        COMPLETED,
        PROCEED_NEXT_TOKEN,
        FAILED,
        PROCEED_NEXT_CHAR;
    }
}
