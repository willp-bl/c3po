package helpers;

import java.util.List;

public class GraphData {

  private List<BaseGraph> graphs;

  public GraphData() {
    
  }
  
  public GraphData(List<BaseGraph> graphs) {
    this.setGraphs(graphs);
  }

  public List<BaseGraph> getGraphs() {
    return graphs;
  }

  public void setGraphs(List<BaseGraph> graphs) {
    this.graphs = graphs;
  }
}
