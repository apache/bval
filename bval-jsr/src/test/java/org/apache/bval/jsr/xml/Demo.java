package org.apache.bval.jsr.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class Demo {

    @Test
    public void test1() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        // Set the parent XMLReader on the XMLFilter
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        // Set UnmarshallerHandler as ContentHandler on XMLFilter
        
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        
        UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();
        xr.setContentHandler(unmarshallerHandler);

        // Parse the XML
        InputSource xml = new InputSource(getClass().getResourceAsStream("/sample-validation2.xml"));
        xr.parse(xml);
        JAXBElement<ValidationConfigType> result = (JAXBElement<ValidationConfigType>) unmarshallerHandler.getResult();
        System.out.println(ToStringBuilder.reflectionToString(result.getValue()));
    }
}
