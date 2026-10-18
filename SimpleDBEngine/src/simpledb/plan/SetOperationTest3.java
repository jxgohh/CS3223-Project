package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

import java.io.File;

import simpledb.query.Scan;

public class SetOperationTest3 {
   public static void main(String[] args) {
	  deleteDir(new File("setexcepttest1"));
      SimpleDB db = new SimpleDB("setexcepttest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate("create table T1(A int, B varchar(9))", tx);
      planner.executeUpdate("create table T2(C int, D varchar(9))", tx);

      planner.executeUpdate("insert into T1(A,B) values(3, 'three')", tx);
      planner.executeUpdate("insert into T1(A,B) values(1, 'one')", tx);
      planner.executeUpdate("insert into T1(A,B) values(2, 'two')", tx);
      planner.executeUpdate("insert into T1(A,B) values(1, 'one')", tx);

      planner.executeUpdate("insert into T2(C,D) values(4, 'four')", tx);
      planner.executeUpdate("insert into T2(C,D) values(2, 'two')", tx);
      planner.executeUpdate("insert into T2(C,D) values(2, 'two')", tx);

      planner.executeUpdate("create table T3(E float)", tx);
      planner.executeUpdate("create table T4(F float)", tx);
      planner.executeUpdate("insert into T3(E) values(1.5)", tx);
      planner.executeUpdate("insert into T3(E) values(-2.25)", tx);
      planner.executeUpdate("insert into T3(E) values(0.0)", tx);
      planner.executeUpdate("insert into T3(E) values(1.5)", tx);
      planner.executeUpdate("insert into T4(F) values(3.75)", tx);
      planner.executeUpdate("insert into T4(F) values(0.0)", tx);
      planner.executeUpdate("insert into T4(F) values(0.0)", tx);

      /*
       * Test 1: rows in T1 but not T2.
       * Expected: 1 one, 3 three
       */
      System.out.println("\nEXCEPT:");
      Plan p = planner.createQueryPlan("select A, B from T1 except select C, D from T2", tx);
      Scan s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + " " + s.getString("b"));
      s.close();

      /*
       * Test 2: float values in T3 but not T4.
       * Expected: -2.25, 1.5
       */
      System.out.println("\nFLOAT EXCEPT:");
      p = planner.createQueryPlan("select E from T3 except select F from T4", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getVal("e").asFloat());
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
