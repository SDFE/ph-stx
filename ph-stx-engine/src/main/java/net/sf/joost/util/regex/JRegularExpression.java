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
/*
 * $Id: JRegularExpression.java,v 1.2 2007/06/13 20:29:08 obecker Exp $
 *
 * Copied from Michael Kay's Saxon 8.9
 * Local changes (excluding package declarations and imports) marked as // OB
 */

package net.sf.joost.util.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.joost.grammar.EvalException;

/**
 * A compiled regular expression implemented using the Java JDK regex package
 */
public class JRegularExpression implements IRegularExpression
{
  private Pattern m_aPattern;
  private String m_sJavaRegex;
  private final int m_nFlagBits;

  /**
   * Create a regular expression, starting with an already-translated Java regex
   */

  public JRegularExpression (final String javaRegex, final int flagBits)
  {
    this.m_nFlagBits = flagBits;
    this.m_sJavaRegex = javaRegex;
    m_aPattern = Pattern.compile (javaRegex, flagBits & (~(Pattern.COMMENTS | Pattern.CASE_INSENSITIVE)));
  }

  /**
   * Create (compile) a regular expression
   *
   * @param regex
   *        the source text of the regular expression, in XML Schema or XPath
   *        syntax
   * @param isXPath
   *        set to true if this is an XPath regular expression, false if it is
   *        XML Schema
   * @param flagBits
   *        the flags argument translated to the Java bit-significant integer
   *        representation
   * @throws EvalException
   *         if the syntax of the regular expression or flags is incorrect
   */
  public JRegularExpression (final CharSequence regex, final boolean isXPath, final int flagBits) throws EvalException
  {
    this.m_nFlagBits = flagBits;
    try
    {
      final boolean ignoreWhitespace = ((flagBits & Pattern.COMMENTS) != 0);
      final boolean caseBlind = ((flagBits & Pattern.CASE_INSENSITIVE) != 0);
      m_sJavaRegex = JDK15RegexTranslator.translate (regex, isXPath, ignoreWhitespace, caseBlind);
      m_aPattern = Pattern.compile (m_sJavaRegex, flagBits & (~(Pattern.COMMENTS | Pattern.CASE_INSENSITIVE)));
    }
    catch (final RegexSyntaxException e)
    {
      throw new EvalException (e.getMessage ());
    }
  }

  // OB: new constructor
  public JRegularExpression (final CharSequence regex, final boolean isXPath, final String flags) throws EvalException
  {
    this (regex, isXPath, setFlags (flags));
  }

  /**
   * @return the Java regular expression (after translation from an XPath regex,
   *         but before compilation)
   */
  public String getJavaRegularExpression ()
  {
    return m_sJavaRegex;
  }

  /**
   * @return the flag bits as used by the Java regular expression engine
   */
  public int getFlagBits ()
  {
    return m_nFlagBits;
  }

  // OB: commented out analyze()
  // /**
  // * Use this regular expression to analyze an input string, in support of the
  // XSLT
  // * analyze-string instruction. The resulting RegexIterator provides both the
  // matching and
  // * non-matching substrings, and allows them to be distinguished. It also
  // provides access
  // * to matched subgroups.
  // */
  //
  // public RegexIterator analyze(CharSequence input) {
  // return new JRegexIterator(input.toString(), pattern);
  // }

  // OB: added matcher()
  /**
   * @see net.sf.joost.util.regex.IRegularExpression#matcher(java.lang.CharSequence)
   */
  public Matcher matcher (final CharSequence input)
  {
    return m_aPattern.matcher (input);
  }

  /**
   * Determine whether the regular expression contains a match for a given
   * string
   *
   * @param input
   *        the string to match
   * @return true if the string matches, false otherwise
   */

  public boolean containsMatch (final CharSequence input)
  {
    return m_aPattern.matcher (input).find ();
  }

  /**
   * Determine whether the regular expression match a given string in its
   * entirety
   *
   * @param input
   *        the string to match
   * @return true if the string matches, false otherwise
   */

  public boolean matches (final CharSequence input)
  {
    return m_aPattern.matcher (input).matches ();
  }

  /**
   * Replace all substrings of a supplied input string that match the regular
   * expression with a replacement string.
   *
   * @param input
   *        the input string on which replacements are to be performed
   * @param replacement
   *        the replacement string in the format of the XPath replace() function
   * @return the result of performing the replacement
   * @throws EvalException
   *         if the replacement string is invalid
   */

  // OB: changed thrown exception
  // public CharSequence replace(CharSequence input, CharSequence replacement)
  // throws XPathException {
  public CharSequence replace (final CharSequence input, final CharSequence replacement) throws EvalException
  {
    final Matcher matcher = m_aPattern.matcher (input);
    try
    {
      final String res = matcher.replaceAll (replacement.toString ());
      return res;
    }
    catch (final IndexOutOfBoundsException e)
    {
      // this occurs if the replacement string references a group $n and there
      // are less than n
      // capturing subexpressions in the regex. In this case we're supposed to
      // replace $n by an
      // empty string. We do this by modifying the replacement string.
      final int gps = matcher.groupCount ();
      if (gps >= 9)
      {
        // don't know what's gone wrong here
        throw e;
      }
      final String r = replacement.toString ();
      // remove occurrences of $n from the replacement string, if n is greater
      // than the number of groups
      final String f = "\\$[" + (gps + 1) + "-9]";
      final String rep = Pattern.compile (f).matcher (r).replaceAll ("");
      final String res = matcher.replaceAll (rep);
      return res;
    }

  }

  // OB: commented out tokenize()
  // /**
  // * Use this regular expression to tokenize an input string.
  // *
  // * @param input the string to be tokenized
  // * @return a SequenceIterator containing the resulting tokens, as objects of
  // type StringValue
  // */
  //
  // public SequenceIterator tokenize(CharSequence input) {
  // if (input.length() == 0) {
  // return EmptyIterator.getInstance();
  // }
  // return new JTokenIterator(input, pattern);
  // }

  /**
   * Set the Java flags from the supplied XPath flags.
   *
   * @param inFlags
   *        the flags as a string, e.g. "im"
   * @return the flags as a bit-significant integer
   * @throws EvalException
   *         if the supplied value is invalid
   */

  // OB: changed thrown exception
  // public static int setFlags(CharSequence inFlags) throws DynamicError {
  public static int setFlags (final CharSequence inFlags) throws EvalException
  {
    int flags = Pattern.UNIX_LINES;
    for (int i = 0; i < inFlags.length (); i++)
    {
      final char c = inFlags.charAt (i);
      switch (c)
      {
        case 'm':
          flags |= Pattern.MULTILINE;
          break;
        case 'i':
          flags |= Pattern.CASE_INSENSITIVE;
          flags |= Pattern.UNICODE_CASE;
          break;
        case 's':
          flags |= Pattern.DOTALL;
          break;
        case 'x':
          flags |= Pattern.COMMENTS; // note, this enables comments as well as
                                     // whitespace
          break;
        default:
          // OB: throw different exception
          // DynamicError err = new DynamicError("Invalid character '" + c + "'
          // in regular expression flags");
          // err.setErrorCode("FORX0001");
          // throw err;
          throw new EvalException ("Invalid character '" + c + "' in regular expression flags");
      }
    }
    return flags;
  }

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
