package helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import play.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.datamodel.Property;
import com.petpet.c3po.datamodel.Property.PropertyType;
import com.petpet.c3po.utils.Configurator;


public class BubbleGraph implements BaseGraph {

	private String property1;
	private String property2;
	
	private List<String> keys1;
	private List<String> keys2;
	private List<Long> values;
	
	private Map<String, Integer> keyMap1;
	private Map<String, Integer> keyMap2;
	
	public BubbleGraph(String property1, String property2) {
		this.property1 = property1;
		this.property2 = property2;
	}
	
	
	public void setFromMapReduceJob(List<? extends DBObject> results) {
		
		keys1 = new ArrayList<String>(results.size());
		keys2 = new ArrayList<String>(results.size());
		values = new ArrayList<Long>(results.size());
		
		keyMap1 = new HashMap<String, Integer>();
		keyMap2 = new HashMap<String, Integer>();
		
		for (DBObject obj : results) {
//			String key = obj.getString("_id");
			BasicDBObject value = (BasicDBObject) obj.get("value");
			
			values.add(value.getLong("count"));
			String key1 = value.getString("value1");
			String key2 = value.getString("value2");
			
			keys1.add(key1);
			keys2.add(key2);
//			int separator = value.getInt("separator");
			
			if (keyMap1.get(key1) == null) {
				keyMap1.put(key1, keyMap1.size());
			}
			
			if (keyMap2.get(key2) == null) {
				keyMap2.put(key2, keyMap2.size());
			}
		}
	}

	
	public void convertNumericKeysToIntervals(int bin_width1, int bin_width2) {
		if (bin_width1 > 0) {
			for (int i = 0; i < keys1.size(); i++) {
				keys1.set(i, formatNumericKey(keys1.get(i), bin_width1));
			}
		}
		if (bin_width2 > 0) {
			for (int i = 0; i < keys2.size(); i++) {
				keys2.set(i, formatNumericKey(keys2.get(i), bin_width2));
			}
		}
	}
	
	private String formatNumericKey(String key, int bin_width) {
		if (!key.equals("Unknown") && !key.equals("Conflicted")) {
			try {
				long low = Long.parseLong(key) * bin_width;
				long high = low + bin_width - 1;
				key = Long.toString(low) + " - " + Long.toString(high);
			} catch (NumberFormatException e) {
				// do nothing here, leave key 1 unchanged
				Logger.error("Number format exception", e);
			}
		}
		return key;
	}

	@Override
	public String getTitle() {
		return property1 + "_" + property2;
	}


	@Override
	public String getType() {
		return "bubblechart";
	}


	@Override
	public String getGraphData() {
		StringBuilder res = new StringBuilder();
		res.append("[");
		boolean first = true;
		for (int i = 0; i < keys1.size(); i++) {
			if (first)
				first = false;
			else
				res.append(", ");
			res.append("['");
			res.append(keys1.get(i));
			res.append("', '");
			res.append(keys2.get(i));
			res.append("', ");
			res.append(values.get(i));
			res.append(", '");
			res.append(keys1.get(i) + " - " + keys2.get(i));
			res.append("']");
		}
		res.append("]");
		return res.toString();
	}


	@Override
	public String getGraphOptions() {
		StringBuilder res = new StringBuilder();
		res.append("{");
		
		res.append(" 'keymap1': ");
		res.append(getKeyMapArray(keyMap1));
		res.append(", ");
		
		res.append(" 'keymap2': ");
		res.append(getKeyMapArray(keyMap2));
		res.append("");
		
		res.append("}");
		return res.toString();
	}
	
	
	public void sort() {
		PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
	    Property p1 = p.getCache().getProperty(property1);
	    Property p2 = p.getCache().getProperty(property2);

	    /*
	     * TODO can we do this better? 
	     */
	    if (p1.getType().equals(PropertyType.INTEGER.name()) ||
	    	p1.getType().equals(PropertyType.DATE.name())) {

	    	if (p2.getType().equals(PropertyType.INTEGER.name()) ||
	    		p2.getType().equals(PropertyType.DATE.name())) {
	    	
	    		Logger.debug("sort by key 1 and key 2");
	    		TreeMap<Integer, TreeMap<Integer, Long>> map = new TreeMap<Integer, TreeMap<Integer, Long>>();
	    		try {
		    		for (int i = 0; i < keys1.size(); i++) {
		    			int k1 = new Integer(keys1.get(i));
		    			int k2 = new Integer(keys2.get(i));
		    			
		    			if (map.get(k1) == null) {
		    				TreeMap<Integer, Long> t = new TreeMap<Integer, Long>();
		    				t.put(k2, values.get(i));
		    				map.put(k1, t);
		    			} else {
		    				map.get(k1).put(k2, values.get(i));
		    			}
		    		}
		    		keys1.clear();
		    		keys2.clear();
		    		values.clear();
		    		for (Iterator<Integer> it = map.keySet().iterator(); it.hasNext() ; ) {
		    			Integer curK1 = it.next();
		    			TreeMap<Integer, Long> curT = map.get(curK1);
		    			for (Iterator<Integer> it2 = curT.keySet().iterator(); it2.hasNext() ; ) {
		    				Integer curK2 = it2.next();
		    				keys1.add(curK1.toString());
		    				keys2.add(curK2.toString());
		    				values.add(curT.get(curK2));
		    			}
		    		}
		    		return ;
	    		} catch (NumberFormatException e) {
	    			// ignore this, just try to sort only by key1
	    			Logger.error("some value for key 2 is not an Integer. just try sorting by key 1");
	    		}
	    	} 
	    	
    		Logger.debug("sort by key 1");
	    	TreeMap<Integer, TreeMap<String, Long>> map = new TreeMap<Integer, TreeMap<String, Long>>();
    		try {
	    		for (int i = 0; i < keys1.size(); i++) {
	    			int k1 = new Integer(keys1.get(i));
	    			String k2 = keys2.get(i);
	    			
	    			if (map.get(k1) == null) {
	    				TreeMap<String, Long> t = new TreeMap<String, Long>();
	    				t.put(k2, values.get(i));
	    				map.put(k1, t);
	    			} else {
	    				map.get(k1).put(k2, values.get(i));
	    			}
	    		}
    		} catch (NumberFormatException e) {
    			Logger.error("some value for key 1 is not an Integer. ignoring the sort", e);
    			return;
    		}
			keys1.clear();
			keys2.clear();
			values.clear();
			for (Iterator<Integer> it = map.keySet().iterator(); it.hasNext();) {
				Integer curK1 = it.next();
				TreeMap<String, Long> curT = map.get(curK1);
				for (Iterator<String> it2 = curT.keySet().iterator(); it2
						.hasNext();) {
					String curK2 = it2.next();
					keys1.add(curK1.toString());
					keys2.add(curK2.toString());
					values.add(curT.get(curK2));
				}
			}
    		
	    } else {
	    	if (p2.getType().equals(PropertyType.INTEGER.name()) ||
		    		p2.getType().equals(PropertyType.DATE.name())) {
		    
	    		Logger.debug("sort by key 2");
				TreeMap<String, TreeMap<Integer, Long>> map = new TreeMap<String, TreeMap<Integer, Long>>();
				try {
					for (int i = 0; i < keys1.size(); i++) {
						String k1 = keys1.get(i);
						int k2 = new Integer(keys2.get(i));

						if (map.get(k1) == null) {
							TreeMap<Integer, Long> t = new TreeMap<Integer, Long>();
							t.put(k2, values.get(i));
							map.put(k1, t);
						} else {
							map.get(k1).put(k2, values.get(i));
						}
					}
					keys1.clear();
					keys2.clear();
					values.clear();
					for (Iterator<String> it = map.keySet().iterator(); it
							.hasNext();) {
						String curK1 = it.next();
						TreeMap<Integer, Long> curT = map.get(curK1);
						for (Iterator<Integer> it2 = curT.keySet().iterator(); it2
								.hasNext();) {
							Integer curK2 = it2.next();
							keys1.add(curK1.toString());
							keys2.add(curK2.toString());
							values.add(curT.get(curK2));
						}
					}
				} catch (NumberFormatException e) {
					// ignore this, just try to sort only by key1
					Logger.error("some value for key 2 is not an Integer. ignore the sort");
				}
			}
	    }
	}
	
	private String getKeyMapArray(Map<String, Integer> map) {
		StringBuilder res = new StringBuilder();
		res.append("[");
		boolean first = true;
		for (String key : map.keySet()) {
			if (first)
				first = false;
			else
				res.append(", ");
			res.append("'" + key + "'");  
		}
		res.append("]");
		return res.toString();
	}
}
