package simpledb.demo;

import java.util.Scanner;

import simpledb.plan.Plan;
import simpledb.tx.Transaction;

/**
 * Type your own queries on the demo database (see DemoDB for the data).
 * Queries go through the normal planner, which prints the plan, then the rows are printed.
 * Other statements (insert, update, delete, create) print the number of rows changed.
 * One statement per line, with no semicolon. Lines starting with "--" are ignored.
 * Type "exit" to quit. demo.sql (in this folder) has every demo query with its expected result.
 */
public class DemoShell {
   public static void main(String[] args) {
      shell(new Scanner(System.in));
   }

   public static void shell(Scanner in) {
      DemoDB.setup();
      System.out.println("\nType a query, or \"exit\".");
      while (true) {
         System.out.print("\nSQL> ");
         if (!in.hasNextLine())
            return;
         String sql = in.nextLine().trim();
         if (sql.equalsIgnoreCase("exit"))
            return;
         if (sql.isEmpty() || sql.startsWith("--"))
            continue;          // blank line or comment (lets you paste from demo.sql)
         Transaction tx = DemoDB.db.newTx();
         try {
            if (sql.toLowerCase().startsWith("select")) {
               Plan p = DemoDB.planner.createQueryPlan(sql, tx);   // prints "Query plan: ..."
               DemoDB.printRows(p);
            }
            else
               System.out.println(DemoDB.planner.executeUpdate(sql, tx) + " rows affected");
            tx.commit();
         }
         catch (Exception e) {
            System.out.println("Error: " + e);
            tx.rollback();
         }
      }
   }
}
