package com.team35.freelance.contract;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Ensures {@code logback-spring.xml} is well-formed XML (e.g. {@code &amp;} in {@code springProfile} names).
 * Does not require Docker or a running Spring context.
 */
class LogbackSpringXmlWellFormedTest {

    @Test
    void logbackSpringXmlParses() throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream in = getClass().getResourceAsStream("/logback-spring.xml")) {
            if (in == null) {
                throw new IllegalStateException("classpath:/logback-spring.xml not found");
            }
            factory.newDocumentBuilder().parse(in);
        }
    }
}
