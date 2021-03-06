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
package java_cup;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class represents a terminal symbol in the grammar. Each terminal has a
 * textual name, an index, and a string which indicates the type of object it
 * will be implemented with at runtime (i.e. the class of object that will be
 * returned by the scanner and pushed on the parse stack to represent it).
 *
 * @version last updated: 7/3/96
 * @author Frank Flannery
 */
public class terminal extends symbol
{

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /**
   * Full constructor.
   *
   * @param nm
   *        the name of the terminal.
   * @param tp
   *        the type of the terminal.
   */
  public terminal (final String nm, final String tp, final int precedence_side, final int precedence_num)
  {
    /* superclass does most of the work */
    super (nm, tp);

    /* add to set of all terminals and check for duplicates */
    final Object conflict = _all.put (nm, this);
    if (conflict != null)
                         // can't throw an execption here because this is used
                         // in static
                         // initializers, so we do a crash instead
                         // was:
                         // throw new internal_error("Duplicate terminal (" + nm
                         // + ") created");
                         (new internal_error ("Duplicate terminal (" + nm + ") created")).crash ();

    /* assign a unique index */
    _index = next_index++;

    /* set the precedence */
    _precedence_num = precedence_num;
    _precedence_side = precedence_side;

    /* add to by_index set */
    _all_by_index.put (new Integer (_index), this);
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Constructor for non-precedented terminal
   */

  public terminal (final String nm, final String tp)
  {
    this (nm, tp, assoc.no_prec, -1);
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Constructor with default type.
   *
   * @param nm
   *        the name of the terminal.
   */
  public terminal (final String nm)
  {
    this (nm, null);
  }

  /*-----------------------------------------------------------*/
  /*-------------------  Class Variables  ---------------------*/
  /*-----------------------------------------------------------*/

  private int _precedence_num;
  private int _precedence_side;

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /**
   * Table of all terminals. Elements are stored using name strings as the key
   */
  protected static Hashtable <String, terminal> _all = new Hashtable <> ();

  // Hm Added clear to clear all static fields
  public static void clear ()
  {
    _all.clear ();
    _all_by_index.clear ();
    next_index = 0;
    EOF = new terminal ("EOF");
    error = new terminal ("error");
  }

  /** Access to all terminals. */
  public static Enumeration <terminal> all ()
  {
    return _all.elements ();
  }

  /** Lookup a terminal by name string. */
  public static terminal find (final String with_name)
  {
    if (with_name == null)
      return null;
    return _all.get (with_name);
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Table of all terminals indexed by their index number. */
  protected static Hashtable <Integer, terminal> _all_by_index = new Hashtable <> ();

  /** Lookup a terminal by index. */
  public static terminal find (final int indx)
  {
    final Integer the_indx = new Integer (indx);

    return _all_by_index.get (the_indx);
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Total number of terminals. */
  public static int number ()
  {
    return _all.size ();
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Static counter to assign unique index. */
  protected static int next_index = 0;

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Special terminal for end of input. */
  public static terminal EOF = new terminal ("EOF");

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** special terminal used for error recovery */
  public static terminal error = new terminal ("error");

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Report this symbol as not being a non-terminal. */
  @Override
  public boolean is_non_term ()
  {
    return false;
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Convert to a string. */
  @Override
  public String toString ()
  {
    return super.toString () + "[" + index () + "]";
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** get the precedence of a terminal */
  public int precedence_num ()
  {
    return _precedence_num;
  }

  public int precedence_side ()
  {
    return _precedence_side;
  }

  /** set the precedence of a terminal */
  public void set_precedence (final int p, final int new_prec)
  {
    _precedence_side = p;
    _precedence_num = new_prec;
  }

  /*-----------------------------------------------------------*/

}
