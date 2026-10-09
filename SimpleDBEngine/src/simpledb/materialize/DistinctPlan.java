package simpledb.materialize;

import java.util.*;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.query.*;
import simpledb.record.*;

/**
 * The Plan class for the <i>distinct</i> operator.
 * Sort-based duplicate elimination: the input is sorted on all
 * the distinct fields, and each run of identical records is
 * output once. Implemented as a group by on all the fields
 * with no aggregation functions.
 */
public class DistinctPlan implements Plan {
   private Plan p;
   private List<String> fields;
   private GroupByPlan gbplan;
   
   /**
    * @param tx the calling transaction
    * @param p the underlying plan
    * @param fields the fields that must be distinct (the select list)
    */
   public DistinctPlan(Transaction tx, Plan p, List<String> fields) {
      this.p = p;
      this.fields = fields;
      this.gbplan = new GroupByPlan(tx, p, fields, new ArrayList<AggregationFn>());
   }
   
   public Scan open() {
      return gbplan.open();
   }
   
   public int blocksAccessed() {
      return gbplan.blocksAccessed();
   }
   
   /**
    * Number of distinct combinations, but never more
    * than the number of input records.
    */
   public int recordsOutput() {
      return Math.min(gbplan.recordsOutput(), p.recordsOutput());
   }
   
   public int distinctValues(String fldname) {
      return gbplan.distinctValues(fldname);
   }
   
   public Schema schema() {
      return gbplan.schema();
   }
   
   public String toString() {
      return "Distinct (sort-based) on " + fields + "\n" + Plan.indent(p.toString());
   }
}