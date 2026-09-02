package simpledb.plan;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class PlannerTest3 {
	public static void main(String[] args) {
	      SimpleDB db = new SimpleDB("plannertest3");
	      Transaction tx = db.newTx();
	      Planner planner = db.planner();
	      String cmd = "create table T3(A int, B varchar(9))";
	      planner.executeUpdate(cmd, tx);

	      int n = 0;
	      System.out.println("Inserting " + n + " random records.");
	      for (int i=0; i<n; i++) {
	         int a = (int) Math.round(Math.random() * 50);
	         int b_ran = (int) Math.round(Math.random() * 50);
	         String b = "rec" + b_ran;
	         cmd = "insert into T3(A,B) values(" + a + ", '" + b + "')";
	         planner.executeUpdate(cmd, tx);
	      }

	      String qry = "select A, B from T3 order by A asc, B desc";
	      Plan p = planner.createQueryPlan(qry, tx);
	      Scan s = p.open();
	      while (s.next())
	    	  System.out.println(s.getInt("a") +": "+ s.getString("b"));
	         // System.out.println(s.getString("b")); 
	      s.close();
	      tx.commit();
	   }
}
