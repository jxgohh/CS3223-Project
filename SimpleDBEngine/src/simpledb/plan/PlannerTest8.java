package simpledb.plan;

import java.io.File;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest8 {
   public static void main(String[] args) {
      deleteDir(new File("distincttest1"));   // start from an empty database every run
      SimpleDB db = new SimpleDB("distincttest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate(
         "create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)", tx);
      planner.executeUpdate(
         "create table DEPT(DId int, DName varchar(8))", tx);

      // index must exist BEFORE the inserts so that the inserts populate it
      planner.executeUpdate("create index using btree idxsid on STUDENT(SId)", tx);

      String ins = "insert into STUDENT(SId, SName, MajorId, GradYear) values ";
      String[] vals = {
         "(1, 'joe', 10, 2021)", "(2, 'amy', 20, 2020)", "(3, 'max', 10, 2022)",
         "(4, 'sue', 20, 2022)", "(5, 'bob', 30, 2020)", "(6, 'kim', 20, 2020)",
         "(7, 'art', 30, 2021)", "(8, 'pat', 20, 2019)", "(9, 'lee', 10, 2021)"};
      for (String v : vals)
         planner.executeUpdate(ins + v, tx);

      planner.executeUpdate("insert into DEPT(DId, DName) values (10, 'compsci')", tx);
      planner.executeUpdate("insert into DEPT(DId, DName) values (20, 'math')", tx);
      planner.executeUpdate("insert into DEPT(DId, DName) values (30, 'drama')", tx);

      /*
       * Test 1: distinct on one field.
       * Expected rows: 10, 20, 30  (3 rows)
       * Expected plan:
       * Project: [majorid]
       *   Distinct (sort-based) on [majorid]
       *     Table: student
       */
      runQuery(planner, tx, "TEST 1: distinct majorid",
         "select distinct majorid from student",
         "majorid");

      /*
       * Test 2: without distinct, duplicates are kept.
       * Expected: 9 rows
       */
      runQuery(planner, tx, "TEST 2: no distinct (duplicates kept)",
         "select majorid from student",
         "majorid");

      /*
       * Test 3: distinct on two fields.
       * Expected (7 rows):
       * 10 2021, 10 2022, 20 2019, 20 2020, 20 2022, 30 2020, 30 2021
       */
      runQuery(planner, tx, "TEST 3: distinct majorid, gradyear",
         "select distinct majorid, gradyear from student",
         "majorid", "gradyear");

      /*
       * Test 4: distinct + order by.
       * Expected rows: 2022, 2021, 2020, 2019
       * Expected plan:
       * Project: [gradyear]
       *   Sort: [gradyear desc]
       *     Distinct (sort-based) on [gradyear]
       *       Table: student
       */
      runQuery(planner, tx, "TEST 4: distinct + order by desc",
         "select distinct gradyear from student order by gradyear desc",
         "gradyear");

      /*
       * Test 5: distinct on a string field.
       * Expected rows: art, amy, bob, joe, kim, lee, max, pat, sue (9 rows, alphabetical)
       */
      runQuery(planner, tx, "TEST 5: distinct sname (no duplicates exist)",
         "select distinct sname from student",
         "sname");

      /*
       * Test 6: distinct over a group by (distinct aggregate values).
       * Counts per major are 3, 4, 2 -> distinct sorted: 2, 3, 4
       * Expected plan:
       * Project: [countofsid]
       *   Distinct (sort-based) on [countofsid]
       *     Group By (sort-based): [majorid] aggregates: [countofsid]
       *       Sort: [majorid]
       *         Table: student
       */
      runQuery(planner, tx, "TEST 6: distinct + group by",
         "select distinct count(sid) from student group by majorid",
         "countofsid");
      /*
       * Test 7: distinct + join + where.
       * Students with gradyear >= 2021: joe(10) max(10) sue(20) art(30) lee(10)
       * Expected rows: compsci, drama, math (3 rows)
       * Expected plan (join order may differ):
       * Project: [dname]
       *   Distinct (sort-based) on [dname]
       *     Select: majorid=did
       *       Hash Join: majorid = did
       *         Select: gradyear>=2021
       *           Table: student
       *         Table: dept
       */
      runQuery(planner, tx, "TEST 7: distinct + join + where",
         "select distinct dname from student, dept where majorid = did and gradyear >= 2021",
         "dname");

      /*
       * Test 8: plan display of an index select.
       * Expected row: max
       * Expected plan:
       * Project: [sname]
       *   Select: sid=3
       *     Index Select: btree index idxsid on sid = 3
       *       Table: student
       */
      runQuery(planner, tx, "TEST 8: index select plan display",
         "select sname from student where sid = 3",
         "sname");

      tx.commit();
   }

   private static void runQuery(Planner planner, Transaction tx,
                                String title, String qry, String... fields) {
      System.out.println("\n==================================================");
      System.out.println(title);
      System.out.println(qry);
      Plan p = planner.createQueryPlan(qry, tx);   // prints "Query plan: ..."
      System.out.println("Result:");
      Scan s = p.open();
      int count = 0;
      while (s.next()) {
         String row = "";
         for (String f : fields)
            row += s.getVal(f) + " ";
         System.out.println(row);
         count++;
      }
      System.out.println("(" + count + " rows)");
      s.close();
   }

   private static void deleteDir(File dir) {
      File[] files = dir.listFiles();
      if (files != null)
         for (File f : files)
            f.delete();
      dir.delete();
   }
}