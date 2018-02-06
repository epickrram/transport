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