package simpledb.query;

import java.time.LocalTime;

/**
 * The class that denotes values stored in the database.
 * @author Edward Sciore
 */
public class Constant implements Comparable<Constant> {
   private Integer ival = null;
   private String  sval = null;
   private Float   fval = null;
   private Double  dval = null;
   private LocalTime tval = null;
   
   public Constant(Integer ival) {
      this.ival = ival;
   }
   
   public Constant(String sval) {
      this.sval = sval;
   }
   
   public Constant(LocalTime tval) {
	   this.tval = tval;
   }
   
   public int asInt() {
      return ival;
   }
   
   public String asString() {
      return sval;
   }
   
   public boolean equals(Object obj) {
	   if (obj == null || !(obj instanceof Constant))
		      return false;

	   Constant c = (Constant) obj;
	   
	   if (ival != null) {
		   return ival.equals(c.ival);
	   } else if (sval != null) {
		   return sval.equals(c.sval);
	   } 
	   
	   return false;
       /* return (ival != null) ? ival.equals(c.ival) : sval.equals(c.sval); */
   }
   
   public int compareTo(Constant c) {
      return (ival != null) ? ival.compareTo(c.ival) : sval.compareTo(c.sval);
   }
   
   public int hashCode() {
      return (ival != null) ? ival.hashCode() : sval.hashCode();
   }
   
   public String toString() {
      return (ival != null) ? ival.toString() : sval.toString();
   }   
}
