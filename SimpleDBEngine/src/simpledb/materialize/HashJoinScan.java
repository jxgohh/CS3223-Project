package simpledb.materialize;

import java.util.*;

import simpledb.plan.*;
import simpledb.query.*;
import simpledb.record.*;

/**
 * The Scan class for the <i>hashjoin</i> operator.
 * For each partition i, the RHS partition is loaded into an
 * in-memory hash table and the LHS partition is probed against it.
 */
public class HashJoinScan implements Scan {
   private List<TempTable> parts1, parts2;
   private String fldname1, fldname2;
   private Schema sch1, sch2;
   
   private int currpart;
   private Scan probescan;
   private Map<Constant, List<Map<String, Constant>>> hashtable;
   private Iterator<Map<String, Constant>> matches;
   private Map<String, Constant> currmatch;
   
   /**
    * Create a hashjoin scan over the two lists of partitions.
    * @param parts1 the LHS (probe) partitions
    * @param parts2 the RHS (build) partitions
    * @param fldname1 the LHS join field
    * @param fldname2 the RHS join field
    * @param sch1 the LHS schema
    * @param sch2 the RHS schema
    */
   public HashJoinScan(List<TempTable> parts1, List<TempTable> parts2,
                       String fldname1, String fldname2, Schema sch1, Schema sch2) {
      this.parts1 = parts1;
      this.parts2 = parts2;
      this.fldname1 = fldname1;
      this.fldname2 = fldname2;
      this.sch1 = sch1;
      this.sch2 = sch2;
      beforeFirst();
   }
   
   /**
    * Position the scan before the first partition.
    * @see simpledb.query.Scan#beforeFirst()
    */
   public void beforeFirst() {
      if (probescan != null)
         probescan.close();
      probescan = null;
      currpart = -1;
      hashtable = null;
      matches = null;
      currmatch = null;
   }
   
   /**
    * Move to the next joined record.
    * @see simpledb.query.Scan#next()
    */
   public boolean next() {
      while (true) {
         // 1. more matches for the current probe record?
         if (matches != null && matches.hasNext()) {
            currmatch = matches.next();
            return true;
         }
         // 2. move to the next probe record in this partition
         if (probescan != null && probescan.next()) {
            List<Map<String, Constant>> bucket = hashtable.get(probescan.getVal(fldname1));
            matches = (bucket == null) ? null : bucket.iterator();
            continue;
         }
         // 3. move to the next partition
         if (!loadNextPartition())
            return false;
      }
   }
   
   /**
    * Build an in-memory hash table from the next RHS partition,
    * and open a scan on the matching LHS partition.
    * Returns false if there are no more partitions.
    */
   private boolean loadNextPartition() {
      if (probescan != null)
         probescan.close();
      probescan = null;
      matches = null;
      currpart++;
      if (currpart >= parts1.size())
         return false;
      
      // build: load the RHS partition into memory
      hashtable = new HashMap<>();
      Scan buildscan = parts2.get(currpart).open();
      while (buildscan.next()) {
         Map<String, Constant> row = new HashMap<>();
         for (String fldname : sch2.fields())
            row.put(fldname, buildscan.getVal(fldname));
         hashtable.computeIfAbsent(buildscan.getVal(fldname2), x -> new ArrayList<>()).add(row);
      }
      buildscan.close();
      
      // probe: scan the LHS partition
      probescan = parts1.get(currpart).open();
      return true;
   }
   
   /**
    * LHS fields come from the probe scan,
    * RHS fields come from the current matching row.
    * @see simpledb.query.Scan#getVal(java.lang.String)
    */
   public Constant getVal(String fldname) {
      if (sch1.hasField(fldname))
         return probescan.getVal(fldname);
      else
         return currmatch.get(fldname);
   }
   
   public int getInt(String fldname) {
      return getVal(fldname).asInt();
   }
   
   public String getString(String fldname) {
      return getVal(fldname).asString();
   }
   
   public boolean hasField(String fldname) {
      return sch1.hasField(fldname) || sch2.hasField(fldname);
   }
   
   public void close() {
	   if (probescan != null)
	       probescan.close();
	}
}