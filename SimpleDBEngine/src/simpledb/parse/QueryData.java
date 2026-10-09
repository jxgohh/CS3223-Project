package simpledb.parse;

import java.util.*;

import simpledb.query.*;
import simpledb.materialize.AggregationFn;

/**
 * Data for the SQL <i>select</i> statement.
 * @author Edward Sciore
 */
public class QueryData {
   private List<String> fields;
   private Collection<String> tables;
   private List<String> orders;
   private Predicate pred;
   private List<String> groupFields;
   private List<AggregationFn> aggFns;
   private boolean distinct;
   
   /**
    * Saves the field and table list and predicate.
    */
   public QueryData(List<String> fields, Collection<String> tables, List<String> orders, Predicate pred,
		   List<String> groupFields, List<AggregationFn> aggFns, boolean distinct) {
      this.fields = fields;
      this.tables = tables;
      this.orders = orders;
      this.pred = pred;
      this.groupFields = groupFields;
      this.aggFns = aggFns;
      this.distinct = distinct;
   }
   
   /**
    * Returns the fields mentioned in the select clause.
    * @return a list of field names
    */
   public List<String> fields() {
      return fields;
   }
   
   /**
    * Returns the tables mentioned in the from clause.
    * @return a collection of table names
    */
   public Collection<String> tables() {
      return tables;
   }
   
   /**
    * Returns the predicate that describes which
    * records should be in the output table.
    * @return the query predicate
    */
   public Predicate pred() {
      return pred;
   }
   
   public List<String> orders() {
	   return orders;
   }
   
   /**
   * Returns the fields mentioned in the group by clause.
   * @return a list of field names (empty if no group by)
   */
   public List<String> groupFields() {
      return groupFields;
   }
   
   /**
   * Returns the aggregate functions mentioned in the select clause.
   * @return a list of aggregation functions (empty if none)
   */
   public List<AggregationFn> aggFns() {
      return aggFns;
   }
   
   /**
    * Returns true if the query needs a group by operator,
    * i.e. it has a group by clause or any aggregate function.
    */
   public boolean hasGroupBy() {
      return !groupFields.isEmpty() || !aggFns.isEmpty();
   }
   
   /**
    * Returns true if the query has the distinct keyword.
    */
   public boolean isDistinct() {
      return distinct;
   }
   
   public String toString() {
      String result = "select ";
      for (String fldname : fields)
         result += fldname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      result += " from ";
      for (String tblname : tables)
         result += tblname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      String predstring = pred.toString();
      if (!predstring.equals(""))
         result += " where " + predstring;
      if (!orders.isEmpty()) {
    	  result += " order by ";
    	  for (String ordernames : orders)
    		  result += ordernames + ", ";
    	  result = result.substring(0, result.length()-2); //remove final comma
      }
      return result;
   }
}
