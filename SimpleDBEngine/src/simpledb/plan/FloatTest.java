package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

import java.io.File;

import simpledb.query.Scan;

public class FloatTest {
   public static void main(String[] args) {
	  deleteDir(new File("floattest1"));
      SimpleDB db = new SimpleDB("floattest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate("create table T1(A int, B float)", tx);
      planner.executeUpdate("insert into T1(A,B) values(1, 1.5)", tx);
      planner.executeUpdate("insert into T1(A,B) values(2, -2.25)", tx);
      planner.executeUpdate("insert into T1(A,B) values(3, 0.0)", tx);
      planner.executeUpdate("insert into T1(A,B) values(4, 1.5)", tx);

      /*
       * Test 1: insert and read float values.
       * Expected: 1 1.5, 2 -2.25, 3 0.0, 4 1.5
       */
      System.out.println("\nFLOAT VALUES:");
      Plan p = planner.createQueryPlan("select A, B from T1", tx);
      Scan s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + " " + s.getVal("b").asFloat());
      s.close();

      /*
       * Test 2: float predicate.
       * Expected: 1 1.5, 4 1.5
       */
      System.out.println("\nFLOAT PREDICATE:");
      p = planner.createQueryPlan("select A, B from T1 where B > 0.0", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + " " + s.getVal("b").asFloat());
      s.close();

      /*
       * Test 3: sort float values.
       * Expected: -2.25, 0.0, 1.5, 1.5
       */
      System.out.println("\nFLOAT ORDER BY:");
      p = planner.createQueryPlan("select B from T1 order by B", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getVal("b").asFloat());
      s.close();

      /*
       * Test 4: remove duplicate float values.
       * Expected: -2.25, 0.0, 1.5
       */
      System.out.println("\nFLOAT DISTINCT:");
      p = planner.createQueryPlan("select distinct B from T1", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getVal("b").asFloat());
      s.close();

      /*
       * Test 5: update a float value.
       * Expected: 1 3.75
       */
      System.out.println("\nFLOAT UPDATE:");
      planner.executeUpdate("update T1 set B = 3.75 where A = 1", tx);
      p = planner.createQueryPlan("select A, B from T1 where A = 1", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + " " + s.getVal("b").asFloat());
      s.close();
      
      /*
       * Test 6: sum and avg of a float field.
       * After Test 5, B is 3.75, -2.25, 0.0, 1.5
       * Expected: 3.0 0.75
       */
      System.out.println("\nFLOAT SUM / AVG:");
      p = planner.createQueryPlan("select sum(B), avg(B) from T1", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getVal("sumofb") + " " + s.getVal("avgofb"));
      s.close();
      
      /*
       * Test 7: indexes on a float field (btree and hash).
       * Expected: both lookups return 2 rows: 1 1.5 and 4 1.5
       */
      planner.executeUpdate("create table T2(A int, B float)", tx);
      planner.executeUpdate("create table T3(A int, B float)", tx);
      planner.executeUpdate("create index using btree idxb2 on T2(B)", tx);
      planner.executeUpdate("create index using hash idxb3 on T3(B)", tx);
      String[] vals = {"(1, 1.5)", "(2, -2.25)", "(3, 0.0)", "(4, 1.5)"};
      for (String v : vals) {
         planner.executeUpdate("insert into T2(A,B) values" + v, tx);
         planner.executeUpdate("insert into T3(A,B) values" + v, tx);
      }

      System.out.println("\nFLOAT BTREE INDEX:");
      p = planner.createQueryPlan("select A, B from T2 where B = 1.5", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + " " + s.getVal("b").asFloat());
      s.close();

      System.out.println("\nFLOAT HASH INDEX:");
      p = planner.createQueryPlan("select A, B from T3 where B = 1.5", tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getInt("a") + " " + s.getVal("b").asFloat());
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
