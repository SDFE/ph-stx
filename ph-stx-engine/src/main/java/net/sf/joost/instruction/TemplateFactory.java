/*
 * $Id: TemplateFactory.java,v 2.11 2008/10/04 17:13:14 obecker Exp $
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
 * Contributor(s): ______________________________________.
 */

package net.sf.joost.instruction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.grammar.AbstractTree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;

/**
 * Factory for <code>template</code> elements, which are represented by the
 * inner Instance class.
 *
 * @version $Revision: 2.11 $ $Date: 2008/10/04 17:13:14 $
 * @author Oliver Becker
 */

public final class TemplateFactory extends AbstractFactoryBase
{
  /** allowed attributes for this element. */
  private final Set <String> attrNames;

  // Constructor
  public TemplateFactory ()
  {
    attrNames = new HashSet<> ();
    attrNames.add ("match");
    attrNames.add ("priority");
    attrNames.add ("visibility");
    attrNames.add ("public");
    attrNames.add ("new-scope");
  }

  /** @return <code>"template"</code> */
  @Override
  public String getName ()
  {
    return "template";
  }

  @Override
  public AbstractNodeBase createNode (final AbstractNodeBase parent,
                                      final String qName,
                                      final Attributes attrs,
                                      final ParseContext context) throws SAXParseException
  {
    if (parent == null || !(parent instanceof AbstractGroupBase))
      throw new SAXParseException ("'" +
                                   qName +
                                   "' must be a top level " +
                                   "element or a child of stx:group",
                                   context.locator);

    final AbstractTree matchPattern = parseRequiredPattern (qName, attrs, "match", context);

    final String priorityAtt = attrs.getValue ("priority");
    double priority;
    if (priorityAtt != null)
    {
      try
      {
        priority = Double.parseDouble (priorityAtt);
      }
      catch (final NumberFormatException ex)
      {
        throw new SAXParseException ("The priority value '" + priorityAtt + "' is not a number", context.locator);
      }
    }
    else
    {
      priority = matchPattern.getPriority ();
    }

    int visibility = getEnumAttValue ("visibility", attrs, AbstractTemplateBase.VISIBILITY_VALUES, context);
    if (visibility == -1)
      visibility = AbstractTemplateBase.LOCAL_VISIBLE; // default value

    final int publicAttVal = getEnumAttValue ("public", attrs, YESNO_VALUES, context);
    // default value depends on the parent:
    // "yes" (true) for top-level templates,
    // "no" (false) for others
    final boolean isPublic = parent instanceof TransformFactory.Instance ? (publicAttVal != NO_VALUE) // default
                                                                                                      // is
                                                                                                      // true
                                                                         : (publicAttVal == YES_VALUE); // default
                                                                                                        // is
                                                                                                        // false

    // default is "no" (false)
    final boolean newScope = getEnumAttValue ("new-scope", attrs, YESNO_VALUES, context) == YES_VALUE;

    checkAttributes (qName, attrs, attrNames, context);

    return new Instance (qName,
                         (AbstractGroupBase) parent,
                         context,
                         matchPattern,
                         priority,
                         visibility,
                         isPublic,
                         newScope);
  }

  // -----------------------------------------------------------------------

  /** The inner Instance class */
  public final class Instance extends AbstractTemplateBase implements Comparable <Instance>
  {
    /** The match pattern */
    private AbstractTree match;

    /** The priority of this template */
    private double priority;

    //
    // Constructor
    //
    protected Instance (final String qName,
                        final AbstractGroupBase parent,
                        final ParseContext context,
                        final AbstractTree match,
                        final double priority,
                        final int visibility,
                        final boolean isPublic,
                        final boolean newScope)
    {
      super (qName, parent, context, visibility, isPublic, newScope);
      this.match = match;
      this.priority = priority;
    }

    /**
     * @param context
     *        the Context object
     * @param setPosition
     *        <code>true</code> if the context position
     *        ({@link Context#position}) should be set in case the event stack
     *        matches the pattern in {@link #match}.
     * @return true if the current event stack matches the pattern of this
     *         template
     * @exception SAXParseException
     *            if an error occured while evaluating the match expression
     */
    public boolean matches (final Context context, final boolean setPosition) throws SAXException
    {
      context.currentInstruction = this;
      context.currentGroup = m_aParentGroup;
      return match.matches (context, context.ancestorStack.size (), setPosition);
    }

    /**
     * Splits a match pattern that is a union into several template instances.
     * The match pattern of the object itself loses one union.
     *
     * @return a template Instance object without a union in its match pattern
     *         or <code>null</code>
     */
    public Instance split () throws SAXException
    {
      if (match.m_nType != AbstractTree.UNION)
        return null;

      Instance copy = null;
      try
      {
        copy = (Instance) clone ();
      }
      catch (final CloneNotSupportedException e)
      {
        throw new SAXException ("Can't split " + this, e);
      }
      copy.match = match.m_aRight; // non-union
      if (Double.isNaN (copy.priority)) // no priority specified
        copy.priority = copy.match.getPriority ();
      match = match.m_aLeft; // may contain another union
      if (Double.isNaN (priority)) // no priority specified
        priority = match.getPriority ();
      return copy;
    }

    /**
     * @return the priority of this template
     */
    public double getPriority ()
    {
      return priority;
    }

    /**
     * @return the match pattern
     */
    public AbstractTree getMatchPattern ()
    {
      return match;
    }

    /**
     * Compares two templates according to their inverse priorities. This
     * results in a descending natural order with java.util.Arrays.sort()
     */
    public int compareTo (final Instance o)
    {
      final double p = o.priority;
      return (p < priority) ? -1 : ((p > priority) ? 1 : 0);
    }

    @Override
    protected void onDeepCopy (final AbstractInstruction copy, final HashMap <Object, Object> copies)
    {
      super.onDeepCopy (copy, copies);
      final Instance theCopy = (Instance) copy;
      if (match != null)
        theCopy.match = match.deepCopy (copies);
    }

    // for debugging
    @Override
    public String toString ()
    {
      return "template:" + lineNo + " " + match + " " + priority;
    }
  }
}
