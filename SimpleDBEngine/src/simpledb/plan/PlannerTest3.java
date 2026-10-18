package simpledb.plan;

import java.io.File;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest3 {
   public static void main(String[] args) {
      deleteDir(new File("plannertest3"));
      SimpleDB db = new SimpleDB("plannertest3");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate("create table T3(A int, B varchar(9))", tx);
      String[] vals = {"(2, 'b')", "(1, 'a')", "(2, 'd')", "(3, 'c')", "(1, 'e')", "(2, 'a')"};
      for (String v : vals)
         planner.executeUpdate("insert into T3(A,B) values" + v, tx);

      /*
       * Two sort fields with mixed directions: A ascending, then B descending for ties.
       * Expected:
       * 1: e
       * 1: a
       * 2: d
       * 2: b
       * 2: a
       * 3: c
       */
      String qry = "select A, B from T3 order by A asc, B desc";
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + ": " + s.getString("b"));
      s.close();
      tx.commit();
   }

   private static void deleteDir(File dir) {
      File[] files = dir.listFiles();
      if (files != null)
         for (File f : files)
            f.delete();
      dir.delete();
   }
}