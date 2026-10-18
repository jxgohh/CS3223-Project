package simpledb.materialize;

import simpledb.query.*;

/**
 * The <i>sum</i> aggregation function.
 * Only applies to integer fields.
 */
public class SumFn implements AggregationFn {
   private String fldname;
   private int sum;
   private float fsum;
   private boolean isFloat;
   
  /**
    * Create a sum aggregation function for the specified field.
    * @param fldname the name of the aggregated field
    */
   public SumFn(String fldname) {
      this.fldname = fldname;
   }
   
   /**
    * Start a new sum with the field value
    * in the current record.
    * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
    */
   public void processFirst(Scan s) {
	  Constant val = s.getVal(fldname);
	  isFloat = val.isFloat();
	  if (isFloat) {
		  fsum = val.asFloat();
	  } else {
		  sum = val.asInt();
	  }
   }
   
   /**
    * Add the field value in the current record to the sum.
    * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
    */
   public void processNext(Scan s) {
	  Constant val = s.getVal(fldname);
	  if (isFloat) {
		  fsum += val.asFloat();
	  } else {
		  sum += val.asInt();
	  }
   }
   
   /**
    * Return the field's name, prepended by "sumof".
    * @see simpledb.materialize.AggregationFn#fieldName()
    */
   public String fieldName() {
      return "sumof" + fldname;
   }
   
   /**
    * Return the name of the field being summed.
    * @see simpledb.materialize.AggregationFn#sourceField()
    */
   public String sourceField() {
      return fldname;
   }
   
   /**
    * Return the current sum.
    * @see simpledb.materialize.AggregationFn#value()
    */
   public Constant value() {
	   if (isFloat) {
		   return new Constant(fsum);
	   } else {
		   return new Constant(sum);
	   }
   }
}