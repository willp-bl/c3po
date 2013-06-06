package helpers;

import java.util.Map;

public interface BaseGraph {

	public String getTitle();
	
	public String getType();
	
	public String getGraphData();
	
	public String getGraphOptions();

	public void setOptions(Map<String, String> options);
	
	public Map<String, String> getOptions();

	
}
