package simpledb.materialize;

import static java.sql.Types.VARCHAR;
import java.util.ArrayList;
import java.util.List;
import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.tx.Transaction;

/** A set operation over two inputs, each sorted and made distinct first. */
public class SetOperationPlan implements Plan {
   private Plan left;
   private Plan right;
   private String operation;
   private List<String> leftFields;
   private List<String> rightFields;
   private Schema sch = new Schema();

   public SetOperationPlan(Transaction tx, Plan left, Plan right, String operation) {
      if (!"union".equals(operation) && !"intersect".equals(operation)
            && !"except".equals(operation))
         throw new IllegalArgumentException("Unknown set operation: " + operation);
      this.operation = operation;
      Schema leftSchema = left.schema();
      Schema rightSchema = right.schema();
      leftFields = new ArrayList<>(leftSchema.fields());
      rightFields = new ArrayList<>(rightSchema.fields());
      if (leftFields.size() != rightFields.size())
         throw new IllegalArgumentException("Set queries must select the same number of columns");

      for (int i = 0; i < leftFields.size(); i++) {
         String leftField = leftFields.get(i);
         String rightField = rightFields.get(i);
         int type = leftSchema.type(leftField);
         if (type != rightSchema.type(rightField))
            throw new IllegalArgumentException("Set query column " + (i + 1)
                  + " must have matching types");
         int length = type == VARCHAR
               ? Math.max(leftSchema.length(leftField), rightSchema.length(rightField)) : 0;
         sch.addField(leftField, type, length);
      }

      // DISTINCT sorts on every output column, so no additional sort is needed.
      this.left = new DistinctPlan(tx, left, leftFields);
      this.right = new DistinctPlan(tx, right, rightFields);
   }

   public Scan open() {
      Scan leftScan = left.open();
      Scan rightScan = null;
      try {
         rightScan = right.open();
         return new SetOperationScan(leftScan, rightScan, leftFields, rightFields, operation);
      }
      catch (RuntimeException e) {
         try {
            leftScan.close();
         }
         finally {
            if (rightScan != null)
               rightScan.close();
         }
         throw e;
      }
   }

   public int blocksAccessed() {
      return left.blocksAccessed() + right.blocksAccessed();
   }

   public int recordsOutput() {
      if (operation.equals("union"))
         return left.recordsOutput() + right.recordsOutput();
      if (operation.equals("intersect"))
         return Math.min(left.recordsOutput(), right.recordsOutput());
      return left.recordsOutput();
   }

   public int distinctValues(String fldname) {
      int position = leftFields.indexOf(fldname);
      int leftValues = left.distinctValues(fldname);
      int rightValues = right.distinctValues(rightFields.get(position));
      int estimate = leftValues;
      if (operation.equals("union"))
         estimate = leftValues + rightValues;
      else if (operation.equals("intersect"))
         estimate = Math.min(leftValues, rightValues);
      return Math.min(estimate, recordsOutput());
   }

   public Schema schema() {
      return sch;
   }

   public String toString() {
      return operation.toUpperCase() + " (sort-based)\n" + Plan.indent(left.toString())
            + "\n" + Plan.indent(right.toString());
   }
}
