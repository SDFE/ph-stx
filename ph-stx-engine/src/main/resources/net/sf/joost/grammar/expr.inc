/*
 * $Id: expr.inc,v 1.9 2004/09/29 05:59:50 obecker Exp $
 * 
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the 
 * License.
 *
 * The Original Code is: this file
 *
 * The Initial Developer of the Original Code is Oliver Becker.
 *
 * Portions created by  ______________________ 
 * are Copyright (C) ______ _______________________. 
 * All Rights Reserved.
 *
 * Contributor(s): Thomas Behrends.
 */

/* Common expression grammar which is needed for expressions (sic!) in
 * Expr.cup as well as for patterns in Pattern.cup 
 * (different start symbols) */


/* Non-terminals */
non terminal AbstractTree   Expr, AndExpr, GeneralComp,
                    AdditiveExpr, MultiplicativeExpr, UnaryExpr, BasicExpr, 
                    Accessor, NodeAccessor, RelativeAccessor, AccessorStep,
                    NodeNameTest, KindTest, AttributeNameTest, 
                    FunctionCall, ExprSeq, ParaSeq;
non terminal String Name;

/* Start symbol, replaced by Ant with the grammar name (Pattern or Expr)*/
start with @@@START-NT@@@;


/* Grammar */
Expr ::= 
   Expr:e1 OR AndExpr:e2
      {: RESULT = new OrTree(e1, e2); :}
 | AndExpr:t
      {: RESULT = t; :}
 ;

AndExpr ::=
   AndExpr:e1 AND GeneralComp:e2
      {: RESULT = new AndTree(e1, e2); :}
 | GeneralComp:t
      {: RESULT = t; :}
 ;

/* recursiveness to discuss! */
GeneralComp ::=
   GeneralComp:e1 EQ AdditiveExpr:e2
      {: RESULT = new EqTree(e1, e2); :}
 | GeneralComp:e1 NE AdditiveExpr:e2
      {: RESULT = new NeTree(e1, e2); :}
 | GeneralComp:e1 LT AdditiveExpr:e2
      {: RESULT = new LtTree(e1, e2); :}
 | GeneralComp:e1 LE AdditiveExpr:e2
      {: RESULT = new LeTree(e1, e2); :}
 | GeneralComp:e1 GT AdditiveExpr:e2
      {: RESULT = new GtTree(e1, e2); :}
 | GeneralComp:e1 GE AdditiveExpr:e2
      {: RESULT = new GeTree(e1, e2); :}
 | AdditiveExpr:t
      {: RESULT = t; :}
 ;

AdditiveExpr ::=
   AdditiveExpr:e1 PLUS MultiplicativeExpr:e2
      {: RESULT = new AddTree(e1, e2); :}
 | AdditiveExpr:e1 MINUS MultiplicativeExpr:e2
      {: RESULT = new SubTree(e1, e2); :}
 | MultiplicativeExpr:t
      {: RESULT = t; :}
 ;

MultiplicativeExpr ::=
   MultiplicativeExpr:e1 STAR UnaryExpr:e2
      {: RESULT = new MultTree(e1, e2); :}
 | MultiplicativeExpr:e1 DIV UnaryExpr:e2
      {: RESULT = new DivTree(e1, e2); :}
 | MultiplicativeExpr:e1 MOD UnaryExpr:e2
      {: RESULT = new ModTree(e1, e2); :}
 | UnaryExpr:t
      {: RESULT = t; :}
 ;

UnaryExpr ::=
   PLUS BasicExpr:t
      {: RESULT = new AddTree(null, t); :}
 | MINUS BasicExpr:t
      {: RESULT = new SubTree(null, t); :}
 | BasicExpr:t
      {: RESULT = t; :}
 ;

BasicExpr ::=
   Accessor:t
      {: RESULT = t; :}
 | STRING:n  /* Literal */
      {: RESULT = new StringTree(n); :}
 | NUMBER:n  /* Literal */
      {: RESULT = new NumberTree(n); :}
 | LPAR ExprSeq:t RPAR  /* ParenthesizedExpr */
      {: RESULT = (t == null ? new ListTree() : t); :}
 | DOT
      {: RESULT = new DotTree(); :}
 ;

Accessor ::= 
   NodeAccessor:n SLASH AttributeNameTest:a
      {: a.m_aLeft = n; 
         RESULT = a; :} 
 | NodeAccessor:n
      {: RESULT = n; :}
 | RelativeAccessor:n SLASH AttributeNameTest:a
      {: a.m_aLeft = n.reverseAssociativity(); 
         RESULT = a; :} 
 | RelativeAccessor:n
      {: RESULT = n.reverseAssociativity(); :}
 | SLASH RelativeAccessor:n SLASH AttributeNameTest:a
      {: a.m_aLeft = new RootTree(n.reverseAssociativity()); 
         RESULT = a; :} 
 | SLASH RelativeAccessor:n
      {: RESULT = new RootTree(n.reverseAssociativity()); :}
 | DSLASH RelativeAccessor:n SLASH AttributeNameTest:a
      {: a.m_aLeft = new RootTree(new DescTree(null, n.reverseAssociativity()));
         RESULT = a; :} 
 | DSLASH RelativeAccessor:n
      {: RESULT = new RootTree(new DescTree(null, 
                                            n.reverseAssociativity())); :}
 | AttributeNameTest:a
      {: RESULT = a; :}
 ;

/* 
I'd rather specify something along the lines

Accessor ::=
   NodeAccessor SLASH AttributeNameTest
 | NodeAccessor
 | AttributeNameTest
 ;

NodeAccessor ::=
   PathAccessor
 | Variable
 | FunctionCall
 ;

PathAccessor ::= 
   SLASH RelativeAccessor
 | DSLASH RelativeAccessor
 | RelativeAccessor
 ;

Unfortunately this results in shift/reduce conflicts in CUP.
*/

NodeAccessor ::= 
   DOLLAR Name:n  /* Variable */
      {: RESULT = new VarTree(n, parser.j_context); :}
 | FunctionCall:t
      {: RESULT = t; :}
 ;

RelativeAccessor ::=
   RelativeAccessor:r SLASH AccessorStep:a
      {: if (a.getType () == AbstractTree.DDOT) {
            a.m_aLeft = r;
            RESULT = a;
         }
         else
            RESULT = new ChildTree(r, a);
      :}
 | RelativeAccessor:r DSLASH AccessorStep:a
      {: AbstractTree d = new DescTree(r, null);
         if (a.getType () == AbstractTree.DDOT) {
            a.m_aLeft = d;
            RESULT = a;
         }
         else 
            RESULT = new ChildTree(d, a);
      :}
 | AccessorStep:a
      {: if (a.getType () == AbstractTree.DDOT)
            RESULT = a;
         else
            RESULT = new ChildTree(null, a);
      :}
 ;

AccessorStep ::=
   NodeNameTest:n
      {: RESULT = n; :}
 | KindTest:k
      {: RESULT = k; :}
 | DDOT
      {: RESULT = new DdotTree(); :}
 ;

NodeNameTest ::=
   Name:n
      {: RESULT = new NameTestTree(n, parser.j_context); :}
 | STAR
      {: RESULT = new WildcardTree(); :}
 | STAR COLON NCNAME:n
      {: RESULT = new UriWildcardTree(n, parser.j_context); :}
 | NCNAME:n COLON STAR
      {: RESULT = new LocalWildcardTree(n, parser.j_context); :}
 ;

KindTest ::=
   COMMENT
      {: RESULT = new CommentTestTree(); :}
 | NODE
      {: RESULT = new NodeTestTree(); :}
 | PI_START RPAR
      {: RESULT = new PiTestTree(); :}
 | PI_START STRING:s RPAR
      {: RESULT = new PiTestTree(s); :}
 | TEXT
      {: RESULT = new TextTestTree(); :}
 | CDATA
      {: RESULT = new CDataTestTree(); :}
 ;

AttributeNameTest ::=
   AT Name:n
      {: RESULT = new AttrTree(n, parser.j_context); :}
 | AT STAR
      {: RESULT = new AttrWildcardTree(); :}
 | AT STAR COLON NCNAME:n
      {: RESULT = new AttrUriWildcardTree(n, parser.j_context); :}
 | AT NCNAME:n COLON STAR
      {: RESULT = new AttrLocalWildcardTree(n, parser.j_context); :}
 ;

FunctionCall ::=
   Name:n LPAR ParaSeq:e RPAR
      {: RESULT = new FunctionTree(n, e, parser.j_context); :}
 ;

ExprSeq ::=
      {: RESULT = new SeqTree(); :}
 | Expr:t
      {: RESULT = t; :}
 | Expr:t1 COMMA ExprSeq:t2
      {: RESULT = new SeqTree(t1, t2); :}
 ;

ParaSeq ::=
   /* empty: RESULT = null */
 | Expr:t
      {: RESULT = t; :}
 | ParaSeq:t1 COMMA Expr:t2
      {: RESULT = new ListTree(t1, t2); :}
 ;

/* some keywords can be names too */
Name ::=
   QNAME:n
     {: RESULT = n; :}
 | NCNAME:n
     {: RESULT = n; :}
 | DIV:n
     {: RESULT = n; :}
 | MOD:n
     {: RESULT = n; :}
 | AND:n
     {: RESULT = n; :}
 | OR:n
     {: RESULT = n; :}
;
