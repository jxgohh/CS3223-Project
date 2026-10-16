package simpledb.materialize;

import java.util.ArrayList;
import java.util.List;
import simpledb.query.Constant;
import simpledb.query.Scan;

/** Merges two sorted, duplicate-free inputs using positional column matching. */
public class SetOperationScan implements Scan {
   private Scan left;
   private Scan right;
   private List<String> leftFields;
   private List<String> rightFields;
   private String operation;
   private boolean hasLeft;
   private boolean hasRight;
   private List<Constant> currentRow;

   public SetOperationScan(Scan left, Scan right, List<String> leftFields,
         List<String> rightFields, String operation) {
      this.left = left;
      this.right = right;
      this.leftFields = new ArrayList<>(leftFields);
      this.rightFields = new ArrayList<>(rightFields);
      this.operation = operation;
      beforeFirst();
   }

   public void beforeFirst() {
      currentRow = null;
      left.beforeFirst();
      right.beforeFirst();
      hasLeft = left.next();
      hasRight = right.next();
   }

   public boolean next() {
      currentRow = null;
      while (hasLeft || hasRight) {
         if (!hasLeft)
            return operation.equals("union") && takeRight();
         if (!hasRight)
            return !operation.equals("intersect") && takeLeft(false);

         int comparison = compareRows();
         if (operation.equals("union")) {
            // Save row and advance right
            if (comparison > 0)
               return takeRight();
            // if both equal save row and advance both
            if (comparison == 0)
               return takeLeft(true);
            // Save row and advance left
            return takeLeft(false);
         }
         if (operation.equals("intersect")) {
            // Save row and advance both
            if (comparison == 0)
               return takeLeft(true);
            if (comparison < 0)
               hasLeft = left.next();
            else
               hasRight = right.next();
         }
         else { // EXCEPT: skip matches and rows that exist only on the right.
            if (comparison < 0)
               return takeLeft(false);
            if (comparison == 0)
               hasLeft = left.next();
            hasRight = right.next();
         }
      }
      return false;
   }

   private int compareRows() {
      for (int i = 0; i < leftFields.size(); i++) {
         Constant leftValue = left.getVal(leftFields.get(i));
         Constant rightValue = right.getVal(rightFields.get(i));
         int comparison = leftValue.compareTo(rightValue);
         if (comparison != 0)
            return comparison;
      }
      return 0;
   }

   private boolean takeLeft(boolean advanceRight) {
      saveRow(left, leftFields);
      hasLeft = left.next();
      if (advanceRight)
         hasRight = right.next();
      return true;
   }

   private boolean takeRight() {
      saveRow(right, rightFields);
      hasRight = right.next();
      return true;
   }

   private void saveRow(Scan source, List<String> fields) {
      // Keep the output values before advancing either underlying scan.
      currentRow = new ArrayList<>();
      for (String field : fields)
         currentRow.add(source.getVal(field));
   }

   public Constant getVal(String fldname) {
      int position = leftFields.indexOf(fldname);
      if (position < 0)
         throw new IllegalArgumentException("Unknown output field: " + fldname);
      if (currentRow == null)
         throw new IllegalStateException("The scan is not positioned on a result row");
      return currentRow.get(position);
   }

   public int getInt(String fldname) {
      return getVal(fldname).asInt();
   }

   public String getString(String fldname) {
      return getVal(fldname).asString();
   }

   public boolean hasField(String fldname) {
      return leftFields.contains(fldname);
   }

   public void close() {
      try {
         left.close();
      }
      finally {
         right.close();
         currentRow = null;
      }
   }
}
