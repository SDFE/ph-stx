/**
 *  The contents of this file are subject to the Mozilla Public License
 *  Version 1.1 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is: this file
 *
 *  The Initial Developer of the Original Code is Oliver Becker.
 *
 *  Portions created by Philip Helger
 *  are Copyright (C) 2016-2017 Philip Helger
 *  All Rights Reserved.
 */
package net.sf.joost.instruction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.CSTX;
import net.sf.joost.grammar.AbstractTree;
import net.sf.joost.grammar.EvalException;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.util.regex.JRegularExpression;

/**
 * Factory for <code>analyze-text</code> elements, which are represented by the
 * inner Instance class.
 *
 * @version $Revision: 1.13 $ $Date: 2009/08/21 12:46:17 $
 * @author Oliver Becker
 */

public final class AnalyzeTextFactory extends AbstractFactoryBase
{
  /** allowed attributes for this element */
  private final Set <String> attrNames = new HashSet<> ();

  public AnalyzeTextFactory ()
  {
    attrNames.add ("select");
  }

  /** @return <code>"analyze-text"</code> */
  @Override
  public String getName ()
  {
    return "analyze-text";
  }

  @Override
  public AbstractNodeBase createNode (final AbstractNodeBase parent,
                                      final String qName,
                                      final Attributes attrs,
                                      final ParseContext context) throws SAXParseException
  {
    final AbstractTree selectExpr = parseRequiredExpr (qName, attrs, "select", context);

    checkAttributes (qName, attrs, attrNames, context);
    return new Instance (qName, parent, context, selectExpr);
  }

  /** Represents an instance of the <code>analyze-text</code> element. */
  public static final class Instance extends AbstractNodeBase
  {
    private AbstractTree m_aSelect;

    private AbstractInstruction successor;

    // this instruction manages its children itself
    private Vector <AbstractNodeBase> mVector = new Vector<> ();
    private MatchFactory.Instance [] matchChildren;
    private AbstractNodeBase noMatchChild;

    // Constructor
    protected Instance (final String qName,
                        final AbstractNodeBase parent,
                        final ParseContext context,
                        final AbstractTree select)
    {
      super (qName, parent, context, true);
      this.m_aSelect = select;
    }

    /**
     * Ensures that only <code>stx:match</code> or <code>stx:no-match</code>
     * children will be inserted.
     */
    @Override
    public void insert (final AbstractNodeBase node) throws SAXParseException
    {
      if (node instanceof MatchFactory.Instance)
      {
        if (noMatchChild != null)
        {
          // this test is not really necessary for the implementation,
          // however, it is required by the specification
          throw new SAXParseException ("'" +
                                       m_sQName +
                                       "' must not have more children after stx:no-match",
                                       node.m_sPublicID,
                                       node.m_sSystemID,
                                       node.lineNo,
                                       node.colNo);
        }
        mVector.add (node);
      }
      else
        if (node instanceof NoMatchFactory.Instance)
        {
          if (noMatchChild != null)
            throw new SAXParseException ("'" +
                                         m_sQName +
                                         "' must have at most one '" +
                                         node.m_sQName +
                                         "' child",
                                         node.m_sPublicID,
                                         node.m_sSystemID,
                                         node.lineNo,
                                         node.colNo);
          noMatchChild = node;
        }
        else
          if (node instanceof TextNode)
          {
            if (((TextNode) node).isWhitespaceNode ())
            {
              // ignore white space nodes (from xml:space="preserve")
              return;
            }
            throw new SAXParseException ("'" +
                                         m_sQName +
                                         "' may only contain stx:match and stx:no-match children " +
                                         "(encountered text)",
                                         node.m_sPublicID,
                                         node.m_sSystemID,
                                         node.lineNo,
                                         node.colNo);
          }
          else
            throw new SAXParseException ("'" +
                                         m_sQName +
                                         "' may only contain stx:match and stx:no-match children " +
                                         "(encountered '" +
                                         node.m_sQName +
                                         "')",
                                         node.m_sPublicID,
                                         node.m_sSystemID,
                                         node.lineNo,
                                         node.colNo);

      // no invocation of super.insert(node) necessary
      // the children have been stored in #mVector and #noMatchChild
    }

    /**
     * Check if there is at least one <code>stx:match</code> child and establish
     * a loop
     */
    @Override
    public boolean compile (final int pass, final ParseContext context) throws SAXParseException
    {
      if (pass == 0)
        return true;

      if (mVector.size () == 0)
        throw new SAXParseException ("'" +
                                     m_sQName +
                                     "' must have at least one stx:match child",
                                     m_sPublicID,
                                     m_sSystemID,
                                     lineNo,
                                     colNo);

      // transform the Vector into an array
      matchChildren = new MatchFactory.Instance [mVector.size ()];
      mVector.toArray (matchChildren);
      mVector = null; // for garbage collection

      successor = m_aNodeEnd.next;
      m_aNodeEnd.next = this; // loop

      return false;
    }

    // needed to detect recursive invocations
    private boolean continued = false;

    /**
     * For the regex-group function (accessed from the stx:match and
     * stx:no-match children, so they cannot be private)
     *
     * @see net.sf.joost.stx.function.RegexGroup
     */
    protected String [] capSubstr, noMatchStr;

    /**
     * Evaluate the expression given in the <code>select</code> attribute; find
     * and process the child with the matching regular expression
     */
    @Override
    public short process (final Context context) throws SAXException
    {
      String text;
      int lastIndex;
      Matcher [] matchers;

      if (continued)
      {
        // restore previous values
        text = (String) m_aLocalFieldStack.pop ();
        lastIndex = ((Integer) m_aLocalFieldStack.pop ()).intValue ();
        matchers = (Matcher []) m_aLocalFieldStack.pop ();
        continued = false; // in case there will be an stx:process-xxx
      }
      else
      { // this is a new invocation
        text = m_aSelect.evaluate (context, this).getStringValue ();
        lastIndex = 0;
        // create a pseudo variable for regex-group()
        if (context.localRegExGroup == null)
          context.localRegExGroup = new Stack<> ();
        matchers = new Matcher [matchChildren.length];
        for (int i = 0; i < matchChildren.length; i++)
        {
          final String re = matchChildren[i].m_aRegex.evaluate (context, matchChildren[i]).getString ();

          final String flags = matchChildren[i].m_aFlags != null ? matchChildren[i].m_aFlags.evaluate (context,
                                                                                                       matchChildren[i])
                                                                                            .getString ()
                                                                 : "";
          try
          {
            matchers[i] = new JRegularExpression (re, true, flags).matcher (text);
          }
          catch (final EvalException e)
          {
            context.m_aErrorHandler.fatalError (e.getMessage (), m_sPublicID, m_sSystemID, lineNo, colNo, e);
            return CSTX.PR_ERROR;
          }
        }
      }

      if (text.length () != lastIndex)
      {
        int newIndex = text.length ();
        int maxSubstringLength = 0;
        int matchIndex = -1;

        for (int i = 0; i < matchers.length; i++)
        {
          int start = -1;
          if (matchers[i].find (lastIndex))
          {
            if (matchers[i].start () == matchers[i].end ())
            {
              while (matchers[i].find ())
              {
                if (matchers[i].start () != matchers[i].end ())
                {
                  start = matchers[i].start ();
                  break;
                }
              }
            }
            else
              start = matchers[i].start ();
          }
          if (start > -1 && start <= newIndex)
          {
            final int length = matchers[i].end () - start;
            if (start < newIndex || length > maxSubstringLength)
            {
              newIndex = matchers[i].start ();
              maxSubstringLength = length;
              matchIndex = i;
            }
          }
        }

        noMatchStr = new String [1];
        if (matchIndex != -1)
        { // found an stx:match
          capSubstr = new String [matchers[matchIndex].groupCount () + 1];
          for (int i = 0; i < capSubstr.length; i++)
            capSubstr[i] = matchers[matchIndex].group (i);
          noMatchStr[0] = text.substring (lastIndex, newIndex);
          m_aLocalFieldStack.push (matchers);
          m_aLocalFieldStack.push (new Integer (newIndex + maxSubstringLength));
          m_aLocalFieldStack.push (text);
          if (noMatchChild != null && newIndex != lastIndex)
          {
            // invoke stx:no-match before stx:match
            next = noMatchChild;
            noMatchChild.m_aNodeEnd.next = matchChildren[matchIndex];
          }
          else
            next = matchChildren[matchIndex];
        }
        else
        { // no matching regex found
          if (noMatchChild != null)
          {
            noMatchStr[0] = text.substring (lastIndex);
            next = noMatchChild;
            // leave stx:analyze-text after stx:no-match
            noMatchChild.m_aNodeEnd.next = successor;
          }
          else
            next = successor; // leave stx:analyze-text instantly
        }
      }
      else // text.length() == lastIndex, we're done
        next = successor;

      return CSTX.PR_CONTINUE;
    }

    @Override
    public short processEnd (final Context context) throws SAXException
    {
      continued = true;
      return CSTX.PR_CONTINUE;
    }

    @Override
    protected void onDeepCopy (final AbstractInstruction copy, final HashMap <Object, Object> copies)
    {
      super.onDeepCopy (copy, copies);
      final Instance theCopy = (Instance) copy;
      theCopy.continued = false;
      theCopy.capSubstr = theCopy.noMatchStr = null;
      if (matchChildren != null)
      {
        theCopy.matchChildren = new MatchFactory.Instance [matchChildren.length];
        for (int i = 0; i < matchChildren.length; i++)
        {
          theCopy.matchChildren[i] = (MatchFactory.Instance) matchChildren[i].deepCopy (copies);
        }
      }
      if (noMatchChild != null)
        theCopy.noMatchChild = (AbstractNodeBase) noMatchChild.deepCopy (copies);
      if (successor != null)
        theCopy.successor = successor.deepCopy (copies);
      if (m_aSelect != null)
        theCopy.m_aSelect = m_aSelect.deepCopy (copies);
    }

  }
}
