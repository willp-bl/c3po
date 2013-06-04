package com.petpet.c3po.analysis.mapreduce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.datamodel.Property;
import com.petpet.c3po.datamodel.Property.PropertyType;

public class BubbleChartJob extends MapReduceJob {

  private static final Logger LOG = LoggerFactory.getLogger(BubbleChartJob.class);

  private String property1;
  private String property2;

  public BubbleChartJob(String c, String property1, String property2) {
	  this(c, property1, property2, new BasicDBObject("collection", c));
  }

  public BubbleChartJob(String c, String property1, String property2, 
		  BasicDBObject query) {
    this.setC3poCollection(c);
    this.property1 = property1;
    this.property2 = property2;
    this.setFilterquery(query);
    this.setConfig(new HashMap<String, String>());
  }

  public MapReduceOutput execute() {
    final Property p1 = this.getPersistence().getCache().getProperty(property1);
    final Property p2 = this.getPersistence().getCache().getProperty(property2);
    
    String map = Constants.BUBBLECHART_MAP;
    
    map = map.replace("{1}", p1.getId());
    map = map.replace("{2}", p2.getId());
    
    map = map.replace("{value1convert}", getMapConvertFunction(p1));
    map = map.replace("{value2convert}", getMapConvertFunction(p2));

    LOG.debug("Executing histogram map reduce job with following map:\n{}", map);
    
    final DBCollection elements = this.getPersistence().getDB().getCollection(Constants.TBL_ELEMENTS);
    final MapReduceCommand cmd = new MapReduceCommand(elements, map, Constants.BUBBLECHART_REDUCE,
        this.getOutputCollection(), this.getType(), this.getFilterquery());
    
    cmd.setFinalize(Constants.BUBBLECHART_FINALIZE);

    return this.getPersistence().mapreduce(Constants.TBL_ELEMENTS, cmd);
  }
  
  
  private String getMapConvertFunction(Property p) {
	  String res = "";
	  
	  if (p.getType().equals(PropertyType.DATE.toString())) {
		  /* 
		   * add "value is DATE type" constraint
		   */
		  String constraintKey = "metadata." + p.getId() + ".value";
	      BasicDBObject constraintValue = new BasicDBObject("$type", 9);
	      
	      BasicDBObject prop = (BasicDBObject) this.getFilterquery()
	    		  .remove("metadata." + p.getId() + ".value");
	      
	      if (prop != null) {
	        LOG.info("Old Date Property: " + prop.toString());
	        List<BasicDBObject> and = new ArrayList<BasicDBObject>();
	        and.add(new BasicDBObject("metadata." + p.getId() + ".value", prop));
	        and.add(new BasicDBObject(constraintKey, constraintValue));
	        this.getFilterquery().put("$and", and);// for date...
	      } else {
	        this.getFilterquery().append(constraintKey, constraintValue);
	      }
	      LOG.debug("Date Filter Query adjusted: " + this.getFilterquery().toString());
	      
	      res = Constants.BUBBLECHART_MAP_CONVERT_DATE;
	      
	  } else if (p.getType().equals(PropertyType.INTEGER.toString()) 
	    		   || p.getType().equals(PropertyType.FLOAT.toString())) {
		  

		  String width = this.getConfig().get("bin_width_" + p.getId());
		  if (width == null)
			  width = this.getConfig().get("bin_width");
		  
		  if (width == null) {
			  String val = (String) this.getFilterquery()
	    				.get("metadata." + p.getId() + ".value");
			  width = inferBinWidth(val) + "";
		  }
		  
		  res = Constants.BUBBLECHART_MAP_CONVERT_NUMERIC
				  .replace("{2}", width);
	  } else {
		  res = Constants.BUBBLECHART_MAP_CONVERT_STRING;
	  }
	  
	  res = res.replace("{1}", p.getId());
	  
	  return res;
  }
  
  @Override
  public JobResult run() {
    return new JobResult(this.execute());
  }

  public static int inferBinWidth(String val) {
    String[] values = val.split(" - ");
    int low = Integer.parseInt(values[0]);
    int high = Integer.parseInt(values[1]);
    int width = high - low + 1; //because of gte/lte

    LOG.debug("inferred bin width is {}", width);

    return width;
  }
}
