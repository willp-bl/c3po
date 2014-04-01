package com.petpet.c3po.adaptor.fits;
import com.petpet.c3po.api.model.helper.MetadataRecord;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultAttribute;
import org.dom4j.tree.DefaultDocument;
import org.dom4j.tree.DefaultElement;

import java.io.StringReader;
import java.util.List;

/**
 * Created by artur on 4/1/14.
 */
public class DOM4Reader {
    static final SAXReader reader=new SAXReader();
    public static List<MetadataRecord> read(String data){
        List<MetadataRecord> result=null;
        Document document=null;
        try {
            document = reader.read(new StringReader(data));
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        if (document==null)
        {
            return null;
        }

        identify(document.getRootElement());


        return result;
    }

    private static void identify(Element rootElement) {
        Element identification = rootElement.element("identification");
        String status = getStatus(identification);
        List identity = identification.elements("identity");

    }

    private static String getStatus(Element element){
        String value=null;
        for ( int i = 0, size = element.attributeCount(); i < size; i++ ) {
            Attribute attribute = element.attribute("status");
            if (attribute != null)  {
                value= attribute.getValue();
            }
        }
        return (value == null) ? "OK" : value;
    }


}
