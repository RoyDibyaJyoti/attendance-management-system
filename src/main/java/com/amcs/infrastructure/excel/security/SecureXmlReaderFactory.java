package com.amcs.infrastructure.excel.security;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Factory creating hardened SAX {@link XMLReader} instances protected against XML External Entity (XXE)
 * injection, Billion Laughs attacks, and external schema retrieval.
 */
public final class SecureXmlReaderFactory {

    private SecureXmlReaderFactory() {}

    /**
     * Creates a hardened SAX XMLReader with all external entity, DTD, and DOCTYPE resolution disabled.
     */
    public static XMLReader createSecureXmlReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);

        // Disallow DOCTYPE declaration entirely to prevent XXE and parameter entity attacks
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        // Disable external general and parameter entities
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        // Disable external DTD loading
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // Enable secure processing (limits entity expansions and entity count)
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        return spf.newSAXParser().getXMLReader();
    }
}
