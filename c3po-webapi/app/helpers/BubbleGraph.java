package helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


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
		res.append("[\n");
		
		for (int i = 0; i < keys1.size(); i++) {
			res.append("[");
			res.append(keyMap1.get(keys1.get(i)));
			res.append(", ");
			res.append(keyMap2.get(keys2.get(i)));
			res.append(", ");
			res.append(values.get(i));
			res.append(", '");
			res.append(keys1.get(i) + " - " + keys2.get(i));
			res.append("'], \n");
		}
		res.append("]\n");
		return res.toString();
	}


	@Override
	public String getGraphOptions() {
		StringBuilder res = new StringBuilder();
		res.append("{\n");
		
		res.append("keymap1: ");
		res.append(getKeyMapArray(keyMap1));
		res.append(", \n");
		
		res.append("keymap2: ");
		res.append(getKeyMapArray(keyMap2));
		res.append("\n");
		
		res.append("}\n");
		return res.toString();
	}
	
	
	private String getKeyMapArray(Map<String, Integer> map) {
		StringBuilder res = new StringBuilder();
		res.append("[");
		for (String key : map.keySet()) {
			res.append("'" + key + "', ");  
		}
		res.append("]");
		return res.toString();
	}
}
