package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.query.Scan;

public class FloatTest {
   public static void main(String[] args) {
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

      tx.commit();
   }
}
