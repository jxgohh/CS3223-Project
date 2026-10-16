package simpledb.parse;

import java.util.*;
import java.io.*;

/**
 * The lexical analyzer.
 * @author Edward Sciore
 */
public class Lexer {
   private Collection<String> keywords;
   private StreamTokenizer tok;
   
   /**
    * Creates a new lexical analyzer for SQL statement s.
    * @param s the SQL statement
    */
   public Lexer(String s) {
      initKeywords();
      tok = new StreamTokenizer(new StringReader(s));
      // Read numeric literals as words for the integer/float checks below.
      tok.ordinaryChars('0', '9');
      tok.ordinaryChar('.');
      tok.ordinaryChar('-');
      tok.wordChars('0', '9');
      tok.wordChars('.', '.');
      tok.wordChars('-', '-'); // supports negative numbers
      tok.wordChars('_', '_'); //allow "_" in identifiers
      tok.lowerCaseMode(true); //ids and keywords are converted
      nextToken();
   }
   
//Methods to check the status of the current token

   public boolean matchEnd() {
      return tok.ttype == StreamTokenizer.TT_EOF;
   }
   
   /**
    * Returns true if the current token is
    * the specified delimiter character.
    * @param d a character denoting the delimiter
    * @return true if the delimiter is the current token
    */
   public boolean matchDelim(char d) {
      return d == (char)tok.ttype;
   }
   
   public boolean matchOperator() {
      char c = (char) tok.ttype;
      return c=='=' || c=='<' || c=='>';
   }
   
   /**
    * Returns true if the current token is an integer.
    * @return true if the current token is an integer
    */
   public boolean matchIntConstant() {
	   return tok.ttype == StreamTokenizer.TT_WORD
	         && tok.sval.matches("-?\\d+");
   }
   
   public boolean matchFloatConstant() {
	   return tok.ttype == StreamTokenizer.TT_WORD
	         && tok.sval.matches("-?(?:\\d+\\.\\d*|\\.\\d+)");
   }
   
   /**
    * Returns true if the current token is a string.
    * @return true if the current token is a string
    */
   public boolean matchStringConstant() {
      return '\'' == (char)tok.ttype;
   }
   
   /**
    * Returns true if the current token is the specified keyword.
    * @param w the keyword string
    * @return true if that keyword is the current token
    */
   public boolean matchKeyword(String w) {
      return tok.ttype == StreamTokenizer.TT_WORD && tok.sval.equals(w);
   }
   
   
   public boolean matchAggregate() {
      return matchKeyword("sum") || matchKeyword("count") || matchKeyword("avg")
            || matchKeyword("min") || matchKeyword("max");
   }
   
   /**
    * Returns true if the current token is a legal identifier.
    * @return true if the current token is an identifier
    */
   public boolean matchId() {
	   return tok.ttype == StreamTokenizer.TT_WORD
	         && tok.sval.matches("[a-z_][a-z0-9_]*")
	         && !keywords.contains(tok.sval);
   }
   
   public boolean matchFieldName() {
	   return tok.ttype == StreamTokenizer.TT_WORD
	         && tok.sval.matches("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?");
   }
   
//Methods to "eat" the current token
   
   /**
    * Throws an exception if the current token is not the
    * specified delimiter. 
    * Otherwise, moves to the next token.
    * @param d a character denoting the delimiter
    */
   public void eatDelim(char d) {
      if (!matchDelim(d))
         throw new BadSyntaxException();
      nextToken();
   }
   
    public String eatOperator() {
	   if (!matchOperator()) {
		   throw new BadSyntaxException();
	   }
	   
       char first = (char) tok.ttype;
	   nextToken();
	   
	   if ((first=='<' || first=='>') && matchDelim('=')) {
	      nextToken();
	      return first + "=";
	   }
	   if (first=='<' && matchDelim('>')) {
	      nextToken();
	      return "<>";
	   }
	   return Character.toString(first);   // single-char operator: =, <, >
    }
   
   /**
    * Throws an exception if the current token is not 
    * an integer. 
    * Otherwise, returns that integer and moves to the next token.
    * @return the integer value of the current token
    */
    public int eatIntConstant() {
	   if (!matchIntConstant())
	      throw new BadSyntaxException();
	   int value = Integer.parseInt(tok.sval); // "05" becomes 5
	   nextToken();
	   return value;
    }

    public float eatFloatConstant() {
	   if (!matchFloatConstant())
	      throw new BadSyntaxException();
	   float value = Float.parseFloat(tok.sval); // "12.05" stays 12.05
	   nextToken();
	   return value;
    }
   
   /**
    * Throws an exception if the current token is not 
    * a string. 
    * Otherwise, returns that string and moves to the next token.
    * @return the string value of the current token
    */
   public String eatStringConstant() {
      if (!matchStringConstant())
         throw new BadSyntaxException();
      String s = tok.sval; //constants are not converted to lower case
      nextToken();
      return s;
   }
   
   /**
    * Throws an exception if the current token is not the
    * specified keyword. 
    * Otherwise, moves to the next token.
    * @param w the keyword string
    */
   public void eatKeyword(String w) {
      if (!matchKeyword(w))
         throw new BadSyntaxException();
      nextToken();
   }
   
   public String eatAggregate() {
      if (!matchAggregate())
         throw new BadSyntaxException();
      String s = tok.sval;
      nextToken();
      return s;
   }
   
   /**
    * Throws an exception if the current token is not 
    * an identifier. 
    * Otherwise, returns the identifier string 
    * and moves to the next token.
    * @return the string value of the current token
    */
   public String eatId() {
      if (!matchId())
         throw new BadSyntaxException();
      String s = tok.sval;
      nextToken();
      return s;
   }
   
   public String eatFieldName() {
	   if (!matchFieldName())
	      throw new BadSyntaxException();
	   String name = tok.sval;
	   nextToken();
	   return name;
   }
   
   private void nextToken() {
      try {
         tok.nextToken();
      }
      catch(IOException e) {
         throw new BadSyntaxException();
      }
   }
   
   private void initKeywords() {
      keywords = Arrays.asList("select", "from", "where", "and",
                               "insert", "into", "values", "delete", "update", "set", 
                               "create", "table", "int", "varchar", "float", "view", "as", "index", 
                               "using", "on", "asc", "desc", "order", "by",
                               "group", "sum", "count", "avg", "min", "max", "distinct",
                               "union", "intersect", "except");
   }
}
