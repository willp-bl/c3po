package com.petpet.collpro;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Query;

import com.petpet.collpro.common.Constants;
import com.petpet.collpro.db.DBManager;
import com.petpet.collpro.tools.FITSMetaDataConverter;
import com.petpet.collpro.tools.SimpleGatherer;
import com.petpet.collpro.utils.Configurator;

/**
 * Hello world!
 * 
 */
public class App {
    public static void main(String[] args) {
        Configurator.getInstance().configure();
        foldertest();
        querytest();
    }
    
    private static void foldertest() {
        SimpleGatherer g = new SimpleGatherer(new FITSMetaDataConverter());
        g.gather(new File("/home/peter/Desktop/output/"));
    }
    
    private static void querytest() {
        Query query = DBManager.getInstance().getEntityManager().createNamedQuery(
            Constants.ELEMENTS_WITH_PROPERTY_AND_VALUE_COUNT_QUERY).setParameter("pname", "mimetype").setParameter(
            "value", "application/pdf");
        Long count = (Long) query.getSingleResult();
        System.out.println("PDFs: " + count);
        
        query = DBManager.getInstance().getEntityManager().createNamedQuery(
            Constants.DISTINCT_PROPERTY_VALUE_COUNT_QUERY).setParameter("pname", "mimetype");
        count = (Long) query.getSingleResult();
        System.out.println("Distinct mimetypes: " + count);
        
        query = DBManager.getInstance().getEntityManager().createNamedQuery(
            Constants.DISTINCT_PROPERTY_VALUES_SET_QUERY).setParameter("pname", "mimetype");
        List<String> list = (List<String>) query.getResultList();
        for (String v : list) {
            System.out.println("mimetype: " + v);
        }
        
        query = DBManager.getInstance().getEntityManager().createNamedQuery(Constants.MOST_OCCURRING_PROPERTIES);
        List res = query.setMaxResults(5).getResultList();
        System.out.println("GET MOST OCCURRING PROPERTIES");
        for (Object o : res) {
            Object[] p = (Object[]) o;
            System.out.println(Arrays.deepToString(p));
        }
        
        query = DBManager.getInstance().getEntityManager().createNamedQuery(Constants.SUM_VALUES_FOR_PROPERTY).setParameter("pname", "size");
        Long sum = (Long) query.getSingleResult();
        System.out.println("All elements size " + sum);
        
        query = DBManager.getInstance().getEntityManager().createNamedQuery(Constants.AVG_VALUES_FOR_PROPERTY).setParameter("pname", "size");
        Double avg = (Double) query.getSingleResult();
        System.out.println("AVG elements size " + avg);
    }
}
