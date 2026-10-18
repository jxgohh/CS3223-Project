package simpledb.plan;

import java.io.File;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class StudentDBTest {
   public static void main(String[] args) {
      deleteDir(new File("studenttest"));   // a separate copy; the real studentdb is not touched
      SimpleDB db = new SimpleDB("studenttest");
      Transaction tx = db.newTx();
      Planner planner = db.planner();
      createStudentDB(planner, tx);

      // ---------------- Lab 3: ORDER BY ----------------

      /*
       * Test 1: the query from the Lab 3 brief.
       * Expected:
       * 8 pat 2019
       * 6 kim 2020
       * 5 bob 2020
       * 2 amy 2020
       * 9 lee 2021
       * 1 joe 2021
       * 7 art 2021
       * 4 sue 2022
       * 3 max 2022
       */
      runQuery(planner, tx, "LAB 3 TEST 1: order by GradYear asc, SName desc",
         "select SId, SName, GradYear from student order by GradYear asc, SName desc",
         "sid", "sname", "gradyear");

      /*
       * Test 2: sort on a field that is not selected (MajorId).
       * Expected: art bob | amy kim pat sue | joe lee max
       * (majors 30, 20, 10; names ascending within each major)
       */
      runQuery(planner, tx, "LAB 3 TEST 2: order by a field that is not selected",
         "select SName from student order by MajorId desc, SName asc",
         "sname");

      // ---------------- Lab 4: joins ----------------

      /*
       * Test 3: equality join, no index (STUDENT.MajorId = DEPT.DId).
       * lab04: Merge Join.  lab5 and later: Hash Join.
       * Expected 9 rows (any order):
       * joe compsci, max compsci, lee compsci, amy math, sue math,
       * kim math, pat math, bob drama, art drama
       */
      runQuery(planner, tx, "LAB 4 TEST 3: equality join without an index",
         "select SName, DName from student, dept where MajorId = DId",
         "sname", "dname");

      /*
       * Test 4: equality join with an index (STUDENT.SId = ENROLL.StudentId).
       * Expected plan: Index Join.
       * Expected 6 rows (any order):
       * joe A, joe C, amy B+, sue B, sue A, kim A
       */
      runQuery(planner, tx, "LAB 4 TEST 4: index join",
         "select SName, Grade from student, enroll where SId = StudentId",
         "sname", "grade");

      /*
       * Test 5: non-equality join from the Lab 1 brief (SId < StudentId).
       * Expected plan: Nested Loop Join.
       * Expected 12 rows (any order):
       * joe 2, joe 4, joe 4, joe 6, amy 4, amy 4, amy 6,
       * max 4, max 4, max 6, sue 6, bob 6
       */
      runQuery(planner, tx, "LAB 4 TEST 5: nested loop join",
         "select SName, StudentId from student, enroll where SId < StudentId",
         "sname", "studentid");

      tx.commit();
   }
   
   /** Builds the same tables and rows as CreateStudentDB, plus two indexes. */
   private static void createStudentDB(Planner planner, Transaction tx) {
      planner.executeUpdate("create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)", tx);
      planner.executeUpdate("create table DEPT(DId int, DName varchar(8))", tx);
      planner.executeUpdate("create table COURSE(CId int, Title varchar(20), DeptId int)", tx);
      planner.executeUpdate("create table SECTION(SectId int, CourseId int, Prof varchar(8), YearOffered int)", tx);
      planner.executeUpdate("create table ENROLL(EId int, StudentId int, SectionId int, Grade varchar(2))", tx);

      // indexes must be created BEFORE the inserts, so the inserts add entries to them
      planner.executeUpdate("create index using btree idxsid on STUDENT(SId)", tx);
      planner.executeUpdate("create index using hash idxstudentid on ENROLL(StudentId)", tx);

      String s = "insert into STUDENT(SId, SName, MajorId, GradYear) values ";
      String[] studvals = {"(1, 'joe', 10, 2021)", "(2, 'amy', 20, 2020)", "(3, 'max', 10, 2022)",
                           "(4, 'sue', 20, 2022)", "(5, 'bob', 30, 2020)", "(6, 'kim', 20, 2020)",
                           "(7, 'art', 30, 2021)", "(8, 'pat', 20, 2019)", "(9, 'lee', 10, 2021)"};
      for (String v : studvals)
         planner.executeUpdate(s + v, tx);

      s = "insert into DEPT(DId, DName) values ";
      String[] deptvals = {"(10, 'compsci')", "(20, 'math')", "(30, 'drama')"};
      for (String v : deptvals)
         planner.executeUpdate(s + v, tx);

      s = "insert into COURSE(CId, Title, DeptId) values ";
      String[] coursevals = {"(12, 'db systems', 10)", "(22, 'compilers', 10)", "(32, 'calculus', 20)",
                             "(42, 'algebra', 20)", "(52, 'acting', 30)", "(62, 'elocution', 30)"};
      for (String v : coursevals)
         planner.executeUpdate(s + v, tx);

      s = "insert into SECTION(SectId, CourseId, Prof, YearOffered) values ";
      String[] sectvals = {"(13, 12, 'turing', 2018)", "(23, 12, 'turing', 2019)", "(33, 32, 'newton', 2019)",
                           "(43, 32, 'einstein', 2017)", "(53, 62, 'brando', 2018)"};
      for (String v : sectvals)
         planner.executeUpdate(s + v, tx);

      s = "insert into ENROLL(EId, StudentId, SectionId, Grade) values ";
      String[] enrollvals = {"(14, 1, 13, 'A')", "(24, 1, 43, 'C')", "(34, 2, 43, 'B+')",
                             "(44, 4, 33, 'B')", "(54, 4, 53, 'A')", "(64, 6, 53, 'A')"};
      for (String v : enrollvals)
         planner.executeUpdate(s + v, tx);
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
