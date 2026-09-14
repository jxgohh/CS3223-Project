package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.query.Scan;

public class PlannerTest5 {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("plannertest8");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      // Create two tables with different join-field names.
      planner.executeUpdate(
         "create table T1(A int, B varchar(9))", tx);

      planner.executeUpdate(
         "create table T2(C int, D varchar(9))", tx);

      // Insert records into T1.
      planner.executeUpdate(
         "insert into T1(A,B) values(1, 'one')", tx);
      planner.executeUpdate(
         "insert into T1(A,B) values(3, 'three')", tx);
      planner.executeUpdate(
         "insert into T1(A,B) values(5, 'five')", tx);

      // Insert records into T2.
      planner.executeUpdate(
         "insert into T2(C,D) values(2, 'two')", tx);
      planner.executeUpdate(
         "insert into T2(C,D) values(3, 'three')", tx);
      planner.executeUpdate(
         "insert into T2(C,D) values(5, 'five')", tx);
      planner.executeUpdate(
         "insert into T2(C,D) values(7, 'seven')", tx);
      planner.executeUpdate(
         "insert into T2(C,D) values(9, 'nine')", tx);

      /*
       * Test 1: equality join without an index.
       * This should use sort-merge join.
       */
//      System.out.println("SORT-MERGE JOIN:");
//
//      runQuery(
//         planner,
//         tx,
//         "select A, B, C, D from T1, T2 where A = C"
//      );
//
//      /*
//       * Create an index on T2.C.
//       */
      planner.executeUpdate("create index using hash idxC on T2(C)", tx);
//
//      /*
//       * Test 2: equality join with an index.
//       * This should use index join.
//       * TablePlanner should print:
//       * index on c used
//       */
//      System.out.println("\nINDEX JOIN:");
//
//      runQuery(
//         planner,
//         tx,
//         "select A, B, C, D from T1, T2 where A = C"
//      );

      /*
       * Test 3: inequality join.
       * This should use nested-loops join.
       */
      System.out.println("\nNESTED-LOOPS JOIN:");

      runQuery(
         planner,
         tx,
         "select A, B, C, D from T1, T2 where A = C"
      );

      tx.commit();
   }

   private static void runQuery(
         Planner planner,
         Transaction tx,
         String qry) {

      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();

      while (s.next()) {
         System.out.println(
            s.getInt("a") + " " +
            s.getString("b") + " | " +
            s.getInt("c") + " " +
            s.getString("d")
         );
      }

      s.close();
   }
}