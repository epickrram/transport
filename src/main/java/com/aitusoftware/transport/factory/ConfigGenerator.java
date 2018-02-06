/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.transport.factory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class ConfigGenerator
{
    public void generateConfigurationProperties(
            final Collection<Class<?>> topicDefinitions, final Path destination)
    {
        final List<Class<?>> orderedTopics = new ArrayList<>(topicDefinitions);
        orderedTopics.sort(Comparator.comparing(Class::getName));
        try (final BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
        {
            writer.append("Generated at ").append(Instant.now().toString());
            writer.newLine();
            for (Class<?> topic : orderedTopics)
            {
                writer.append("transport.publisher.idler.");
                writer.append(topic.getName()).append("=").
                        append("STATIC,1,MILLISECONDS");
                writer.newLine();
            }

            for (Class<?> topic : orderedTopics)
            {
                writer.append("transport.subscriber.idler.");
                writer.append(topic.getName()).append("=").
                        append("STATIC,1,MILLISECONDS");
                writer.newLine();
            }

            for (Class<?> topic : orderedTopics)
            {
                writer.append("transport.publisher.affinity.");
                writer.append(topic.getName()).append("=").
                        append("-1");
                writer.newLine();
            }

            for (Class<?> topic : orderedTopics)
            {
                writer.append("transport.subscriber.affinity.");
                writer.append(topic.getName()).append("=").
                        append("-1");
                writer.newLine();
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}