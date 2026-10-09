package simpledb.plan;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest7 {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("hashjointest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      // Student / dept tables
      planner.executeUpdate(
         "create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)", tx);
      planner.executeUpdate(
         "create table DEPT(DId int, DName varchar(8))", tx);

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

      // Tables with duplicates and non-matching values
      planner.executeUpdate("create table T1(A int, B varchar(9))", tx);
      planner.executeUpdate("create table T2(C int, D varchar(9))", tx);
      planner.executeUpdate("insert into T1(A,B) values(1, 'one')", tx);
      planner.executeUpdate("insert into T1(A,B) values(3, 'three')", tx);
      planner.executeUpdate("insert into T1(A,B) values(3, 'three2')", tx);
      planner.executeUpdate("insert into T1(A,B) values(5, 'five')", tx);
      planner.executeUpdate("insert into T2(C,D) values(2, 'two')", tx);
      planner.executeUpdate("insert into T2(C,D) values(3, 'three')", tx);
      planner.executeUpdate("insert into T2(C,D) values(5, 'five')", tx);
      planner.executeUpdate("insert into T2(C,D) values(5, 'five2')", tx);
      planner.executeUpdate("insert into T2(C,D) values(7, 'seven')", tx);

      /*
       * Test 1: basic equality join. Should print "Hash join: ..."
       * Expected 9 rows (any order):
       * joe compsci, max compsci, lee compsci,
       * amy math, sue math, kim math, pat math,
       * bob drama, art drama
       */
      runQuery(planner, tx, "TEST 1: student join dept",
         "select sname, dname from student, dept where majorid = did",
         "sname", "dname");

      /*
       * Test 2: duplicates on both sides + values with no match.
       * Expected 4 rows (any order):
       * 3 three | 3 three
       * 3 three2 | 3 three
       * 5 five | 5 five
       * 5 five | 5 five2
       */
      runQuery(planner, tx, "TEST 2: duplicates and non-matches",
         "select A, B, C, D from T1, T2 where A = C",
         "a", "b", "c", "d");

      /*
       * Test 3: hash join + group by.
       * Expected:
       * compsci 3 2021
       * drama 2 2020
       * math 4 2020
       */
      runQuery(planner, tx, "TEST 3: join + group by",
         "select dname, count(sid), avg(gradyear) from student, dept where majorid = did group by dname",
         "dname", "countofsid", "avgofgradyear");

      /*
       * Test 4: non-equality join.
       * Should NOT print "Hash join" (falls back to nested-loops).
       * Expected 12 rows.
       */
      runQuery(planner, tx, "TEST 4: non-equality join (no hash join)",
         "select A, B, C, D from T1, T2 where A < C",
         "a", "b", "c", "d");

      tx.commit();
   }
   
   private static void runQuery(Planner planner, Transaction tx,
           String title, String qry, String... fields) {
		System.out.println("\n" + title);
		System.out.println(qry);
		Plan p = planner.createQueryPlan(qry, tx);
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
}