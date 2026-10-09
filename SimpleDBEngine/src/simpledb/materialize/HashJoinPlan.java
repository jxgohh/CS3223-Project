package simpledb.materialize;

import java.util.*;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.query.*;
import simpledb.record.*;

/**
 * The Plan class for the partition-based (Grace) <i>hashjoin</i> operator.
 */
public class HashJoinPlan implements Plan {
   private Transaction tx;
   private Plan p1, p2;
   private String fldname1, fldname2;
   private Schema sch = new Schema();
   
   /**
    * Creates a hashjoin plan for the two specified queries.
    * @param tx the calling transaction
    * @param p1 the LHS (probe) query plan
    * @param p2 the RHS (build) query plan
    * @param fldname1 the LHS join field
    * @param fldname2 the RHS join field
    */
   public HashJoinPlan(Transaction tx, Plan p1, Plan p2, String fldname1, String fldname2) {
      this.tx = tx;
      this.p1 = p1;
      this.p2 = p2;
      this.fldname1 = fldname1;
      this.fldname2 = fldname2;
      sch.addAll(p1.schema());
      sch.addAll(p2.schema());
   }
   
   /**
    * Partitioning phase: hash both inputs into k temp tables each,
    * using the same hash function on the join field.
    * k is limited by the available buffers (one output buffer per partition).
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan src1 = p1.open();
      int k = Math.max(1, tx.availableBuffs() - 1);
      List<TempTable> parts1 = partition(src1, p1.schema(), fldname1, k);
      src1.close();
      
      Scan src2 = p2.open();
      List<TempTable> parts2 = partition(src2, p2.schema(), fldname2, k);
      src2.close();
      
      return new HashJoinScan(parts1, parts2, fldname1, fldname2, p1.schema(), p2.schema());
   }
   
   /**
    * Partitioning reads and writes each input once,
    * and probing reads each partition once.
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return 3 * (p1.blocksAccessed() + p2.blocksAccessed());
   }
   
   /**
    * Return the number of records in the join.
    * Assuming uniform distribution, the formula is:
    * <pre> R(join(p1,p2)) = R(p1)*R(p2)/max{V(p1,F1),V(p2,F2)}</pre>
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      int maxvals = Math.max(p1.distinctValues(fldname1),
                             p2.distinctValues(fldname2));
      return (p1.recordsOutput() * p2.recordsOutput()) / maxvals;
   }
   
   /**
    * The join does not change field values, so the estimate
    * is the same as in the appropriate underlying query.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }
   
   /**
    * The schema of the join is the union of the two input schemas.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return sch;
   }
   
   /**
    * Copies every record of src into one of k temp tables,
    * chosen by hashing the join field.
    */
   private List<TempTable> partition(Scan src, Schema srcsch, String joinfld, int k) {
      List<TempTable> parts = new ArrayList<>();
      List<UpdateScan> dests = new ArrayList<>();
      for (int i = 0; i < k; i++) {
         TempTable t = new TempTable(tx, srcsch);
         parts.add(t);
         dests.add(t.open());
      }
      src.beforeFirst();
      while (src.next()) {
         int h = Math.floorMod(src.getVal(joinfld).hashCode(), k);
         UpdateScan dest = dests.get(h);
         dest.insert();
         for (String fldname : srcsch.fields())
            dest.setVal(fldname, src.getVal(fldname));
      }
      for (UpdateScan dest : dests)
         dest.close();
      return parts;
   }
}