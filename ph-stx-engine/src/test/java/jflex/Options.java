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
package jflex;

import java.io.File;

/**
 * Collects all global JFlex options. Can be set from command line parser, ant
 * taks, gui, etc.
 *
 * @author Gerwin Klein
 * @version JFlex 1.6.1
 */
public class Options
{

  /**
   * If true, additional verbose debug information is produced. This is a
   * compile time option.
   */
  public final static boolean DEBUG = false;

  /** output directory */
  private static File directory;
  /** strict JLex compatibility */
  public static boolean jlex;
  /** don't run minimization algorithm if this is true */
  public static boolean no_minimize;
  /** don't write backup files if this is true */
  public static boolean no_backup;
  /** If false, only error/warning output will be generated */
  public static boolean verbose;
  /** Whether to warn about unused macros. */
  public static boolean unused_warning;
  /** If true, progress dots will be printed */
  public static boolean progress;
  /** If true, jflex will print time statistics about the generation process */
  public static boolean time;
  /** If true, jflex will write graphviz .dot files for generated automata */
  public static boolean dot;
  /** If true, you will be flooded with information (e.g. dfa tables). */
  public static boolean dump;
  /**
   * If true, dot (.) metachar matches [^\n] instead of
   * [^\r\n\u000B\u000C\u0085\u2028\u2029]|"\r\n"
   */
  public static boolean legacy_dot;
  /**
   * If true, the generated scanner will include a constructor taking an
   * InputStream.
   */
  public static boolean emitInputStreamCtor;

  static
  {
    setDefaults ();
  }

  /**
   * @return the output directory
   */
  public static File getDir ()
  {
    return directory;
  }

  /**
   * Set output directory
   *
   * @param dirName
   *        the name of the directory to write output files to
   */
  public static void setDir (final String dirName)
  {
    setDir (new File (dirName));
  }

  /**
   * Set output directory
   * 
   * @param d
   *        the directory to write output files to
   */
  public static void setDir (final File d)
  {
    if (d.isFile ())
    {
      Out.error ("Error: \"" + d + "\" is not a directory.");
      throw new GeneratorException ();
    }

    if (!d.isDirectory () && !d.mkdirs ())
    {
      Out.error ("Error: couldn't create directory \"" + d + "\"");
      throw new GeneratorException ();
    }

    directory = d;
  }

  /**
   * Sets all options back to default values.
   */
  public static void setDefaults ()
  {
    directory = null;
    jlex = false;
    no_minimize = false;
    no_backup = false;
    verbose = true;
    progress = true;
    unused_warning = true;
    time = false;
    dot = false;
    dump = false;
    legacy_dot = false;
    // TODO: in the JFlex version after 1.6, the emitInputStreamCtor option will
    // cease to exist.
    emitInputStreamCtor = false;
    Skeleton.readDefault ();
  }

  public static void setSkeleton (final File skel)
  {
    Skeleton.readSkelFile (skel);
  }
}
