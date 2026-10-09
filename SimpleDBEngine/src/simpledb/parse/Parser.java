package simpledb.parse;

import java.util.*;

import simpledb.query.*;
import simpledb.record.*;
import simpledb.materialize.AggregationFn;
import simpledb.materialize.AvgFn;
import simpledb.materialize.CountFn;
import simpledb.materialize.MaxFn;
import simpledb.materialize.MinFn;
import simpledb.materialize.SumFn;

/**
 * The SimpleDB parser.
 * @author Edward Sciore
 */
public class Parser {
   private Lexer lex;
   
   public Parser(String s) {
      lex = new Lexer(s);
   }
   
// Methods for parsing predicates, terms, expressions, constants, and fields
   
   public String field() {
      return lex.eatId();
   }
   
   public Constant constant() {
      if (lex.matchStringConstant())
         return new Constant(lex.eatStringConstant());
      else
         return new Constant(lex.eatIntConstant());
   }
   
   public Expression expression() {
      if (lex.matchId())
         return new Expression(field());
      else
         return new Expression(constant());
   }
   
   public Term term() {
      Expression lhs = expression();
      String opr = lex.eatOperator();
      Expression rhs = expression();
      
      return new Term(lhs, opr, rhs);
   }
   
   public Predicate predicate() {
      Predicate pred = new Predicate(term());
      if (lex.matchKeyword("and")) {
         lex.eatKeyword("and");
         pred.conjoinWith(predicate());
      }
      return pred;
   }
   
// Methods for parsing queries
   public QueryData query() {
      lex.eatKeyword("select");
      boolean distinct = false;
      if (lex.matchKeyword("distinct")) {
    	  lex.eatKeyword("distinct");
    	  distinct = true;
      }
      List<String> fields = new ArrayList<String>();
      List<AggregationFn> aggFns = new ArrayList<AggregationFn>();
      selectList(fields, aggFns);
      lex.eatKeyword("from");
      Collection<String> tables = tableList();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      
      List<String> groupFields = new ArrayList<String>();
      if (lex.matchKeyword("group")) {
         lex.eatKeyword("group");
         lex.eatKeyword("by");
         groupFields = fieldList();
      }
      
      List<String> orders = new ArrayList<>();
      if (lex.matchKeyword("order")) {
        lex.eatKeyword("order");
        lex.eatKeyword("by");
        orders = orderList();  
      }
      
      // with group by / aggregates, every non-aggregate select field
      // must be a group by field
      if (!groupFields.isEmpty() || !aggFns.isEmpty()) {
         for (String fld : fields) {
            boolean isAgg = false;
            for (AggregationFn fn : aggFns)
               if (fn.fieldName().equals(fld))
                  isAgg = true;
            if (!isAgg && !groupFields.contains(fld))
               throw new BadSyntaxException();
         }
      }
      
      return new QueryData(fields, tables, orders, pred, groupFields, aggFns, distinct);
   }
   
   private void selectList(List<String> fields, List<AggregationFn> aggFns) {
      if (lex.matchAggregate()) {
         AggregationFn fn = aggregate();
         aggFns.add(fn);
         fields.add(fn.fieldName());   // e.g. "countofsid", so ProjectPlan keeps it
      }
      else
         fields.add(field());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         selectList(fields, aggFns);
      }
   }
   
   private AggregationFn aggregate() {
      String fnName = lex.eatAggregate();
      lex.eatDelim('(');
      String fld = field();
      lex.eatDelim(')');
      switch (fnName) {
         case "sum":   return new SumFn(fld);
         case "count": return new CountFn(fld);
         case "avg":   return new AvgFn(fld);
         case "min":   return new MinFn(fld);
         default:      return new MaxFn(fld);
      }
   }

   private Collection<String> tableList() {
      Collection<String> L = new ArrayList<String>();
      L.add(lex.eatId());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(tableList());
      }
      return L;
   }
   
   private List<String> orderList() {
	   List<String> orders = new ArrayList<String>();

	   String currField = lex.matchAggregate() ? aggregate().fieldName() : field();
	   String direction = "asc"; // default

	   if (lex.matchKeyword("asc")) {
	      lex.eatKeyword("asc");
	      direction = "asc";
	   }
	   else if (lex.matchKeyword("desc")) {
	      lex.eatKeyword("desc");
	      direction = "desc";
	   }

	   orders.add(currField + " " + direction);

	   if (lex.matchDelim(',')) {
	      lex.eatDelim(',');
	      orders.addAll(orderList());
	   }

	   return orders;
	}
   
// Methods for parsing the various update commands
   
   public Object updateCmd() {
      if (lex.matchKeyword("insert"))
         return insert();
      else if (lex.matchKeyword("delete"))
         return delete();
      else if (lex.matchKeyword("update"))
         return modify();
      else
         return create();
   }
   
   private Object create() {
      lex.eatKeyword("create");
      if (lex.matchKeyword("table"))
         return createTable();
      else if (lex.matchKeyword("view"))
         return createView();
      else
         return createIndex();
   }
   
// Method for parsing delete commands
   
   public DeleteData delete() {
      lex.eatKeyword("delete");
      lex.eatKeyword("from");
      String tblname = lex.eatId();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      return new DeleteData(tblname, pred);
   }
   
// Methods for parsing insert commands
   
   public InsertData insert() {
      lex.eatKeyword("insert");
      lex.eatKeyword("into");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      List<String> flds = fieldList();
      lex.eatDelim(')');
      lex.eatKeyword("values");
      lex.eatDelim('(');
      List<Constant> vals = constList();
      lex.eatDelim(')');
      return new InsertData(tblname, flds, vals);
   }
   
   private List<String> fieldList() {
      List<String> L = new ArrayList<String>();
      L.add(field());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(fieldList());
      }
      return L;
   }
   
   private List<Constant> constList() {
      List<Constant> L = new ArrayList<Constant>();
      L.add(constant());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(constList());
      }
      return L;
   }
   
// Method for parsing modify commands
   
   public ModifyData modify() {
      lex.eatKeyword("update");
      String tblname = lex.eatId();
      lex.eatKeyword("set");
      String fldname = field();
      lex.eatDelim('=');
      Expression newval = expression();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      return new ModifyData(tblname, fldname, newval, pred);
   }
   
// Method for parsing create table commands
   
   public CreateTableData createTable() {
      lex.eatKeyword("table");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      Schema sch = fieldDefs();
      lex.eatDelim(')');
      return new CreateTableData(tblname, sch);
   }
   
   private Schema fieldDefs() {
      Schema schema = fieldDef();
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         Schema schema2 = fieldDefs();
         schema.addAll(schema2);
      }
      return schema;
   }
   
   private Schema fieldDef() {
      String fldname = field();
      return fieldType(fldname);
   }
   
   private Schema fieldType(String fldname) {
      Schema schema = new Schema();
      if (lex.matchKeyword("int")) {
         lex.eatKeyword("int");
         schema.addIntField(fldname);
      }
      else {
         lex.eatKeyword("varchar");
         lex.eatDelim('(');
         int strLen = lex.eatIntConstant();
         lex.eatDelim(')');
         schema.addStringField(fldname, strLen);
      }
      return schema;
   }
   
// Method for parsing create view commands
   
   public CreateViewData createView() {
      lex.eatKeyword("view");
      String viewname = lex.eatId();
      lex.eatKeyword("as");
      QueryData qd = query();
      return new CreateViewData(viewname, qd);
   }
   
   
//  Method for parsing create index commands
   
   public CreateIndexData createIndex() {
      lex.eatKeyword("index");
      lex.eatKeyword("using");
      String idxtype = lex.eatId();
      String idxname = lex.eatId();
      lex.eatKeyword("on");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      String fldname = field();
      lex.eatDelim(')');
      return new CreateIndexData(idxname, idxtype, tblname, fldname);
   }
}

