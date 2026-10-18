package simpledb.plan;

import java.io.File;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest1 {
   public static void main(String[] args) {
      deleteDir(new File("plannertest1"));
      SimpleDB db = new SimpleDB("plannertest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate("create table T1(A int, B varchar(9))", tx);
      String[] vals = {"(3, 'c')", "(1, 'a')", "(2, 'b')"};
      for (String v : vals)
         planner.executeUpdate("insert into T1(A,B) values" + v, tx);

      /*
       * A query with no ORDER BY still works, and rows come out in insert order.
       * Expected: 3: c, 1: a, 2: b
       */
      String qry = "select A, B from T1";
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