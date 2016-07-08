/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.6.1                                                             *
 * Copyright (C) 1998-2015  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * License: BSD                                                            *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package jflex;

/**
 * @author Gerwin Klein
 * @version JFlex 1.6.1
 */
public final class CharSet
{

  final static int BITS = 6; // the number of bits to shift (2^6 = 64)
  final static int MOD = (1 << BITS) - 1; // modulus

  long bits[];

  private int numElements;

  public CharSet ()
  {
    bits = new long [1];
  }

  public CharSet (final int initialSize, final int character)
  {
    bits = new long [(initialSize >> BITS) + 1];
    add (character);
  }

  public void add (final int character)
  {
    resize (character);

    if ((bits[character >> BITS] & (1L << (character & MOD))) == 0)
      numElements++;

    bits[character >> BITS] |= (1L << (character & MOD));
  }

  private int nbits2size (final int nbits)
  {
    return ((nbits >> BITS) + 1);
  }

  private void resize (final int nbits)
  {
    final int needed = nbits2size (nbits);

    if (needed < bits.length)
      return;

    final long newbits[] = new long [Math.max (bits.length * 2, needed)];
    System.arraycopy (bits, 0, newbits, 0, bits.length);

    bits = newbits;
  }

  public boolean isElement (final int character)
  {
    final int index = character >> BITS;
    if (index >= bits.length)
      return false;
    return (bits[index] & (1L << (character & MOD))) != 0;
  }

  public CharSetEnumerator characters ()
  {
    return new CharSetEnumerator (this);
  }

  public boolean containsElements ()
  {
    return numElements > 0;
  }

  public int size ()
  {
    return numElements;
  }

  @Override
  public String toString ()
  {
    final CharSetEnumerator set = characters ();

    final StringBuilder result = new StringBuilder ("{");

    if (set.hasMoreElements ())
      result.append ("").append (set.nextElement ());

    while (set.hasMoreElements ())
    {
      final int i = set.nextElement ();
      result.append (", ").append (i);
    }

    result.append ("}");

    return result.toString ();
  }
}