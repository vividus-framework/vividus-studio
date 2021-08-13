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

package org.vividus.studio.plugin.util;

import java.util.function.Function;
import java.util.stream.Stream;

public final class RuntimeWrapper
{
    private RuntimeWrapper()
    {
    }

    public static <T, E extends Exception> void wrap(ExceptionRunnable<E> runnable,
            Function<Exception, ? extends RuntimeException> factory)
    {
        wrapMono(() ->
        {
            runnable.run();
            return null;
        }, factory);
    }

    @SuppressWarnings("IllegalCatchExtended")
    public static <T, E extends Exception> T wrapMono(ExceptionSupplier<E, T> supplier,
            Function<Exception, ? extends RuntimeException> factory)
    {
        try
        {
            return supplier.get();
        }
        catch (Exception e)
        {
            throw factory.apply(e);
        }
    }

    public static <T, E extends Exception> Stream<T> wrapStream(ExceptionSupplier<E, T[]> supplier,
            Function<Exception, ? extends RuntimeException> factory)
    {
        return Stream.of(wrapMono(supplier, factory));
    }

    public interface ExceptionSupplier<E extends Exception, R>
    {
        R get() throws E;
    }

    public interface ExceptionRunnable<E extends Exception>
    {
        void run() throws E;
    }
}
