package com.aitusoftware.transport.messaging.proxy;

import org.junit.Test;

import java.io.StringWriter;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PublisherGeneratorTest
{
    private static final String EXPECTED_CLASS_DEFINITION = "" +
            "package com.package;\n" +
            "\n" +
            "import com.aitusoftware.transport.messaging.proxy.AbstractPublisher;\n" +
            "import com.aitusoftware.transport.buffer.WritableRecord;\n" +
            "import com.aitusoftware.transport.messaging.proxy.Encoder;\n" +
            "\n" +
            "\n" +
            "public class TestPublisher extends AbstractPublisher {\n" +
            "\n" +
            "\tpublic void say(\n" +
            "\t\tfinal java.lang.CharSequence word, final int count) {\n" +
            "\t\t\n" +
            "\t\tfinal int recordLength = word.length() +  4;\n" +
            "\t\tfinal WritableRecord wr = acquireRecord(recordLength);\n" +
            "\t\tEncoder.encodeCharSequence(wr.buffer(), word);\n" +
            "\t\tEncoder.encodeInt(wr.buffer(), count);\n" +
            "\t\twr.commit();\n" +
            "\t}\n" +
            "\n" +
            "}\n";
    private final PublisherGenerator generator = new PublisherGenerator();

    @Test
    public void shouldGeneratePublisherImplementation() throws Exception
    {
        final StringWriter writer = new StringWriter();
        generator.generatePublisher(
                "com.package", "TestPublisher",
                new MethodDescriptor[]{
                        new MethodDescriptor(0, "say",
                                new ParameterDescriptor[]{
                                    new ParameterDescriptor("word", CharSequence.class),
                                    new ParameterDescriptor("count", int.class)
                        })},
                Collections.emptyList(),
                writer);

        assertThat(writer.toString(), is(EXPECTED_CLASS_DEFINITION));
    }
}