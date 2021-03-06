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
package net.sf.joost.grammar.tree;

import org.xml.sax.SAXException;

import net.sf.joost.grammar.AbstractTree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;

/**
 * Objects of WildcardTree represent element test nodes ("*") in the syntax tree
 * of a pattern or an STXPath expression.
 *
 * @version $Revision: 1.2 $ $Date: 2007/05/20 18:00:44 $
 * @author Oliver Becker
 */
public final class WildcardTree extends AbstractTree
{
  public WildcardTree ()
  {
    super (WILDCARD);
  }

  @Override
  public boolean matches (final Context context, final int top, final boolean setPosition) throws SAXException
  {
    if (top < 2)
      return false;

    final SAXEvent e = context.ancestorStack.elementAt (top - 1);
    if (e.m_nType != SAXEvent.ELEMENT)
      return false;

    if (setPosition)
      context.position = context.ancestorStack.elementAt (top - 2).getPositionOf ("*", "*");

    return true;
  }

  @Override
  public double getPriority ()
  {
    return -0.5;
  }

  @Override
  public boolean isConstant ()
  {
    return false;
  }
}
