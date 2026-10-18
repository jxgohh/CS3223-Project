package simpledb.plan;

import java.io.File;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest5 {
   public static void main(String[] args) {
      deleteDir(new File("plannertest5"));   // start from an empty database every run
      SimpleDB db = new SimpleDB("plannertest5");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      // T1 and T2: no indexes
      planner.executeUpdate("create table T1(A int, B varchar(9))", tx);
      planner.executeUpdate("create table T2(C int, D varchar(9))", tx);

      // T3: same data as T2, with an index created BEFORE the inserts
      planner.executeUpdate("create table T3(E int, F varchar(9))", tx);
      planner.executeUpdate("create index using hash idxE on T3(E)", tx);

      String[] t1 = {"(1, 'one')", "(3, 'three')", "(5, 'five')"};
      String[] t2 = {"(2, 'two')", "(3, 'three')", "(5, 'five')", "(7, 'seven')", "(9, 'nine')"};
      for (String v : t1)
         planner.executeUpdate("insert into T1(A,B) values" + v, tx);
      for (String v : t2) {
         planner.executeUpdate("insert into T2(C,D) values" + v, tx);
         planner.executeUpdate("insert into T3(E,F) values" + v, tx);
      }

      /*
       * Test 1: equality join, no index.
       * lab04 branch: Merge Join.  lab5 and later: Hash Join (tried first).
       * Expected 2 rows: 3 three | 3 three, 5 five | 5 five
       */
      runQuery(planner, tx, "TEST 1: equality join, no index",
         "select A, B, C, D from T1, T2 where A = C",
         "a", "b", "c", "d");

      /*
       * Test 2: equality join on an indexed field.
       * Expected plan: Index Join.
       * Expected 2 rows: 3 three | 3 three, 5 five | 5 five
       */
      runQuery(planner, tx, "TEST 2: index join",
         "select A, B, E, F from T1, T3 where A = E",
         "a", "b", "e", "f");

      /*
       * Test 3: non-equality join.
       * Expected plan: Nested Loop Join.
       * Expected 10 rows:
       * 1 one with 2, 3, 5, 7, 9
       * 3 three with 5, 7, 9
       * 5 five with 7, 9
       */
      runQuery(planner, tx, "TEST 3: nested loop join",
         "select A, B, C, D from T1, T2 where A < C",
         "a", "b", "c", "d");

      tx.commit();
   }

   private static void runQuery(Planner planner, Transaction tx,
                                String title, String qry, String... fields) {
      System.out.println("\n==================================================");
      System.out.println(title);
      System.out.println(qry);
      Plan p = planner.createQueryPlan(qry, tx);   // prints the query plan
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