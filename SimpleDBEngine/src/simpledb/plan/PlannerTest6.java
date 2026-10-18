package simpledb.plan;

import java.io.File;

import simpledb.parse.BadSyntaxException;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest6 {
   public static void main(String[] args) {
	  deleteDir(new File("aggtest1"));
      SimpleDB db = new SimpleDB("aggtest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate(
         "create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)", tx);

      String ins = "insert into STUDENT(SId, SName, MajorId, GradYear) values ";
      String[] vals = {
         "(1, 'joe', 10, 2021)", "(2, 'amy', 20, 2020)", "(3, 'max', 10, 2022)",
         "(4, 'sue', 20, 2022)", "(5, 'bob', 30, 2020)", "(6, 'kim', 20, 2020)",
         "(7, 'art', 30, 2021)", "(8, 'pat', 20, 2019)", "(9, 'lee', 10, 2021)"};
      for (String v : vals)
         planner.executeUpdate(ins + v, tx);

      /*
       * Test 1: all aggregates, no group by.
       * Expected: 9 2019 2022 18186 2020
       */
      runQuery(planner, tx, "TEST 1: aggregates without group by",
         "select count(sid), min(gradyear), max(gradyear), sum(gradyear), avg(gradyear) from student",
         "countofsid", "minofgradyear", "maxofgradyear", "sumofgradyear", "avgofgradyear");

      /*
       * Test 2: group by with numeric aggregates.
       * Expected:
       * 10 3 6064 2021
       * 20 4 8081 2020
       * 30 2 4041 2020
       */
      runQuery(planner, tx, "TEST 2: group by majorid",
         "select majorid, count(sid), sum(gradyear), avg(gradyear) from student group by majorid",
         "majorid", "countofsid", "sumofgradyear", "avgofgradyear");

      /*
       * Test 3: min/max on a string field.
       * Expected:
       * 10 joe max
       * 20 amy sue
       * 30 art bob
       */
      runQuery(planner, tx, "TEST 3: min/max on strings",
         "select majorid, min(sname), max(sname) from student group by majorid",
         "majorid", "minofsname", "maxofsname");

      /*
       * Test 4: order by an aggregate.
       * Expected:
       * 20 4 sue
       * 10 3 max
       * 30 2 bob
       */
      runQuery(planner, tx, "TEST 4: order by count desc",
         "select majorid, count(sid), max(sname) from student group by majorid order by count(sid) desc",
         "majorid", "countofsid", "maxofsname");

      /*
       * Test 5: where + group by.
       * Expected:
       * 10 3
       * 20 1
       * 30 1
       */
      runQuery(planner, tx, "TEST 5: where + group by",
         "select majorid, count(sid) from student where gradyear >= 2021 group by majorid",
         "majorid", "countofsid");

      /*
       * Test 6: invalid query (sname not in group by).
       * Expected: "BadSyntaxException caught (correct)"
       */
      System.out.println("\nTEST 6: invalid group by");
      try {
         planner.createQueryPlan("select sname, count(sid) from student group by majorid", tx);
         System.out.println("ERROR: query was accepted");
      }
      catch (BadSyntaxException e) {
         System.out.println("BadSyntaxException caught (correct)");
      }

      tx.commit();
   }

   private static void runQuery(Planner planner, Transaction tx,
                                String title, String qry, String... fields) {
      System.out.println("\n" + title);
      System.out.println(qry);
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      while (s.next()) {
         String row = "";
         for (String f : fields)
            row += s.getVal(f) + " ";
         System.out.println(row);
      }
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