package simpledb.materialize;

import simpledb.query.*;

/**
 * The <i>avg</i> aggregation function.
 * Only applies to integer fields.
 * Since Constant only supports int and String,
 * the average is computed using integer division.
 */
public class AvgFn implements AggregationFn {
   private String fldname;
   private int sum;
   private int count;
   
   /**
    * Create an avg aggregation function for the specified field.
    * @param fldname the name of the aggregated field
    */
   public AvgFn(String fldname) {
      this.fldname = fldname;
   }
   
   /**
    * Start a new average: the sum is the field value
    * in the current record, and the count is 1.
    * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
    */
   public void processFirst(Scan s) {
      sum = s.getInt(fldname);
      count = 1;
   }
   
   /**
    * Add the field value in the current record to the sum,
    * and increment the count.
    * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
    */
   public void processNext(Scan s) {
      sum += s.getInt(fldname);
      count++;
   }
   
   /**
    * Return the field's name, prepended by "avgof".
    * @see simpledb.materialize.AggregationFn#fieldName()
    */
   public String fieldName() {
      return "avgof" + fldname;
   }
   
   /**
    * Return the name of the field being averaged.
    * @see simpledb.materialize.AggregationFn#sourceField()
    */
   public String sourceField() {
      return fldname;
   }
   
   /**
    * Return the current average (sum / count).
    * @see simpledb.materialize.AggregationFn#value()
    */
   public Constant value() {
      return new Constant(sum / count);
   }
}