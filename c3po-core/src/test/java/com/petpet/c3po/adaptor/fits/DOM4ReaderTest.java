package com.petpet.c3po.adaptor.fits;

import com.petpet.c3po.utils.Configurator;
import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;

/**
 * Created by artur on 4/2/14.
 */
public class DOM4ReaderTest extends TestCase {
    public void testIdentify() throws Exception {

        Configurator configurator = Configurator.getDefaultConfigurator();
        configurator.configure();

        FITSAdaptor fitsAdaptor=new FITSAdaptor();
        fitsAdaptor.setCache(configurator.getPersistence().getCache());

        InputStream resourceAsStream = DOM4Reader.class.getClassLoader().getResourceAsStream("fits.xml");
        SAXReader reader=new SAXReader();
        Document doc =reader.read(resourceAsStream);
        DOM4Reader dom4Reader=new DOM4Reader(fitsAdaptor);
        Element root=doc.getRootElement();
        dom4Reader.identify(root);
    }

    public void testRead() throws Exception {
        Configurator configurator = Configurator.getDefaultConfigurator();
        configurator.configure();

        FITSAdaptor fitsAdaptor=new FITSAdaptor();
        fitsAdaptor.setCache(configurator.getPersistence().getCache());

        InputStream resourceAsStream = DOM4Reader.class.getClassLoader().getResourceAsStream("fits.xml");
        SAXReader reader=new SAXReader();
        Document doc =reader.read(resourceAsStream);
        DOM4Reader dom4Reader=new DOM4Reader(fitsAdaptor);
        dom4Reader.read(doc);
    }
}
