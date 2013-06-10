package com.petpet.c3po.datamodel;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class is used to filter a c3po collection on many levels. It has a
 * matching and non-matching child filters that can be applied to the current
 * partition of the collection.
 * 
 * @author Petar Petrov <me@petarpetrov.org>
 * 
 */
public class Filter {

	private String descriminator;

	/**
	 * The collection that is filtered.
	 */
	private String collection;

	/**
	 * The property that is filtered by this filter (e.g. mimetype).
	 */
	private String property;

	/**
	 * The value of the property by which the filter is partitioning (e.g.
	 * application/pdf).
	 */
	private String value;

	/**
	 * The type of filter:
	 * 		null = basic filter
	 * 		bubblefilter = bubble chart filter with 2 property - value pairs
	 */
	private String type;

	/**
	 * The list of property - value pairs of the bubble filter
	 */
	private List<Entry<String, String>> properties;

	/**
	 * The ID of the filter
	 * If this filter is part of a bubble filter,
	 * the ID should be set
	 */
	private String bubbleFilterID;

	/**
	 * Creates a default root filter. This means that the filter has no parent
	 * filter.
	 * 
	 * @param collection
	 *          the collection to filter.
	 * @param property
	 *          the property to apply.
	 * @param value
	 *          the value of the property to apply for this filter.
	 */
	public Filter(String collection, String property, String value) {
		type = "null";
		this.collection = collection;
		this.property = property;
		this.value = value;
	}


	/**
	 * Creates a default root filter for Bubble Charts.
	 * @param collection
	 *          the collection to filter.
	 * @param property1
	 *          the first property to apply.
	 * @param value1
	 *          the value of the first property to apply for this filter.
	 * @param property2
	 *          the second property to apply.
	 * @param value2
	 *          the value of the second property to apply for this filter.
	 */
	public Filter(String collection, String property1, String value1, String property2, String value2) {
		type = "bubblefilter";
		this.collection = collection;
		properties = new ArrayList<Entry<String, String>>();
		properties.add(new SimpleEntry<String, String>(property1, value1));
		properties.add(new SimpleEntry<String, String>(property2, value2));
	}

	public String getDescriminator() {
		return descriminator;
	}

	public void setDescriminator(String id) {
		descriminator = id;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}


	public String getType() {
		return type;
	}

	/**
	 * 
	 * @param index		which property to return
	 * 					bubble filter can only have index 0 or 1
	 * @return
	 */
	public String getBubbleProperty(int index) {
		if(type.equals("null") || (index > 1)) {
			return null;
		}
		return properties.get(index).getKey();
	}

	/**
	 * 
	 * @param index		which value to return
	 * 					bubble filter can only have index 0 or 1
	 * @return
	 */
	public String getBubbleValue(int index) {
		if(type.equals("null") || (index > 1)) {
			return null;
		}
		return properties.get(index).getValue();
	}

	public void setBubbleValue(int index, String value) {
		properties.get(index).setValue(value);
	}


	public DBObject getDocument() {
		if(type == null) {
			return null;
		}
		final BasicDBObject filter = new BasicDBObject();
		filter.put("type", type);
		filter.put("descriminator", descriminator);
		filter.put("collection", collection);
		filter.put("filterID", bubbleFilterID);

		if(type.equals("null")) {
			filter.put("property", property);
			filter.put("value", value);
		}
		else {
			for(int i=0; i<properties.size(); i++) {
				filter.put("property" + i, properties.get(i).getKey());
				filter.put("value" + i, properties.get(i).getValue());
			}
		}

		return filter;
	}


	public String getBubbleFilterID() {
		return bubbleFilterID;
	}


	public void setBubbleFilterID(String bubbleFilterID) {
		this.bubbleFilterID = bubbleFilterID;
	}

}
