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

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.integration.MarketNews;
import com.aitusoftware.transport.integration.OrderNotifications;
import com.aitusoftware.transport.integration.TradeNotifications;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigGeneratorTest
{
    private static final Collection<Class<?>> TOPICS = Arrays.asList(
            TradeNotifications.class,
            OrderNotifications.class,
            MarketNews.class
    );

    private final ConfigGenerator generator = new ConfigGenerator();

    @Test
    public void shouldGenerateConfig() throws IOException
    {
        final Path path = Fixtures.tempDirectory();
        Files.createDirectories(path);
        final Path file = path.resolve("configuration.properties");
        generator.generateConfigurationProperties(TOPICS, file);

        assertFileContentEqual(file, "expected-configuration.properties");
    }

    private void assertFileContentEqual(
            final Path generatedFile, final String expectedResourceName) throws IOException
    {
        final List<String> lines = new ArrayList<>(Files.readAllLines(generatedFile));
        // remove header
        lines.remove(0);
        final List<String> expectedLines = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(expectedResourceName))))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                expectedLines.add(line);
            }
        }

        assertThat(lines, is(expectedLines));
    }
}