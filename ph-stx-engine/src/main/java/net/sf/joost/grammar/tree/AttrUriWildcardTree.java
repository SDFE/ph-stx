/*
 * $Id: AttrUriWildcardTree.java,v 1.3 2007/11/25 14:18:01 obecker Exp $
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

package net.sf.joost.grammar.tree;

import org.xml.sax.SAXException;

import net.sf.joost.grammar.AbstractTree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;

/**
 * Objects of Tree represent nodes in the syntax tree of a pattern or an STXPath
 * expression.
 *
 * @version $Revision: 1.3 $ $Date: 2007/11/25 14:18:01 $
 * @author Oliver Becker
 */
public final class AttrUriWildcardTree extends AbstractTree
{
  /**
   * Constructs an AttrUriWildcardTree object with a given local name.
   *
   * @param lName
   *        the local name
   * @param context
   *        the parse context
   */
  public AttrUriWildcardTree (final String lName, final ParseContext context)
  {
    super (ATTR_URI_WILDCARD);
    m_sLocalName = lName;
  }

  @Override
  public boolean matches (final Context context, final int top, final boolean setPosition) throws SAXException
  {
    // an attribute requires at least two ancestors
    if (top < 3)
      return false;
    final SAXEvent e = context.ancestorStack.elementAt (top - 1);
    if (e.m_nType != SAXEvent.ATTRIBUTE)
      return false;
    if (setPosition)
      context.position = 1; // position for attributes is undefined
    if (m_sLocalName.equals (e.m_sLocalName))
      return true;
    return false;
  }

  @Override
  public Value evaluate (final Context context, final int top) throws SAXException
  {
    Value v1;
    // determine effective parent node sequence (-> v1)
    if (m_aLeft != null)
    { // preceding path
      v1 = m_aLeft.evaluate (context, top);
      if (v1.type == Value.EMPTY)
        return v1;
    }
    else
      if (top > 0) // use current node
        v1 = new Value (context.ancestorStack.elementAt (top - 1));
      else
        return Value.VAL_EMPTY;

    // iterate through this node sequence
    Value ret = null, last = null; // for constructing the result seq
    do
    {
      final SAXEvent e = v1.getNode ();
      if (e == null)
      {
        context.m_aErrorHandler.error ("Current item for evaluating '@*:" +
                                    m_sLocalName +
                                    "' is not a node (got " +
                                    v1 +
                                    ")",
                                    context.currentInstruction.m_sPublicID,
                                    context.currentInstruction.m_sSystemID,
                                    context.currentInstruction.lineNo,
                                    context.currentInstruction.colNo);
        // if the errorHandler decides to continue ...
        return Value.VAL_EMPTY;
      }
      final int len = e.m_aAttrs.getLength ();
      // iterate through attribute list
      for (int i = 0; i < len; i++)
      {
        if (m_sLocalName.equals (e.m_aAttrs.getLocalName (i)))
        {
          final Value v2 = new Value (SAXEvent.newAttribute (e.m_aAttrs.getURI (i),
                                                             m_sLocalName,
                                                             e.m_aAttrs.getQName (i),
                                                             e.m_aAttrs.getValue (i)));
          if (last != null)
            last.next = v2;
          else
            ret = v2;
          last = v2;
        }
      } // for
      v1 = v1.next; // next node
    } while (v1 != null);

    if (ret != null)
      return ret;
    return Value.VAL_EMPTY;
  }

  @Override
  public double getPriority ()
  {
    return -0.25;
  }

  @Override
  public boolean isConstant ()
  {
    return false;
  }
}
