package controllers;

import helpers.BubbleGraph;
import helpers.Graph;
import helpers.PropertyValuesFilter;
import helpers.Statistics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.analysis.mapreduce.BubbleChartJob;
import com.petpet.c3po.analysis.mapreduce.HistogramJob;
import com.petpet.c3po.analysis.mapreduce.JobResult;
import com.petpet.c3po.analysis.mapreduce.MapReduceJob;
import com.petpet.c3po.analysis.mapreduce.NumericAggregationJob;
import com.petpet.c3po.api.dao.Cache;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.datamodel.Filter;
import com.petpet.c3po.datamodel.Property;
import com.petpet.c3po.datamodel.Property.PropertyType;
import com.petpet.c3po.utils.Configurator;
import com.petpet.c3po.utils.DataHelper;

public class FilterController extends Controller {

  private static final int DEFAULT_BIN_WIDTH = 50;	
	
  /**
   * Gets all selected filters and returns them to the client, so that it can
   * reconstruct the page.
   * 
   * @return
   */
  public static Result getAll() {
    Logger.debug("in method getAll(), retrieving all properties");
    List<PropertyValuesFilter> filters = new ArrayList<PropertyValuesFilter>();
    Filter filter = Application.getFilterFromSession();

    if (filter != null) {
      BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
      ref.put("collection", filter.getCollection());
      DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);

      while (cursor.hasNext()) {
        Filter tmp = DataHelper.parseFilter(cursor.next());
        if(tmp.getType() == null || tmp.getType().equals("null")) {
        	
	        if (tmp.getProperty() != null && tmp.getValue() != null) {
	          final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
	          final Property property = cache.getProperty(tmp.getProperty());
	          PropertyValuesFilter f = getValues(tmp.getCollection(), property, tmp.getValue());
	
	          f.setSelected(tmp.getValue());
	          f.setBubble("null");
	          filters.add(f);
	        }
        }
        else {
        	for(int i=0; i<2; i++) {
        		
        		String p = tmp.getBubbleProperty(i);
        		String v = tmp.getBubbleValue(i);
        		Logger.info("p=" + p + ". v=" + v);
        		if( (p != null) && (v != null) ) {
        			final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
        	          final Property property = cache.getProperty(p);
        	          PropertyValuesFilter f = getValues(tmp.getCollection(), property, v);
        	          f.setSelected(v);
        	          if(i==0) {
        	        	  f.setBubble("bubble");
        	          }
        	          else {
        	        	  f.setBubble("bubble2");
        	          }
        	          Logger.info("adding filter " + f.getBubble());
        	          filters.add(f);
        		}
        	}
        }
      }
    }

    return ok(play.libs.Json.toJson(filters));

  }

  public static Result remove(String property) {
    Logger.debug("in method remove(String property), removing filter with property " + property);
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    Filter filter = Application.getFilterFromSession();
    BasicDBObject query = new BasicDBObject("descriminator", filter.getDescriminator());
    query.put("collection", filter.getCollection());
    query.put("property", property);

    DBCursor cursor = p.find(Constants.TBL_FILTERS, query);
    if (cursor.count() == 0) {
      Logger.debug("No filter found for property: " + property);
    } else if (cursor.count() == 1) {
      Logger.debug("Removing filter for property: " + property);
      Filter tmp = DataHelper.parseFilter(cursor.next());
      p.getDB().getCollection(Constants.TBL_FILTERS).remove(tmp.getDocument());
    } else {
      Logger.error("Something went wrong, while removing filter for property: " + property);
      throw new RuntimeException("Two many filters found for property " + property);
    }

    return ok();
  }
  
  public static Result removeBubbleFilter(String property1, String property2) {
	    Logger.debug("in method remove(String property), removing filter with property " + property1 + ", " +  property2);
	    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
	    Filter filter = Application.getFilterFromSession();
	    BasicDBObject query = new BasicDBObject("descriminator", filter.getDescriminator());
	    query.put("collection", filter.getCollection());
	    query.put("property0", property1);
	    query.put("property1", property2);

	    DBCursor cursor = p.find(Constants.TBL_FILTERS, query);
	    if (cursor.count() == 0) {
	      Logger.debug("No filter found for property: " + property1 + ", " +  property2);
	    } else if (cursor.count() == 1) {
	      Logger.debug("Removing filter for property: " + property1 + ", " +  property2);
	      Filter tmp = DataHelper.parseFilter(cursor.next());
	      p.getDB().getCollection(Constants.TBL_FILTERS).remove(tmp.getDocument());
	    } else {
	      Logger.error("Something went wrong, while removing filter for property: " + property1 + ", " +  property2);
	      throw new RuntimeException("Too many filters found for property " + property1 + ", " +  property2);
	    }

	    return ok();
	  }

  public static Result add() {
    Logger.debug("in method add(), adding new filter");
    // final List<String> names = Application.getCollectionNames();
    Filter filter = Application.getFilterFromSession();

    if (filter != null) {
      final DynamicForm form = form().bindFromRequest();
      final String f = form.get("filter");
      final String v = form.get("value");
      final String t = form.get("type");
      final String a = form.get("alg");
      final String w = form.get("width");

      if (t == null || t.equals("normal")) {
        return addFromFilter(filter, f, v);
      } else if (t.equals("graph")) {
        int value = Integer.parseInt(v);

        return addFromGraph(filter, f, value, a, w);
      }
    }

    return badRequest("No filter was found in the session\n");
  }
  
  public static Result addBubbleFilter() {
	    Logger.debug("in method addBubbleFilter(), adding new bubble filter");
	    Filter filter = Application.getFilterFromSession();

	    if (filter != null) {
	      final DynamicForm form = form().bindFromRequest();
	      final String f1 = form.get("property0");
	      final String f2 = form.get("property1");
	      final String v1 = form.get("value0");
	      final String v2 = form.get("value1");
	      final String t = form.get("type");
	      final String a = form.get("alg");
	      final String w = form.get("width");
	      

//	      if (t == null || t.equals("normal")) {
	    	  	//return addFromFilter(filter, f1, v1);
//	    	  	return addFromBubbleFilter(filter, f1, v1, f2, v2);
	    	  addFromFilter(filter, f1, v1);
	    	  addFromFilter(filter, f2, v2);
	    	  
//	        } else if (t.equals("graph")) {
//	        	// TODO ALEX int values and bubblegraph method
//	        	return addFromBubbleFilter(filter, f1, v1, f2, v2);
//	        	//int value = Integer.parseInt(v1);
//	        	//return addFromGraph(filter, f1, value, a, w);
//	        }
	    	  return ok();
	    }
	    
	    return badRequest("No filter was found in the session\n");
  }

  private static Result addFromFilter(Filter filter, String f, String v) {
    Logger.debug("in method addFromFilter(), adding new filter with property '" + f + "' and value '" + v + "'");
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();

    BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
    ref.put("collection", filter.getCollection());
    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);
    boolean existing = false;
    while (cursor.hasNext()) {
      Filter tmp = DataHelper.parseFilter(cursor.next());
//      if(tmp.getType() != null && tmp.getType().equals("bubblefilter")) {
//		  continue;
//	  }
      if (tmp.getProperty() != null && tmp.getProperty().equals(f)) {
        Logger.debug("Filter is already present, changing value");
        p.getDB().getCollection(Constants.TBL_FILTERS).remove(tmp.getDocument());

        tmp.setValue(v);
        p.insert(Constants.TBL_FILTERS, tmp.getDocument());
        existing = true;
        break;
      }
    }

    if (!existing) {
      Logger.info("Filtering based on new filter: " + filter + " " + v);
      Filter newFilter = new Filter(filter.getCollection(), f, v);
      newFilter.setDescriminator(filter.getDescriminator());
      p.insert(Constants.TBL_FILTERS, newFilter.getDocument());
    }

    return ok();

  }
  
  private static Result addFromBubbleFilter(Filter filter, String f1, String v1, String f2, String v2) {
	    Logger.debug("in method addFromBubbleFilter(), adding new filter with properties '" + f1 + "," + v1 + "' and values '" + f2 + "," + v2 + "'");
	    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();

	    BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
	    ref.put("collection", filter.getCollection());
	    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);
	    boolean existing = false;
	    while (cursor.hasNext()) {
	      Filter tmp = DataHelper.parseFilter(cursor.next());
    	  if(tmp.getType() == null || tmp.getType().equals("null")) {
    		  continue;
    	  }
    	  
    	  String p1 = tmp.getBubbleProperty(0);
    	  String p2 = tmp.getBubbleProperty(1);
    	  
    	  if(p1 != null && p2 != null) {
    		  if( (p1.equals(f1) && p2.equals(f2)) || (p2.equals(f1) && p1.equals(f2)) ) {
    			  Logger.debug("Filter is already present, changing value");
    		      p.getDB().getCollection(Constants.TBL_FILTERS).remove(tmp.getDocument());
    		        
    			  tmp.setBubbleValue(0, tmp.getBubbleValue(0));
    			  tmp.setBubbleValue(1, tmp.getBubbleValue(1));
    			  
    			  p.insert(Constants.TBL_FILTERS, tmp.getDocument());
			      existing = true;
			      break;
    		  }
    	  }
	    }

	    if (!existing) {
	      Logger.info("Filtering based on new filter: " + f1 + " ," + f2 + "' and values '" + v1 + "," + v2 + "'");
	      Filter newFilter = new Filter(filter.getCollection(), f1, v1, f2, v2);
	      newFilter.setDescriminator(filter.getDescriminator());
	      p.insert(Constants.TBL_FILTERS, newFilter.getDocument());
	      
	    }
	    return ok();

	  }

  private static Result addFromGraph(Filter filter, String f, int value, String alg, String width) {
    Logger.debug("in method addFromGraph(), adding new filter with property '" + f.toString() + "' and position value '" + value
        + "'");
    Logger.info("Current filter was: " + filter.getDescriminator());
    // query histogram to check the value of the filter that was selected

    final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
    final Property property = cache.getProperty(f);
    Graph graph = null;

    if (property.getType().equals(Property.PropertyType.INTEGER.toString())) {
      graph = getNumericGraph(filter, f, alg, width);
    } else {
      graph = getGraph(filter, f);
    }

    final String filtervalue = graph.getKeys().get(value);

    return addFromFilter(filter, f, filtervalue);
  }
  
  // TODO ALEX
  private static Result addFromBubbleGraph(Filter filter, String f1, int val1, String f2, int val2, String alg, String width) {
	  	String v1 = null;
	  	String v2 = null;
	    return null;
	  	//return addFromBubbleFilter(filter, f1, v1, f2, v2);
    }

  public static Result getValues() {
    Logger.debug("in method getValues(), retrieving values for selected property");
    final DynamicForm form = form().bindFromRequest();
    final String c = form.get("collection");
    final String p = form.get("filter");

    // get algorithm and width
    final String a = form.get("alg");
    final String w = form.get("width");

    final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
    final Property property = cache.getProperty(p);
    PropertyValuesFilter f = null;
    if (property.getType().equals(PropertyType.INTEGER.toString())) {
      f = getNumericValues(c, property, a, w);
    } else {
      f = getValues(c, property, null);
    }

    return ok(play.libs.Json.toJson(f));
  }

  private static PropertyValuesFilter getValues(String c, Property p, String v) {
    Logger.debug("get property values filter for " + c + " and property " + p.getId());

    final MapReduceJob job = new HistogramJob(c, p.getId());

    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      // int width = (v == null) ? 10 : HistogramJob.inferBinWidth(v);
      int width = HistogramJob.inferBinWidth(v);

      HashMap<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");
      job.setConfig(config);
    }

    final MapReduceOutput output = job.execute();
    final List<String> keys = new ArrayList<String>();
    final List<String> values = new ArrayList<String>();

    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      // int width = (v == null) ? 10 : HistogramJob.inferBinWidth(v);
      int width = HistogramJob.inferBinWidth(v);

      calculateNumericHistogramResults(output, keys, values, width);
    } else {
      calculateHistogramResults(output, keys, values);
    }

    PropertyValuesFilter f = new PropertyValuesFilter();
    f.setProperty(p.getId());
    f.setType(p.getType());
    f.setValues(keys); // this is not a mistake.
    f.setSelected(v);

    return f;
  }

  private static PropertyValuesFilter getNumericValues(String c, Property p, String alg, String width) {
    Filter filter = Application.getFilterFromSession();
    Graph graph = null;

    if (alg.equals("fixed")) {
      int w = Integer.parseInt(width);
      graph = getFixedWidthHistogram(filter, p.getId(), w);
    } else if (alg.equals("sqrt")) {
      graph = getSquareRootHistogram(filter, p.getId());
    } else if (alg.equals("sturge")) {
      graph = getSturgesHistogramm(filter, p.getId());
    }

    graph.sort();
//
//    if (graph.getKeys().size() > 100) {
//      graph.cutLongTail();
//    }
    
    PropertyValuesFilter f = new PropertyValuesFilter();
    f.setProperty(p.getId());
    f.setType(p.getType());
    f.setValues(graph.getKeys()); // this is not a mistake.

    return f;
  }

  public static Graph getGraph(String property) {
    Filter filter = Application.getFilterFromSession();

    DynamicForm form = form().bindFromRequest();
    String alg = form.get("alg");
    Graph g = null;

    if (alg == null) {
      g = getOrdinalGraph(filter, property);
    } else {
      g = getNumericGraph(filter, property, form.get("alg"), form.get("width"));
    }

    if (g != null) {
      g.sort();

      if (g.getKeys().size() > 100) {
        g.cutLongTail();
      }
    }
    return g;
  }

  public static Graph getGraph(String collection, String property) {
    final PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    final List<String> keys = new ArrayList<String>();
    final List<String> values = new ArrayList<String>();
    final Graph result = new Graph(property, keys, values);

    DBCollection dbc = p.getDB().getCollection("histogram_" + collection + "_" + property);

    if (dbc.find().count() == 0) {
      final MapReduceJob job = new HistogramJob(collection, property);
      final MapReduceOutput output = job.execute();

      calculateHistogramResults(output, keys, values);

    } else {
      DBCursor cursor = dbc.find();
      while (cursor.hasNext()) {
        BasicDBObject dbo = (BasicDBObject) cursor.next();
        parseHistogram(dbo, keys, values);
      }
    }

    result.sort();

    if (result.getKeys().size() > 100) {
      result.cutLongTail();
    }

    return result;
  }

  public static Graph getGraph(Filter filter, String property) {
    final List<String> keys = new ArrayList<String>();
    final List<String> values = new ArrayList<String>();
    final Graph result = new Graph(property, keys, values);
    final BasicDBObject query = Application.getFilterQuery(filter);
    final MapReduceJob job = new HistogramJob(filter.getCollection(), property, query);

    final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
    final Property p = cache.getProperty(property);
    long width = -1;

    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      DBObject range = (DBObject) query.get("metadata." + property + ".value");
      
      Long low = (Long) range.get("$gte");
      Long high = (Long) range.get("$lte");
      
      width = high - low + 1; //because of lte/gte

      HashMap<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");
      job.setConfig(config);

    }

    final MapReduceOutput output = job.execute();
    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      calculateNumericHistogramResults(output, keys, values, width);
    } else {
      calculateHistogramResults(output, keys, values);
    }
    
    result.sort();

    if (result.getKeys().size() > 100) {
      result.cutLongTail();
    }
    
    return result;
  }

  public static Statistics getCollectionStatistics(Filter filter) {
    final NumericAggregationJob job = new NumericAggregationJob(filter.getCollection(), "size");
    final BasicDBObject query = Application.getFilterQuery(filter);
    job.setFilterquery(query);

    final MapReduceOutput output = job.execute();
    final List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    BasicDBObject aggregation = null;

    if (!results.isEmpty()) {
      aggregation = (BasicDBObject) results.get(0).get("value");
    }

    return getStatisticsFromResult(aggregation);
  }

  public static Statistics getCollectionStatistics(String name) {
    final PersistenceLayer pl = Configurator.getDefaultConfigurator().getPersistence();
    BasicDBObject aggregation = null;

    DBCollection collection = pl.getDB().getCollection("statistics_" + name);
    if (collection.find().count() != 0) {
      aggregation = (BasicDBObject) collection.findOne().get("value");
    }

    if (aggregation == null) {
      final NumericAggregationJob job = new NumericAggregationJob(name, "size");
      job.setType(OutputType.REPLACE);
      job.setOutputCollection("statistics_" + name);
      final MapReduceOutput output = job.execute();

      if (output != null) {
        aggregation = (BasicDBObject) collection.findOne().get("value");
      }
    }

    return getStatisticsFromResult(aggregation);
  }

  public static Statistics getStatisticsFromResult(BasicDBObject aggregation) {
    if (aggregation == null)
      return null;

    final DecimalFormat df = new DecimalFormat("#.##");
    Statistics stats = new Statistics();
    stats.setCount(aggregation.getInt("count") + " objects");
    stats.setSize(df.format(aggregation.getLong("sum") / 1024D / 1024) + " MB");
    stats.setAvg(df.format(aggregation.getDouble("avg") / 1024 / 1024) + " MB");
    stats.setMin(aggregation.getLong("min") + " B");
    stats.setMax(df.format(aggregation.getLong("max") / 1024D / 1024) + " MB");
    stats.setSd(df.format(aggregation.getDouble("stddev") / 1024 / 1024) + " MB");
    stats.setVar(df.format(aggregation.getDouble("variance") / 1024 / 1024 / 1024 / 1024) + " MB");
    // because of sd^2
    return stats;
  }
  
  public static BubbleGraph getBubbleGraph(Filter filter, String property1, 
		  String property2) {
	  return getBubbleGraph(filter, property1, property2, null, null, null, null);
  }

  public static BubbleGraph getBubbleGraph(String property1, String property2,
		  String alg1, String width1, String alg2, String width2) {
	  
	Filter filter = Application.getFilterFromSession();
	return getBubbleGraph(filter, property1, property2, alg1, width1, alg2, width2);
  }
  
  public static BubbleGraph getBubbleGraph(Filter filter, 
		  String property1, String property2,
		  String alg1, String width1, String alg2, String width2) {
	if (filter == null)
		return null;
	
	BubbleGraph g = null;
	
    BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator())
    	.append("collection", filter.getCollection());
    DBCursor cursor = Configurator.getDefaultConfigurator()
    		.getPersistence().find(Constants.TBL_FILTERS, ref);
    
    int bin_width1 = getBinWidth(filter, property1, alg1, width1);
    Logger.debug("got bin_width 1: " + Integer.toString(bin_width1));
    int bin_width2 = getBinWidth(filter, property2, alg2, width2);
    Logger.debug("got bin_width 2: " + Integer.toString(bin_width2));
    
    
    List<? extends DBObject> graphData;
    if (cursor.count() == 1 &&	// only root filter
    		bin_width1 == -1 && bin_width2 == -1) {	// numeric porperties selected 
      // use cached values
      Logger.info("getBubbleChart: no filter, use cached values");
      final PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
      final String cacheCol = "bubblechart_" 
    		  + filter.getCollection() + "_" + property1 + "_" + property2;
      DBCollection dbc = p.getDB().getCollection(cacheCol);
      DBCursor cacheCursor = dbc.find();
	  if (cacheCursor.count() == 0) {
		Logger.info("getBubbleChart: cache doesn't exist yet.");
		final MapReduceJob job = new BubbleChartJob(filter.getCollection(),
				property1, property2);
		job.setType(OutputType.REPLACE);
		job.setOutputCollection(cacheCol);
		graphData = job.run().getResults();
	  }
	  cacheCursor = dbc.find();
	  graphData = cacheCursor.toArray();
	  
    } else {
      // apply filter query
      Logger.info("getBubbleChart: filter active or numeric properties " + 
    		  "selected, can not use cache");
      
      final MapReduceJob job = new BubbleChartJob(filter.getCollection(), 
    		  property1, property2, Application.getFilterQuery(filter));
      job.getConfig().put("bin_width_" + property1, Integer.toString(bin_width1));
      job.getConfig().put("bin_width_" + property2, Integer.toString(bin_width2));
      graphData = job.run().getResults();
    }
    
    g = new BubbleGraph(property1, property2);
    g.setFromMapReduceJob(graphData);
    g.sort();
    g.convertNumericKeysToIntervals(bin_width1, bin_width2);

    return g;
  }
  
   
  private static Graph getOrdinalGraph(Filter filter, String property) {
    Graph g = null;
    if (filter != null) {
      BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
      DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);
      if (cursor.count() == 1) { // only root filter
        g = FilterController.getGraph(filter.getCollection(), property);
      } else {
        g = FilterController.getGraph(filter, property);
      }

    }

    return g;
  }

  private static Graph getNumericGraph(Filter filter, String property, String alg, String w) {

    // TODO find number of elements based on filter...
    // calculate bins...
    // find classes based on number of bins...
    // map reduce this property based on the classes...
    Graph g = null;
    if (alg.equals("fixed")) {
      int width = DEFAULT_BIN_WIDTH;
      try {
        width = Integer.parseInt(w);
      } catch (NumberFormatException e) {
        Logger.warn("Not a number, using default bin width: " 
        		+ Integer.toString(DEFAULT_BIN_WIDTH));
      }

      g = getFixedWidthHistogram(filter, property, width);
      g.getOptions().put("width", w);

    } else if (alg.equals("sturge")) {
      // bins = log2 n + 1
      g = getSturgesHistogramm(filter, property);
    } else if (alg.equals("sqrt")) {
      // bins = sqrt(n);
      g = getSquareRootHistogram(filter, property);
    }

    g.getOptions().put("type", PropertyType.INTEGER.toString());
    g.getOptions().put("alg", alg);

    
    g.sort();
    
    return g;
  }
  
  private static int getBinWidth(Filter filter, String propertyName, 
		  String algo, String width) {
	  
	int result = -1;
	int bins = -1;
	long max = -1;
	int n = -1;
	
	PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    Property property = p.getCache().getProperty(propertyName);
    
    if (! property.getType().equals(PropertyType.INTEGER.name()) &&
    		! property.getType().equals(PropertyType.FLOAT.name())) {
      return -1;	// bin width only useful for numeric properties
    }
	
	BasicDBObject aggregation;
	if (algo == null) {
		Logger.error("no algorithm for numeric property specified, using default bin width of "
				+ Integer.toString(DEFAULT_BIN_WIDTH));
		return DEFAULT_BIN_WIDTH;
	}
	if(algo.equals("fixed")) {
		try {
			  result = Integer.parseInt(width);
		  } catch (NumberFormatException e) {
			  Logger.error("given width '" + width + "' does not seem to be a " + 
					  "valid number. using default value " + 
					  Integer.toString(DEFAULT_BIN_WIDTH));
		  }
	}
	else if (algo.equals("sqrt")) {
		n = getTotalNumberOfElements(filter);
		  bins = (int) Math.sqrt(n);
		  aggregation = getNumericAggregationResult(filter, propertyName);
		  max = aggregation.getLong("max");
	      result = (int) (max / bins);
	} 
	else if (algo.equals("sturge")) {
		n = getTotalNumberOfElements(filter);
		  bins = (int) ((Math.log(n) / Math.log(2)) + 1);
		  aggregation = getNumericAggregationResult(filter, propertyName);
		  max = aggregation.getLong("max");
	      result = (int) (max / bins);
	}
	else {
		Logger.error("unknown algorithm for numeric porperty, using default bin width of " 
				+ Integer.toString(DEFAULT_BIN_WIDTH));
		result = DEFAULT_BIN_WIDTH;
	}
	/*
	switch (algo) {
	case "fixed":
	  try {
		  result = Integer.parseInt(width);
	  } catch (NumberFormatException e) {
		  Logger.error("given width '" + width + "' does not seem to be a " + 
				  "valid number. using default value " + 
				  Integer.toString(DEFAULT_BIN_WIDTH));
	  }
	  break;
		  
	case "sqrt":
	  n = getTotalNumberOfElements(filter);
	  bins = (int) Math.sqrt(n);
	  aggregation = getNumericAggregationResult(filter, propertyName);
	  if (aggregation == null)
		  return -1;
	  max = aggregation.getLong("max");
      result = (int) (max / bins);
	  break;
		  
	case "sturge":
	  n = getTotalNumberOfElements(filter);
	  bins = (int) ((Math.log(n) / Math.log(2)) + 1);
	  aggregation = getNumericAggregationResult(filter, propertyName);
	  if (aggregation == null)
		  return -1;
	  max = aggregation.getLong("max");
      result = (int) (max / bins);
	  break;
		  
	default:
		Logger.error("unknown algorithm for numeric porperty, using default bin width of " 
				+ Integer.toString(DEFAULT_BIN_WIDTH));
		result = DEFAULT_BIN_WIDTH;
	}
	*/
	return result;
  }
  
  private static int getTotalNumberOfElements(Filter filter) {
	 BasicDBObject query = Application.getFilterQuery(filter);
	 DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence()
			 .find(Constants.TBL_ELEMENTS, query);
	 return cursor.size();
  }

  private static BasicDBObject getNumericAggregationResult(Filter filter, 
		  String property) {
	BasicDBObject query = Application.getFilterQuery(filter);
	MapReduceJob job = new NumericAggregationJob(filter.getCollection(), property);
	job.setFilterquery(query);

	JobResult res = job.run();
	if (res.getResults().isEmpty())
		return null;
	
	return (BasicDBObject) res.getResults().get(0).get("value");
  }
  
  private static Graph getFixedWidthHistogram(Filter filter, String property, int width) {
    BasicDBObject query = Application.getFilterQuery(filter);
    MapReduceJob job = new NumericAggregationJob(filter.getCollection(), property);
    job.setFilterquery(query);

    MapReduceOutput output = job.execute();
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    Graph g = null;
    if (!results.isEmpty()) {
      BasicDBObject aggregation = (BasicDBObject) results.get(0).get("value");
      long min = aggregation.getLong("min");
      long max = aggregation.getLong("max");

      int bins = (int) ((max - min) / width);
      Map<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");

      job = new HistogramJob(filter.getCollection(), property);
      job.setFilterquery(query);
      job.setConfig(config);
      output = job.execute();
      results = (List<BasicDBObject>) output.getCommandResult().get("results");
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();

      calculateNumericHistogramResults(output, keys, values, width);

      g = new Graph(property, keys, values);
    }

    return g;

  }

  private static Graph getSturgesHistogramm(Filter f, String property) {
    BasicDBObject query = Application.getFilterQuery(f);
    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_ELEMENTS, query);
    int n = cursor.size();
    int bins = (int) ((Math.log(n) / Math.log(2)) + 1);
    MapReduceJob job = new NumericAggregationJob(f.getCollection(), property);
    job.setFilterquery(query);

    MapReduceOutput output = job.execute();
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    Graph g = null;
    if (!results.isEmpty()) {
      BasicDBObject aggregation = (BasicDBObject) results.get(0).get("value");
      long max = aggregation.getLong("max");
      int width = (int) (max / bins);
      Map<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");

      job = new HistogramJob(f.getCollection(), property);
      job.setFilterquery(query);
      job.setConfig(config);
      output = job.execute();
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();

      calculateNumericHistogramResults(output, keys, values, width);

      g = new Graph(property, keys, values);
    }

    return g;
  }

  private static Graph getSquareRootHistogram(Filter f, String property) {
    BasicDBObject query = Application.getFilterQuery(f);
    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_ELEMENTS, query);
    int n = cursor.size();
    int bins = (int) Math.sqrt(n);
    MapReduceJob job = new NumericAggregationJob(f.getCollection(), property);
    job.setFilterquery(query);

    MapReduceOutput output = job.execute();
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    Graph g = null;
    if (!results.isEmpty()) {
      BasicDBObject aggregation = (BasicDBObject) results.get(0).get("value");
      long max = aggregation.getLong("max");
      int width = (int) (max / bins);
      Map<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");

      job = new HistogramJob(f.getCollection(), property);
      job.setFilterquery(query);
      job.setConfig(config);
      output = job.execute();
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();

      calculateNumericHistogramResults(output, keys, values, width);

      g = new Graph(property, keys, values);
    }

    return g;
  }

  private static void calculateHistogramResults(MapReduceOutput output, List<String> keys, List<String> values) {
    final List<BasicDBObject> jobresults = (List<BasicDBObject>) output.getCommandResult().get("results");
    for (final BasicDBObject dbo : jobresults) {
      parseHistogram(dbo, keys, values);
    }
  }

  private static void calculateNumericHistogramResults(MapReduceOutput output, List<String> keys, List<String> values,
      long width) {
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    for (BasicDBObject obj : results) {
      
      String id = obj.getString("_id");

      if (!id.equals("Unknown") && !id.equals("Conflicted")) {
        long low = (int) Double.parseDouble(id) * width;
        long high = low + width - 1;
        keys.add(low + " - " + high);
      } 
      values.add(obj.getString("value"));
    }
  }

  private static void parseHistogram(BasicDBObject dbo, List<String> keys, List<String> values) {
    String key = dbo.getString("_id");
    if (key.endsWith(".0")) {
      key = key.substring(0, key.length() - 2);
    }
    keys.add(key);
    values.add(dbo.getString("value"));
  }
}
