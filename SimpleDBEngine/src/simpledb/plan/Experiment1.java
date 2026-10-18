package simpledb.plan;

import java.io.File;

import simpledb.index.planner.IndexJoinPlan;
import simpledb.materialize.HashJoinPlan;
import simpledb.materialize.MergeJoinPlan;
import simpledb.metadata.MetadataMgr;
import simpledb.query.Expression;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.query.Term;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Experiment 1: time a 2-table join (STUDENT join ENROLL on SId = StudentId)
 * under each join algorithm. Run CreateBigStudentDB first.
 *
 * The four join plans are built directly (not through the optimizer),
 * so every algorithm joins the same tables in the same order:
 * STUDENT is the outer (left) input and ENROLL is the inner (right) input.
 */
public class Experiment1 {
   private static final int WARMUP = 1;    // runs not counted
   private static final int RUNS = 5;      // timed runs

   /** Builds one join plan, inside the given transaction. */
   interface PlanMaker {
      Plan make(Transaction tx, MetadataMgr mdm);
   }

   public static void main(String[] args) {
      if (!new File(CreateBigStudentDB.DB).exists()) {
         System.out.println("Run CreateBigStudentDB first.");
         return;
      }
      SimpleDB db = new SimpleDB(CreateBigStudentDB.DB);
      MetadataMgr mdm = db.mdMgr();

      String[] names = {"Nested loop join", "Sort-merge join", "Hash join", "Index join"};
      PlanMaker[] makers = {
         (tx, md) -> new NestedLoopJoinPlan(table(tx, md, "student"), table(tx, md, "enroll"),
               new Predicate(new Term(new Expression("sid"), "=", new Expression("studentid")))),
         (tx, md) -> new MergeJoinPlan(tx, table(tx, md, "student"), table(tx, md, "enroll"),
               "sid", "studentid"),
         (tx, md) -> new HashJoinPlan(tx, table(tx, md, "student"), table(tx, md, "enroll"),
               "sid", "studentid"),
         (tx, md) -> new IndexJoinPlan(table(tx, md, "student"), table(tx, md, "enroll"),
               md.getIndexInfo("enroll", tx).get("studentid"), "sid")
      };

      long[][] times = new long[names.length][RUNS];
      int[] rows = new int[names.length];

      for (int i = 0; i < names.length; i++) {
         System.out.println("\n==================================================");
         System.out.println(names[i]);
         for (int r = -WARMUP; r < RUNS; r++) {
            Transaction tx = db.newTx();
            Plan p = makers[i].make(tx, mdm);
            if (r == -WARMUP)
               System.out.println("Plan:\n" + p);
            long start = System.nanoTime();
            Scan s = p.open();
            int count = 0;
            while (s.next())
               count++;
            s.close();
            long ms = (System.nanoTime() - start) / 1_000_000;
            tx.commit();
            rows[i] = count;
            if (r < 0)
               System.out.println("warm-up: " + ms + " ms, " + count + " rows");
            else {
               times[i][r] = ms;
               System.out.println("run " + (r + 1) + ": " + ms + " ms, " + count + " rows");
            }
         }
      }

      System.out.println("\n==================================================");
      System.out.println("EXPERIMENT 1 RESULTS (" + RUNS + " timed runs each, after " + WARMUP + " warm-up)");
      System.out.printf("%-18s %9s %9s %9s %8s%n", "Algorithm", "Avg ms", "Min ms", "Max ms", "Rows");
      for (int i = 0; i < names.length; i++) {
         long sum = 0, min = Long.MAX_VALUE, max = 0;
         for (long t : times[i]) {
            sum += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
         }
         System.out.printf("%-18s %9.1f %9d %9d %8d%n",
               names[i], (double) sum / RUNS, min, max, rows[i]);
      }
      boolean sameRows = true;
      for (int c : rows)
         sameRows = sameRows && (c == rows[0]);
      System.out.println(sameRows ? "Row check: all algorithms returned the same number of rows"
                                  : "Row check: FAILED, algorithms returned different row counts");
   }

   private static Plan table(Transaction tx, MetadataMgr mdm, String name) {
      return new TablePlan(tx, name, mdm);
   }
}