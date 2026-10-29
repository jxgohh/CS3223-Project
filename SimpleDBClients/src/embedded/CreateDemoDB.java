package embedded;
import java.io.File;
import java.sql.*;

import simpledb.jdbc.embedded.EmbeddedDriver;

/**
 * Creates the demo database (STUDENT 100 rows, DEPT 25, ENROLL 100, plus a
 * B-tree index on STUDENT(SId) and a hash index on ENROLL(StudentId)).
 * The rows are the same as in demo_setup.sql and simpledb.demo.DemoDB.
 * Any old demodb folder is deleted first, so this can be re-run safely.
 * Afterwards, run SimpleIJ and connect with jdbc:simpledb:demodb
 */
public class CreateDemoDB {
   static final String[] NAMES    = {"joe", "amy", "max", "sue", "bob", "kim", "art", "pat", "lee", "ann"};
   static final String[] SUBJECTS = {"compsci", "math", "drama", "physics", "chem",
                                     "bio", "econ", "history", "music", "art"};
   static final String[] COURSES  = {"db", "algebra", "calculus", "acting", "optics"};
   static final String[] GRADES   = {"A", "A-", "B+", "B", "C", "F"};

   public static void main(String[] args) {
      deleteDir(new File("demodb"));
      Driver d = new EmbeddedDriver();
      String url = "jdbc:simpledb:demodb";

      try (Connection conn = d.connect(url, null);
            Statement stmt = conn.createStatement()) {
         stmt.executeUpdate("create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int, Gpa float)");
         stmt.executeUpdate("create table DEPT(DId int, DName varchar(10))");
         stmt.executeUpdate("create table ENROLL(EId int, StudentId int, Course varchar(10), Grade varchar(2))");
         System.out.println("Tables STUDENT, DEPT, ENROLL created.");

         // indexes BEFORE the inserts, so the inserts add entries to them
         stmt.executeUpdate("create index using btree idxsid on STUDENT(SId)");
         stmt.executeUpdate("create index using hash idxstudentid on ENROLL(StudentId)");
         System.out.println("Indexes idxsid (btree) and idxstudentid (hash) created.");

         for (int sid = 1; sid <= 100; sid++) {
            String sname = NAMES[(sid - 1) % 10] + sid;
            int majorid = ((sid * 7) % 20 + 1) * 10;
            int gradyear = 2019 + sid % 4;
            float gpa = 2.0f + ((sid * 7) % 9) * 0.25f;
            stmt.executeUpdate("insert into STUDENT(SId, SName, MajorId, GradYear, Gpa) values ("
                  + sid + ", '" + sname + "', " + majorid + ", " + gradyear + ", " + gpa + ")");
         }
         System.out.println("STUDENT records inserted.");

         for (int i = 1; i <= 25; i++) {
            String dname = SUBJECTS[(i - 1) % 10] + ((i - 1) / 10 + 1);
            stmt.executeUpdate("insert into DEPT(DId, DName) values (" + (i * 10) + ", '" + dname + "')");
         }
         System.out.println("DEPT records inserted.");

         for (int eid = 1; eid <= 100; eid++) {
            int studentid = (eid * 7) % 75 + 1;
            stmt.executeUpdate("insert into ENROLL(EId, StudentId, Course, Grade) values (" + eid + ", "
                  + studentid + ", '" + COURSES[eid % 5] + "', '" + GRADES[eid % 6] + "')");
         }
         System.out.println("ENROLL records inserted.");
      }
      catch(SQLException e) {
         e.printStackTrace();
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
