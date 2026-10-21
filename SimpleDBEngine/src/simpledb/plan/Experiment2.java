package simpledb.plan;

import java.io.File;
import java.util.EnumSet;

import simpledb.opt.HeuristicQueryPlanner;
import simpledb.opt.HeuristicQueryPlanner.JoinMethod;
import simpledb.parse.Parser;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Experiment 2: time a 4-table join under three optimizer configurations.
 * Run CreateBigStudentDB first, using the same database as Experiment1.
 * Plans are chosen by the optimizer with restricted join methods.
 */
public class Experiment2 {
   private static final int WARMUP = 1;    // runs not counted
   private static final int RUNS = 5;      // timed runs
   private static final String SQL = "select sid, sname, eid, sectid, title, grade "
         + "from student, enroll, section, course "
         + "where sid = studentid and sectionid = sectid and courseid = cid";

   public static void main(String[] args) {
      if (!new File(CreateBigStudentDB.DB).exists()) {
         System.out.println("Run CreateBigStudentDB first.");
         return;
      }
      SimpleDB db = new SimpleDB(CreateBigStudentDB.DB);
      String[] names = {"Nested loop only", "Sort-merge only", "Index/hash"};
      long[][] times = new long[names.length][RUNS];
      int[] rows = new int[names.length];

      System.out.println("Query: " + SQL);
      for (int i = 0; i < names.length; i++) {
         EnumSet<JoinMethod> allowed = i == 0 ? EnumSet.of(JoinMethod.NESTED_LOOP)
               : i == 1 ? EnumSet.of(JoinMethod.MERGE)
               : EnumSet.of(JoinMethod.INDEX, JoinMethod.HASH);
         System.out.println("\n==================================================");
         System.out.println(names[i]);
         for (int r = -WARMUP; r < RUNS; r++) {
            Transaction tx = db.newTx();
            Plan p = new HeuristicQueryPlanner(db.mdMgr(), allowed)
                  .createPlan(new Parser(SQL).query(), tx);
            if (r == -WARMUP)
               System.out.println("Plan:\n" + p);
            long start = System.nanoTime();
            Scan s = p.open();
            int count = 0;
            try {
               while (s.next())
                  count++;
            } finally {
               s.close();
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            tx.commit();
            if (count != 10000)
               throw new IllegalStateException("Expected 10000 rows, got " + count);
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
      System.out.println("EXPERIMENT 2 RESULTS (" + RUNS + " timed runs each, after " + WARMUP + " warm-up)");
      System.out.printf("%-18s %9s %9s %9s %8s%n", "Plan", "Avg ms", "Min ms", "Max ms", "Rows");
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
      System.out.println(sameRows ? "Row check: all plans returned the same number of rows"
                                  : "Row check: FAILED, plans returned different row counts");
   }
}
