package simpledb.plan;

import java.io.File;
import java.util.Random;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Builds studentdb_big: the student database schema with many more rows.
 * Run this ONCE. Experiment1 and Experiment2 then open the same database.
 * Running it again deletes the old database and rebuilds identical data.
 */
public class CreateBigStudentDB {
   public static final String DB = "studentdb_big";

   // Commit after this many inserts. SimpleDB's B-tree code keeps one buffer
   // pinned each time an index page splits, until the transaction commits.
   // With only 8 buffers, committing often stops them from running out.
   private static final int BATCH = 10;

   private static SimpleDB db;
   private static Planner planner;
   private static Transaction tx;
   private static int inserts = 0;

   public static void main(String[] args) {
      deleteDir(new File(DB));
      db = new SimpleDB(DB);
      planner = db.planner();
      tx = db.newTx();
      Random rnd = new Random(3223);          // fixed seed: everyone gets identical data

      planner.executeUpdate("create table DEPT(DId int, DName varchar(8))", tx);
      planner.executeUpdate("create table COURSE(CId int, Title varchar(20), DeptId int)", tx);
      planner.executeUpdate("create table SECTION(SectId int, CourseId int, Prof varchar(8), YearOffered int)", tx);
      planner.executeUpdate("create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)", tx);
      planner.executeUpdate("create table ENROLL(EId int, StudentId int, SectionId int, Grade varchar(2))", tx);

      // indexes BEFORE the inserts, so the inserts add entries to them
      planner.executeUpdate("create index using btree idxsid on STUDENT(SId)", tx);
      planner.executeUpdate("create index using hash idxstudentid on ENROLL(StudentId)", tx);
      planner.executeUpdate("create index using hash idxsectid on SECTION(SectId)", tx);
      planner.executeUpdate("create index using btree idxcid on COURSE(CId)", tx);
      tx.commit();
      tx = db.newTx();

      for (int d = 1; d <= 20; d++)
         insert("insert into DEPT(DId, DName) values (" + d + ", 'dept" + d + "')");
      System.out.println("DEPT: 20 rows");

      for (int c = 1; c <= 200; c++)
         insert("insert into COURSE(CId, Title, DeptId) values ("
               + c + ", 'course" + c + "', " + (1 + rnd.nextInt(20)) + ")");
      System.out.println("COURSE: 200 rows");

      for (int s = 1; s <= 1000; s++)
         insert("insert into SECTION(SectId, CourseId, Prof, YearOffered) values ("
               + s + ", " + (1 + rnd.nextInt(200)) + ", 'prof" + (1 + rnd.nextInt(50)) + "', "
               + (2015 + rnd.nextInt(10)) + ")");
      System.out.println("SECTION: 1000 rows");

      for (int s = 1; s <= 2000; s++)
         insert("insert into STUDENT(SId, SName, MajorId, GradYear) values ("
               + s + ", 's" + s + "', " + (1 + rnd.nextInt(20)) + ", " + (2019 + rnd.nextInt(8)) + ")");
      System.out.println("STUDENT: 2000 rows");

      String[] grades = {"A", "B", "C", "D", "F"};
      for (int e = 1; e <= 10000; e++) {
         insert("insert into ENROLL(EId, StudentId, SectionId, Grade) values ("
               + e + ", " + (1 + rnd.nextInt(2000)) + ", " + (1 + rnd.nextInt(1000)) + ", '"
               + grades[rnd.nextInt(grades.length)] + "')");
         if (e % 2000 == 0)
            System.out.println("ENROLL: " + e + " rows");
      }

      tx.commit();
      System.out.println("\n" + DB + " built. Run Experiment1 or Experiment2 next.");
   }

   /** Runs one insert, and commits every BATCH inserts. */
   private static void insert(String sql) {
      planner.executeUpdate(sql, tx);
      inserts++;
      if (inserts % BATCH == 0) {
         tx.commit();
         tx = db.newTx();
      }
   }

   private static void deleteDir(File dir) {
      File[] files = dir.listFiles();
      if (files != null)
         for (File f : files)
            f.delete();
      dir.delete();
   }
}
