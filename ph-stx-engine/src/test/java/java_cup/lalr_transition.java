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

/**
 * This class represents a transition in an LALR viable prefix recognition
 * machine. Transitions can be under terminals for non-terminals. They are
 * internally linked together into singly linked lists containing all the
 * transitions out of a single state via the _next field.
 *
 * @see java_cup.lalr_state
 * @version last updated: 11/25/95
 * @author Scott Hudson
 */
public class lalr_transition
{

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /**
   * Full constructor.
   * 
   * @param on_sym
   *        symbol we are transitioning on.
   * @param to_st
   *        state we transition to.
   * @param nxt
   *        next transition in linked list.
   */
  public lalr_transition (final symbol on_sym, final lalr_state to_st, final lalr_transition nxt) throws internal_error
  {
    /* sanity checks */
    if (on_sym == null)
      throw new internal_error ("Attempt to create transition on null symbol");
    if (to_st == null)
      throw new internal_error ("Attempt to create transition to null state");

    /* initialize */
    _on_symbol = on_sym;
    _to_state = to_st;
    _next = nxt;
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Constructor with null next.
   * 
   * @param on_sym
   *        symbol we are transitioning on.
   * @param to_st
   *        state we transition to.
   */
  public lalr_transition (final symbol on_sym, final lalr_state to_st) throws internal_error
  {
    this (on_sym, to_st, null);
  }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The symbol we make the transition on. */
  protected symbol _on_symbol;

  /** The symbol we make the transition on. */
  public symbol on_symbol ()
  {
    return _on_symbol;
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** The state we transition to. */
  protected lalr_state _to_state;

  /** The state we transition to. */
  public lalr_state to_state ()
  {
    return _to_state;
  }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Next transition in linked list of transitions out of a state */
  protected lalr_transition _next;

  /** Next transition in linked list of transitions out of a state */
  public lalr_transition next ()
  {
    return _next;
  }

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Convert to a string. */
  @Override
  public String toString ()
  {
    String result;

    result = "transition on " + on_symbol ().name () + " to state [";
    result += _to_state.index ();
    result += "]";

    return result;
  }

  /*-----------------------------------------------------------*/
}
