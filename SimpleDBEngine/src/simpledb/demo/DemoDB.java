package simpledb.demo;

import java.io.File;
import java.util.EnumSet;

import simpledb.opt.HeuristicQueryPlanner;
import simpledb.opt.HeuristicQueryPlanner.JoinMethod;
import simpledb.parse.Parser;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Fixed demo database with 3 tables, and helpers shared by all the demo files.
 *
 *   STUDENT(SId, SName, MajorId, GradYear, Gpa)   100 rows, B-tree index on SId
 *   DEPT(DId, DName)                               25 rows
 *   ENROLL(EId, StudentId, Course, Grade)         100 rows, hash index on StudentId
 *
 * The rows come from fixed formulas (no randomness), so the results never change:
 *
 *   STUDENT  sid      = 1..100
 *            sname    = NAMES[(sid-1) % 10] + sid       joe1, amy2, max3, ..., ann10, joe11, ...
 *            majorid  = ((sid*7) % 20 + 1) * 10          10..200: 20 depts with 5 students each
 *            gradyear = 2019 + sid % 4                   2019..2022: 25 students each
 *            gpa      = 2.0 + ((sid*7) % 9) * 0.25       2.0, 2.25, ..., 4.0
 *
 *   DEPT     did      = 10, 20, ..., 250
 *            dname    = SUBJECTS[(i-1) % 10] + ((i-1)/10 + 1)   compsci1, math1, ..., art1, compsci2, ...
 *                       depts 210..250 have no students
 *
 *   ENROLL   eid       = 1..100
 *            studentid = (eid*7) % 75 + 1                students 1..75 only: 76..100 never enroll
 *            course    = COURSES[eid % 5]
 *            grade     = GRADES[eid % 6]
 *
 * The database is rebuilt once each time a demo program starts.
 */
public class DemoDB {
   public static final String DB = "demodb";
   static final String[] NAMES    = {"joe", "amy", "max", "sue", "bob", "kim", "art", "pat", "lee", "ann"};
   static final String[] SUBJECTS = {"compsci", "math", "drama", "physics", "chem",
                                     "bio", "econ", "history", "music", "art"};
   static final String[] COURSES  = {"db", "algebra", "calculus", "acting", "optics"};
   static final String[] GRADES   = {"A", "A-", "B+", "B", "C", "F"};
   static SimpleDB db;
   static Planner planner;

   /** Builds the demo database, once per program run. */
   public static void setup() {
      if (db != null)
         return;           // already built in this run (e.g. by an earlier menu choice)
      deleteDir(new File(DB));
      db = new SimpleDB(DB);
      planner = db.planner();
      Transaction tx = db.newTx();

      planner.executeUpdate("create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int, Gpa float)", tx);
      planner.executeUpdate("create table DEPT(DId int, DName varchar(10))", tx);
      planner.executeUpdate("create table ENROLL(EId int, StudentId int, Course varchar(10), Grade varchar(2))", tx);

      // indexes BEFORE the inserts, so the inserts add entries to them
      planner.executeUpdate("create index using btree idxsid on STUDENT(SId)", tx);
      planner.executeUpdate("create index using hash idxstudentid on ENROLL(StudentId)", tx);
      tx.commit();

      for (int sid = 1; sid <= 100; sid++) {
         String sname = NAMES[(sid - 1) % 10] + sid;
         int majorid = ((sid * 7) % 20 + 1) * 10;
         int gradyear = 2019 + sid % 4;
         float gpa = 2.0f + ((sid * 7) % 9) * 0.25f;
         insert("insert into STUDENT(SId, SName, MajorId, GradYear, Gpa) values ("
               + sid + ", '" + sname + "', " + majorid + ", " + gradyear + ", " + gpa + ")", sid);
      }
      for (int i = 1; i <= 25; i++) {
         String dname = SUBJECTS[(i - 1) % 10] + ((i - 1) / 10 + 1);
         insert("insert into DEPT(DId, DName) values (" + (i * 10) + ", '" + dname + "')", i);
      }
      for (int eid = 1; eid <= 100; eid++) {
         int studentid = (eid * 7) % 75 + 1;
         insert("insert into ENROLL(EId, StudentId, Course, Grade) values (" + eid + ", " + studentid
               + ", '" + COURSES[eid % 5] + "', '" + GRADES[eid % 6] + "')", eid);
      }
      if (batchTx != null)
         batchTx.commit();
      batchTx = null;
   }

   // Commit every 10 inserts: B-tree page splits keep buffers pinned until the
   // transaction ends, so one big transaction runs out of the 8 buffers.
   private static Transaction batchTx;
   private static void insert(String sql, int n) {
      if (batchTx == null)
         batchTx = db.newTx();
      planner.executeUpdate(sql, batchTx);
      if (n % 10 == 0) {
         batchTx.commit();
         batchTx = null;
      }
   }

   /** Prints a heading for a demo. */
   public static void title(String name) {
      System.out.println("\n==================================================");
      System.out.println(name);
      System.out.println("==================================================");
   }

   /** Runs a query through the normal planner, which prints its plan. */
   public static void run(String title, String sql) {
      System.out.println("\n--- " + title);
      System.out.println(sql);
      Transaction tx = db.newTx();
      Plan p = planner.createQueryPlan(sql, tx);   // prints "Query plan: ..."
      printRows(p);
      tx.commit();
   }

   /** Runs a query with the optimizer restricted to the given join methods. */
   public static void runWithJoins(String title, String sql, EnumSet<JoinMethod> joins) {
      System.out.println("\n--- " + title + "   [join methods allowed: " + joins + "]");
      System.out.println(sql);
      Transaction tx = db.newTx();
      Plan p = new HeuristicQueryPlanner(db.mdMgr(), joins).createPlan(new Parser(sql).query(), tx);
      System.out.println("Query plan:\n" + p);
      printRows(p);
      tx.commit();
   }

   /** Runs a query that should be rejected, and prints the error. */
   public static void runExpectError(String title, String sql) {
      System.out.println("\n--- " + title);
      System.out.println(sql);
      Transaction tx = db.newTx();
      try {
         planner.createQueryPlan(sql, tx);
         System.out.println("UNEXPECTED: the query was accepted");
      }
      catch (RuntimeException e) {
         System.out.println("Rejected as expected: " + e.getClass().getSimpleName());
      }
      tx.rollback();
   }

   static void printRows(Plan p) {
      System.out.println("Result:");
      Scan s = p.open();
      int count = 0;
      while (s.next()) {
         String row = "";
         for (String f : p.schema().fields())
            row += s.getVal(f) + "  ";
         System.out.println("  " + row);
         count++;
      }
      s.close();
      System.out.println("(" + count + " rows)");
   }

   private static void deleteDir(File dir) {
      File[] files = dir.listFiles();
      if (files != null)
         for (File f : files)
            f.delete();
      dir.delete();
   }
}
