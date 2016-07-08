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
 * An emitter for an array encoded as count/value pairs in a string.
 *
 * @author Gerwin Klein
 * @version JFlex 1.6.1
 */
public class CountEmitter extends PackEmitter
{
  /** number of entries in expanded array */
  private int numEntries;

  /** translate all values by this amount */
  private int translate = 0;

  /**
   * Create a count/value emitter for a specific field.
   *
   * @param name
   *        name of the generated array
   */
  protected CountEmitter (final String name)
  {
    super (name);
  }

  /**
   * Emits count/value unpacking code for the generated array.
   *
   * @see jflex.PackEmitter#emitUnpack()
   */
  @Override
  public void emitUnpack ()
  {
    // close last string chunk:
    println ("\";");

    nl ();
    println ("  private static int [] zzUnpack" + name + "() {");
    println ("    int [] result = new int[" + numEntries + "];");
    println ("    int offset = 0;");

    for (int i = 0; i < chunks; i++)
    {
      println ("    offset = zzUnpack" + name + "(" + constName () + "_PACKED_" + i + ", offset, result);");
    }

    println ("    return result;");
    println ("  }");
    nl ();

    println ("  private static int zzUnpack" + name + "(String packed, int offset, int [] result) {");
    println ("    int i = 0;       /* index in packed string  */");
    println ("    int j = offset;  /* index in unpacked array */");
    println ("    int l = packed.length();");
    println ("    while (i < l) {");
    println ("      int count = packed.charAt(i++);");
    println ("      int value = packed.charAt(i++);");
    if (translate == 1)
    {
      println ("      value--;");
    }
    else
      if (translate != 0)
      {
        println ("      value-= " + translate);
      }
    println ("      do result[j++] = value; while (--count > 0);");
    println ("    }");
    println ("    return j;");
    println ("  }");
  }

  /**
   * Translate all values by given amount. Use to move value interval from [0,
   * 0xFFFF] to something different.
   *
   * @param i
   *        amount the value will be translated by. Example: <code>i = 1</code>
   *        allows values in [-1, 0xFFFE].
   */
  public void setValTranslation (final int i)
  {
    this.translate = i;
  }

  /**
   * Emit one count/value pair. Automatically translates value by the
   * <code>translate</code> value.
   *
   * @param count
   * @param value
   * @see CountEmitter#setValTranslation(int)
   */
  public void emit (final int count, final int value)
  {
    numEntries += count;
    breaks ();
    emitUC (count);
    emitUC (value + translate);
  }
}
