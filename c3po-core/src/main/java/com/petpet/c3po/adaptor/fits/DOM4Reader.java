package com.petpet.c3po.adaptor.fits;
import com.petpet.c3po.api.model.Source;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by artur on 4/1/14.
 */
public class DOM4Reader {
    final SAXReader reader=new SAXReader();
    private FITSAdaptor adaptor;
    public DOM4Reader(FITSAdaptor adaptor)    {
        this.adaptor=adaptor;
    }
    public  List<MetadataRecord> read(String data){
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

    public void identify(Element rootElement) {
        Element identification = rootElement.element("identification");
        String status = getStatus(identification);
        Iterator identityIterator = identification.elementIterator("identity");
        while (identityIterator.hasNext()){
            Element identity = (Element) identityIterator.next();

            HashMap<List<String>, List<Source>> formatMimetypes = getFormatMimetypes(identity);
            HashMap<String, List<Source>> versions = getVersions(identity);
            HashMap<String, List<Source>> puid = getPuid(identity);
        }
    }

    private HashMap<List<String>, List<Source>> getFormatMimetypes(Element identity) {
        HashMap<List<String>, List<Source>> formatMimetypes=new HashMap<List<String>, List<Source>>();
        List<String>  formatMimetype= new ArrayList<String>();
        formatMimetype.add(identity.attribute("format").getValue());
        formatMimetype.add(identity.attribute("mimetype").getValue());
        List<Source> sourcesIdentity=getSources(identity);
        formatMimetypes.put(formatMimetype,sourcesIdentity);
        return formatMimetypes;
    }

    private HashMap<String, List<Source>> getPuid(Element identity) {
        Element externalIdentifier = identity.element("externalIdentifier");
        HashMap<String, List<Source>> puid=new HashMap<String, List<Source>>();
        puid.put(externalIdentifier.getStringValue(),getSources(externalIdentifier));
        return puid;
    }

    private HashMap<String, List<Source>> getVersions(Element identity) {
        HashMap<String, List<Source>> versions=new HashMap<String, List<Source>>();
        Iterator versionIterator = identity.elementIterator("version");
        while (versionIterator.hasNext()){
            Element version = (Element) versionIterator.next();
            List<Source> sourcesVersion=new ArrayList<Source>();
            sourcesVersion.addAll(getSources(version));
            versions.put(version.getStringValue(),sourcesVersion);
        }
        return versions;
    }

    private String getStatus(Element element){
        String value=null;
        for ( int i = 0, size = element.attributeCount(); i < size; i++ ) {
            Attribute attribute = element.attribute("status");
            if (attribute != null)  {
                value= attribute.getValue();
            }
        }
        return (value == null) ? "OK" : value;
    }
    private List<Source> getSources(Element element){
        List<Source> result=new ArrayList<Source>();
        if (element.getName().equals("identity")){
            Iterator sourceIterator = element.elementIterator("tool");
            while (sourceIterator.hasNext()){
                Element source = (Element) sourceIterator.next();
                String toolname= source.attribute("toolname").getValue();
                String toolversion= source.attribute("toolversion").getValue();
                result.add( adaptor.getSource( toolname, toolversion ))   ;
            }
        } else {
            String toolname= element.attribute("toolname").getValue();
            String toolversion= element.attribute("toolversion").getValue();
            result.add(adaptor.getSource( toolname, toolversion ));
        }
        return result;
    }


}