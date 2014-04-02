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
import java.util.*;

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
        result.addAll(identify(document.getRootElement()));
        result.addAll(extractFeatures(document.getRootElement())) ;
        return result;
    }

    public  List<MetadataRecord> read(Document document){
        List<MetadataRecord> result= new ArrayList<MetadataRecord>();
        result.addAll(identify(document.getRootElement()));
        result.addAll(extractFeatures(document.getRootElement())) ;
        return result;
    }


    private List<MetadataRecord> extractFeatures(Element rootElement) {
        List<MetadataRecord> result= new ArrayList<MetadataRecord>();

        result.addAll(extractFeaturesFrom(rootElement.element("filestatus")));
        result.addAll(extractFeaturesFrom(rootElement.element("fileinfo")));

        Element metadata = rootElement.element("metadata");
        Iterator iterator = metadata.elementIterator();
        while (iterator.hasNext()){
            result.addAll(extractFeaturesFrom((Element) iterator.next()));
        }
        return result;
    }

    private List<MetadataRecord> extractFeaturesFrom(Element element){
        if (element==null)
            return null;
        List<MetadataRecord> result=new ArrayList<MetadataRecord>();
        Iterator iterator = element.elementIterator();
        while (iterator.hasNext()){
            result.add(extractFeatureCommon((Element) iterator.next()));
        }
        return result;
    }

    private MetadataRecord extractFeatureCommon(Element element) {
        return new MetadataRecord(adaptor.getProperty(element.getName()),
                element.getStringValue(), getStatus(element), getSources(element)) ;
    }

    public List<MetadataRecord> identify(Element rootElement) {
        List<MetadataRecord> result= new ArrayList<MetadataRecord>();
        Element identification = rootElement.element("identification");
        String status = getStatus(identification);
        Iterator identityIterator = identification.elementIterator("identity");

        while (identityIterator.hasNext()){
            Element identity = (Element) identityIterator.next();

            List<MetadataRecord> versions = getVersions(identity);
            List<MetadataRecord> formatMimetypes = getFormatMimetypes(identity, status);
            MetadataRecord puid = getPuid(identity);
            for ( MetadataRecord v: versions){
                List<MetadataRecord> identityRecord=new ArrayList<MetadataRecord>();
                identityRecord.addAll(formatMimetypes);
                identityRecord.add(v);
                identityRecord.add(puid);
                result.add(new MetadataRecord(adaptor.getProperty("identity"),identityRecord,status));
            }
        }
        return result;
    }

    private List<MetadataRecord> getFormatMimetypes(Element identity, String status) {
        List<MetadataRecord> result=new ArrayList<MetadataRecord>();
        result.add(new MetadataRecord(adaptor.getProperty("format"),
                identity.attribute("format").getValue(), status, getSources(identity)));
        result.add(new MetadataRecord(adaptor.getProperty("mimetype"),
                identity.attribute("mimetype").getValue(), status, getSources(identity)));
        return result;
    }

    private MetadataRecord getPuid(Element identity) {
        Element externalIdentifier = identity.element("externalIdentifier");
        String status = getStatus(externalIdentifier);
        return new MetadataRecord(adaptor.getProperty("puid"),
                externalIdentifier.getStringValue(), status, getSources(externalIdentifier));

    }
    private MetadataRecord getFeature(Element element, String feature) {
        return new MetadataRecord(adaptor.getProperty(feature),
                element.getStringValue(), getStatus(element), getSources(element));

    }

    private List<MetadataRecord> getVersions(Element identity) {
        List<MetadataRecord> result=new ArrayList<MetadataRecord>();
        Iterator versionIterator = identity.elementIterator("version");
        while (versionIterator.hasNext()){
            Element version = (Element) versionIterator.next();
            String status = getStatus(version);
            result.add(new MetadataRecord(adaptor.getProperty("format_version"),
                    version.getStringValue(), status, getSources(version)));
        }
        return result;
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
    private List<String> getSources(Element element){
        List<String> result=new ArrayList<String>();
        if (element.getName().equals("identity")){
            Iterator sourceIterator = element.elementIterator("tool");
            while (sourceIterator.hasNext()){
                Element source = (Element) sourceIterator.next();
                String toolname= source.attribute("toolname").getValue();
                String toolversion= source.attribute("toolversion").getValue();
                result.add( adaptor.getSource( toolname, toolversion ).getId())   ;
            }
        } else {
            String toolname= element.attribute("toolname").getValue();
            String toolversion= element.attribute("toolversion").getValue();
            result.add(adaptor.getSource( toolname, toolversion ).getId());
        }
        return result;
    }



}