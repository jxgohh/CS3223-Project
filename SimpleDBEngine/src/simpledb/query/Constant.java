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
//   private Double  dval = null;
//   private LocalTime tval = null;
   
   public Constant(Integer ival) {
      this.ival = ival;
   }
   
   public Constant(String sval) {
      this.sval = sval;
   }
   
   public Constant(Float fval) {
	   this.fval = fval;
   }
   
   public int asInt() {
      return ival;
   }
   
   public String asString() {
      return sval;
   }
   
   public float asFloat() {
	   return fval;
   }
   
   public boolean isFloat() {
	   return fval != null;
   }
   
   public boolean equals(Object obj) {
	   if (obj == null || !(obj instanceof Constant))
		      return false;

	   Constant c = (Constant) obj;
	   
	   if (ival != null) return ival.equals(c.ival);
	   if (sval != null) return sval.equals(c.sval);
	   if (fval != null) return fval.equals(c.fval);
	   return false;
       /* return (ival != null) ? ival.equals(c.ival) : sval.equals(c.sval); */
   }
   
   public int compareTo(Constant c) {
	   if (ival != null && c.ival != null) return ival.compareTo(c.ival);
	   if (sval != null && c.sval != null) return sval.compareTo(c.sval);
	   if (fval != null && c.fval != null) return fval.compareTo(c.fval);
	   throw new IllegalArgumentException("Cannot compare constants of different types");
   }
   
   public int hashCode() {
	   if (ival != null) return ival.hashCode();
	   if (sval != null) return sval.hashCode();
	   if (fval != null) return fval.hashCode();
	   return 0;
	}

	public String toString() {
	   if (ival != null) return ival.toString();
	   if (sval != null) return sval;
	   if (fval != null) return fval.toString();
	   return "";
	}
}
