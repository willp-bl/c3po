package com.petpet.c3po.common;

public final class Constants {

  /**
   * The url for the xml schema property used by the sax parser while validating
   * xml files against their schemata.
   */
  public static final String XML_SCHEMA_PROPERTY = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  /**
   * The url for the xml schema language used by the sax parser while validating
   * xml files against their schemata.
   */
  public static final String XML_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

  /**
   * The version of the generated profile.
   */
  @Deprecated
  public static final String PROFILE_FORMAT_VERSION = "0.1";

  /**
   * The version of the core module.
   */
  public static final String CORE_VERSION = "0.3.0";

  public static final String API_VERSION = "0.3.0";

  /**
   * The elements collection in the document store.
   */
  public static final String TBL_ELEMENTS = "elements";

  /**
   * The properties collection in the document store.
   */
  public static final String TBL_PROEPRTIES = "properties";

  /**
   * The source collection in the document store.
   */
  public static final String TBL_SOURCES = "sources";

  /**
   * The filters stored in the db.
   */
  public static final String TBL_FILTERS = "filters";

  /**
   * The actions done on a collection basis in the db.
   */
  public static final String TBL_ACTIONLOGS = "actionlogs";

  /**
   * A c3po configuration for the collection on which to operate.
   */
  public static final String CNF_COLLECTION_NAME = "c3po.collection.name";

  /**
   * A c3po configuration for the location where the metadata is.
   */
  public static final String CNF_COLLECTION_LOCATION = "c3po.collection.location";
  
  /**
   * A c3po configuration for the type of the input files. 
   * Currently only FITS and TIKA are supported. This config is
   * required for the controller to operate.
   */
  public static final String CNF_INPUT_TYPE = "c3po.input.type";

  /**
   * Experimental property allowing to infer the date of the objects, if their
   * file names have a specific format.
   */
  public static final String CNF_INFER_DATE = "adaptor.inference.date";

  /**
   * A collection identifier configuration for the adaptors.s
   */
  public static final String CNF_COLLECTION_ID = "adaptor.collection.identifier";

  /**
   * A configuartion for recursive processing.
   */
  public static final String CNF_RECURSIVE = "c3po.recursive";

  /**
   * The thread count configuration during meta data harvesting.
   */
  public static final String CNF_THREAD_COUNT = "c3po.thread.count";

  /**
   * The hostname of the server where the db is running.
   */
  public static final String CNF_DB_HOST = "db.host";

  /**
   * The port of the server where the db is listening to.
   */
  public static final String CBF_DB_PORT = "db.port";

  /**
   * The database name.
   */
  public static final String CNF_DB_NAME = "db.name";

  /**
   * A javascript Map function for building a histogram of a specific property.
   * All occurrences of that property are used (if they do not have conflcited
   * values). Note that there is a '{}' wildcard that has to be replaced with
   * the id of the desired property, prior to usage.
   */
  public static final String HISTOGRAM_MAP = "function map() {if (this.metadata['{}'] != null) {if (this.metadata['{}'].status !== 'CONFLICT') {emit(this.metadata['{}'].value, 1);}else{emit('Conflicted', 1);}} else {emit('Unknown', 1);}}";

  /**
   * A javascript Map function for building a histogram over a specific date
   * property. All occurrences of that property are used. If they are conflicted
   * then they are aggregated under one key 'Conflcited'. If the property is
   * missing, then the values are aggregated under the key 'Unknown'. Otherwise
   * the year is used as the key. Note that there is a '{}' wildcard that has to
   * be replaced with the id of the desired property, prior to usage.
   */
  public static final String DATE_HISTOGRAM_MAP = "function () {if (this.metadata['{}'] != null) {if (this.metadata['{}'].status !== 'CONFLICT') {emit(this.metadata['{}'].value.getFullYear(), 1);}else{emit('Conflicted', 1);}}else{emit('Unknown', 1);}}";

  /**
   * A javascript Map function for building a histogram with fixed bin size. It
   * takes two wilde cards as parameters - The {1} is the numeric property and
   * the {2} is the bin size. The result contains the bins, where the id is from
   * 0 to n and the value is the number of occurrences. Note that each bin has a
   * fixed size so the label can be easily calculated. For example the id 0
   * marks the number of elements where the numeric property was between 0 and
   * the width, the id 1 marks the number of elements where the numeric property
   * was between the width and 2*width and so on.
   */
  public static final String NUMERIC_HISTOGRAM_MAP = "function () {if (this.metadata['{1}'] != null) {if (this.metadata['{1}'].status !== 'CONFLICT') {var idx = Math.floor(this.metadata['{1}'].value / {2});emit(idx, 1);} else {emit('Conflicted', 1);}}else{emit('Unknown', 1);}}";

  /**
   * The reduce function for the {@link Constants#HISTOGRAM_MAP}.
   */
  public static final String HISTOGRAM_REDUCE = "function reduce(key, values) {var res = 0;values.forEach(function (v) {res += v;});return res;}";

  
  /**
   * a string delimiter used during the map-reduce for the bubblechart job
   */
  public static final String BUBBLECHART_KEY_SEP = ":";
  
  /**
   * a javascript map function to map values of two given property ids to one
   * value. The emitted key of this map function consists of the two values
   * of the properties, concatenated with a separator 
   * {@link Constants#BUBBLECHART_KEY_SEP}. The emitted value includes the 
   * position of the separator within the key string. This way we know the 
   * exact values of the properties for the emited result without using too
   * much space (this method is also faster, compared to adding the values to 
   * the 'value' object).  
   * 
   * Before using this map function the strings "{value1convert}" and 
   * "{value2convert}" have to be replaced by one of the 
   * BUBBLECHART_MAP_CONVERT_* constants, according to the property type  
   */
  public static final String BUBBLECHART_MAP = "function () {\n" +
  		"  var result = { count: 1,     " +
  		"                 separator: 0 " +	// store the position of the separator 
  		"               };\n " +
		"  var value1 = 'Unknown'; \n" + 
		"  var value2 = 'Unknown'; \n" +
  		"  if (this.metadata['{1}'] != null) {\n" +
  		"    value1 = this.metadata['{1}'].status !== 'CONFLICT' ? \n" +
  		"             ( {value1convert} ) : 'Conflicted';\n" +
		"  } \n" +
  		"  if (this.metadata['{2}'] != null) {\n" +
  		"    value2 = this.metadata['{2}'].status !== 'CONFLICT' ? \n" +
  		"             ( {value2convert} ) : 'Conflicted';\n" +
		"  } \n" +
  		"  var key = value1 + \"" + BUBBLECHART_KEY_SEP + "\" + value2;\n" +
  		"  result.separator = value1.toString().length;\n" +
  		"  emit(key, result);\n" +
//  		"  } else {\n" +
//  		"    result.separator = 'Unknown'.length; \n" + 	
//  		"    emit('Unknown" + BUBBLECHART_KEY_SEP + "Unknown', result);\n" +
//  		"  }\n" +
  		"}";

  /**
   * convert part for the map-reduce of {@link Constants#BUBBLECHART_MAP}
   * function.
   * 
   * before use replace "{1}" with the property id
   */
  public static final String BUBBLECHART_MAP_CONVERT_STRING = 
		  "this.metadata['{1}'].value !== null ? " +
		  "this.metadata['{1}'].value : 'Unknown'" ;
  
  /**
   * convert part for the map-reduce of {@link Constants#BUBBLECHART_MAP}
   * function.
   *
   * use just the year for the date
   * 
   * before use replace "{1}" with the property id
   */
  public static final String BUBBLECHART_MAP_CONVERT_DATE = 
		  "this.metadata['{1}'].value !== null ? " +
		  "this.metadata['{1}'].value.getFullYear().toString() : 'Unknown'" ;
  
  /**
   * convert part for the map-reduce of {@link Constants#BUBBLECHART_MAP}
   * function.
   * 
   * map a numeric value to the bin id
   * 
   * before use replace "{1}" with the property id and "{2}" with the bin width
   */
  public static final String BUBBLECHART_MAP_CONVERT_NUMERIC = 
		  "this.metadata['{1}'].value !== null ? " +
		  "Math.floor(this.metadata['{1}'].value / {2}).toString() : 'Unknown'";
  
  /**
   * this is the reduce function for the bubblechart map-reduce data fetching
   * part. It simply counts the occurrences of value-tuples of the two defined
   * properties.  
   */
  public static final String BUBBLECHART_REDUCE = "function reduce(key, value) {" +
  		"  var sum = 0;" +
  		"  value.forEach(function (v) {" +
  		"       sum += v.count;" +
  		"    });" +
  		"  return { count: sum," +
  		"           separator: value[0].separator " +
  		"         };" +
  		"}";
  
  /**
   * after finishing the map reduce part for the bubblechart, we extract the 
   * values from the key again using the stored separator position. The result
   * after this finalize function looks like this:
   * key = "some-value1:some-value2"
   * value = {  count: x,
   *            separator: 11,
   *            value1: some-value1,
   *            value2: some-value2
   *         } 
   */
  public static final String BUBBLECHART_FINALIZE = "function (key, rv) {" +
  		"  rv.value1 = key.substring(0, rv.separator);" +
  		"  rv.value2 = key.substring(rv.separator + \"" + BUBBLECHART_KEY_SEP + "\".length);" +
  		"  return rv;" +
  		"}"; 
  
  /**
   * A javascript Map function for calculating the min, max, sum, avg, sd and
   * var of a numeric property. Note that there is a wildcard {1} that has to be
   * replaced with the id of the desired numeric property prior to usage.
   */
  public static final String AGGREGATE_MAP = "function map() {emit(1,{sum: this.metadata['{1}'].value, min: this.metadata['{1}'].value,max: this.metadata['{1}'].value,count:1,diff: 0,});}";

  /**
   * The same as {@link Constants#AGGREGATE_MAP} but it aggregates the desired
   * property only for elements where the passed filter has a specific value.
   * {1} - the filter property id (e.g. 'mimetype') {2} - the value of the
   * filter (e.g. 'application/pdf') {3} - the property to aggregate (e.g.
   * 'size')
   */
  public static final String FILTER_AGGREGATE_MAP = "function map() {if (this.metadata['{1}'].value === '{2}') {emit(1,{sum: this.metadata['{3}'].value, min: this.metadata['{3}'].value,max: this.metadata['{3}'].value,count:1,diff: 0,});}}";

  /**
   * The reduce of the aggregation functions.
   */
  public static final String AGGREGATE_REDUCE = "function reduce(key, values) {var a = values[0];for (var i=1; i < values.length; i++){var b = values[i];var delta = a.sum/a.count - b.sum/b.count;var weight = (a.count * b.count)/(a.count + b.count);a.diff += b.diff + delta*delta*weight;a.sum += b.sum;a.count += b.count;a.min = Math.min(a.min, b.min);a.max = Math.max(a.max, b.max);}return a;}";

  /**
   * A finalize function for the aggregation map reduce job, to calculate the
   * average, standard deviation and variance.
   */
  public static final String AGGREGATE_FINALIZE = "function finalize(key, value){ value.avg = value.sum / value.count;value.variance = value.diff / value.count;value.stddev = Math.sqrt(value.variance);return value;}";
  
  
  public static final String PROPERTIES_IN_COLLECTION_MAP = "function map() {for (var key in this) {if (key == 'metadata') {for (var subkey in this[key]) {emit(subkey, null);}}}}";
  
  public static final String PROPERTIES_IN_COLLECTION_REDUCE = "function reduce(key, values) {return null;}";

  // "function reduce(key, values) {var res = {count: 0}; values.forEach(function (v) {res.count += v.count}); return res;}";

  private Constants() {

  }
}
