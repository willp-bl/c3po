package helpers;

import java.util.List;

public class BubbleFilter implements FilterHelper {
  
  private String type;
  private PropertyValuesFilter f1;
  private PropertyValuesFilter f2;

  public void addPropertyValuesFilter(int i, PropertyValuesFilter f) {
	  if(i==1) {
		  f1 = f;
	  }
	  else {
		  f2 = f;
	  }
  }
  
  public PropertyValuesFilter getPropertyValuesFilter(int i) {
	  if(i==1) {
		  return f1;
	  }
	  else {
		  return f2;
	  }
  }
  
  public String getProperty(int i) {
	if(i==1) {
		return f1.getProperty();
	}
    return f2.getProperty();
  }

  public void setProperty(int i, String property) {
	  if(i==1) {
			f1.setProperty(property);
		}
	  else {
		  f2.setProperty(property);
	  }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getValues(int i) {
		if(i==1) {
			return f1.getValues();
		}
	    return f2.getValues();
  }

  public void setValues(int i, List<String> values) {
	  if(i==1) {
			f1.setValues(values);
		}
	  else {
		  f2.setValues(values);
	  }
  }

  public String getSelected(int i) {
	  if(i==1) {
			return f1.getSelected();
		}
	    return f2.getSelected();
  }

  public void setSelected(int i, String selected) {
	  if(i==1) {
			f1.setSelected(selected);
		}
	  else {
		  f2.setSelected(selected);
	  }
  }
}
