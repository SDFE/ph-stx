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
package net.sf.joost.emitter;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import net.sf.joost.CSTX;

/**
 * This class implements an emitter that uses the <code>xml</code> output method
 * for byte or character streams.
 *
 * @version $Revision: 1.9 $ $Date: 2008/10/06 13:31:41 $
 * @author Oliver Becker, Anatolij Zubow
 */
public class XmlEmitter extends AbstractStreamEmitter
{
  // Logger initialization
  private static final Logger log = LoggerFactory.getLogger (XmlEmitter.class);

  /** output property: omit-xml-declaration */
  private boolean propOmitXmlDeclaration = false;

  /** output property: standalone */
  private boolean propStandalone = false;

  /** output property: version */
  private String propVersion = "1.0";

  /** string buffer for namespace declarations */
  private final StringBuffer nsDeclarations = new StringBuffer ();

  /** qName of the previous element */
  private String lastQName;

  /** attributes of the previous element */
  private Attributes lastAttrs;

  /** flag indicating if we're within a CDATA section */
  private boolean insideCDATA = false;

  /** flag indicating if disable output escaping will be supported */
  private boolean supportDisableOutputEscaping = false;

  /** flag indicating if disabled output escaping is active */
  private boolean disabledOutputEscaping = false;

  /** Constructor */
  public XmlEmitter (final Writer writer, final String encoding, final Properties outputProperties)
  {
    super (writer, encoding);

    if (outputProperties != null)
    {
      String val;
      val = outputProperties.getProperty (OutputKeys.OMIT_XML_DECLARATION);
      if (val != null)
        propOmitXmlDeclaration = val.equals ("yes");
      if (!encoding.equals ("UTF-8") && !encoding.equals ("UTF-16"))
        propOmitXmlDeclaration = false;

      val = outputProperties.getProperty (OutputKeys.STANDALONE);
      if (val != null)
        propStandalone = val.equals ("yes");

      val = outputProperties.getProperty (OutputKeys.VERSION);
      if (val != null)
        propVersion = val;
    }
  }

  /**
   * Defines whether the XML declaration should be omitted, default is
   * <code>false</code>.
   *
   * @param flag
   *        <code>true</code>: the XML declaration will be omitted;
   *        <code>false</code>: the XML declaration will be output
   */
  @Override
  public void setOmitXmlDeclaration (final boolean flag)
  {
    propOmitXmlDeclaration = flag;
  }

  /**
   * Defines whether disable-output-escaping will be supported (means whether
   * the corresponding processing instructions
   * {@link Result#PI_DISABLE_OUTPUT_ESCAPING} and
   * {@link Result#PI_ENABLE_OUTPUT_ESCAPING} will be interpreted). The default
   * is <code>false</code>
   *
   * @param flag
   *        <code>true</code> the PIs will be interpreted; <code>false</code>
   *        the PIs will be written literally
   */
  @Override
  public void setSupportDisableOutputEscaping (final boolean flag)
  {
    this.supportDisableOutputEscaping = flag;
  }

  /**
   * Outputs a start or empty element tag if there is one stored.
   *
   * @param end
   *        true if this method was called due to an endElement event, i.e. an
   *        empty element tag has to be output.
   * @return true if something was output (needed for endElement to determine,
   *         if a separate end tag must be output)
   */
  private boolean processLastElement (final boolean end) throws SAXException
  {

    if (lastQName != null)
    {
      final StringBuffer out = new StringBuffer ("<");
      out.append (lastQName);
      out.append (nsDeclarations);
      nsDeclarations.setLength (0);

      // attributes
      final int length = lastAttrs.getLength ();
      for (int i = 0; i < length; i++)
      {
        out.append (' ').append (lastAttrs.getQName (i)).append ("=\"");
        final char [] attChars = lastAttrs.getValue (i).toCharArray ();

        // output escaping
        for (int j = 0; j < attChars.length; j++)
          switch (attChars[j])
          {
            case '&':
              out.append ("&amp;");
              break;
            case '<':
              out.append ("&lt;");
              break;
            case '>':
              out.append ("&gt;");
              break;
            case '\"':
              out.append ("&quot;");
              break;
            case '\t':
              out.append ("&#x9;");
              break;
            case '\n':
              out.append ("&#xA;");
              break;
            case '\r':
              out.append ("&#xD;");
              break;
            default:
              j = encodeCharacters (attChars, j, out);
          }
        out.append ('\"');
      }

      out.append (end ? " />" : ">");

      try
      {
        // stream string to writer
        m_aWriter.write (out.toString ());
        if (CSTX.DEBUG)
          log.debug (out.toString ());
      }
      catch (final IOException ex)
      {
        log.error ("Exception", ex);
        throw new SAXException (ex);
      }

      lastQName = null;
      return true;
    }
    return false;
  }

  /**
   * SAX2-Callback - Outputs XML-Deklaration with encoding.
   */
  public void startDocument () throws SAXException
  {
    if (propOmitXmlDeclaration)
      return;

    try
    {
      m_aWriter.write ("<?xml version=\"");
      m_aWriter.write (propVersion);
      m_aWriter.write ("\" encoding=\"");
      m_aWriter.write (m_sEncoding);
      if (propStandalone)
        m_aWriter.write ("\" standalone=\"yes");
      m_aWriter.write ("\"?>\n");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }

  /**
   * SAX2-Callback - Flushes the output writer
   */
  public void endDocument () throws SAXException
  {
    processLastElement (false);

    try
    {
      m_aWriter.write ("\n");
      m_aWriter.flush ();
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }

  /**
   * SAX2-Callback
   */
  public void startElement (final String uri,
                            final String lName,
                            final String qName,
                            final Attributes attrs) throws SAXException
  {
    processLastElement (false);
    this.lastQName = qName;
    this.lastAttrs = attrs;
  }

  /**
   * SAX2-Callback - Outputs the element-tag.
   */
  public void endElement (final String uri, final String lName, final String qName) throws SAXException
  {
    // output end tag only if processLastElement didn't output
    // something (here: empty element tag)
    if (processLastElement (true) == false)
    {
      try
      {
        m_aWriter.write ("</");
        m_aWriter.write (qName);
        m_aWriter.write (">");
      }
      catch (final IOException ex)
      {
        log.error ("Exception", ex);
        throw new SAXException (ex);
      }
    }
  }

  /**
   * SAX2-Callback - Constructs characters.
   */
  public void characters (final char [] ch, final int start, final int length) throws SAXException
  {
    processLastElement (false);

    try
    {
      if (insideCDATA || disabledOutputEscaping)
      {
        // check that the characters can be represented in the current
        // encoding (escaping not possible within CDATA)
        for (int i = 0; i < length; i++)
          if (!m_aCharsetEncoder.canEncode (ch[start + i]))
            throw new SAXException ("Cannot output character with code " +
                                    (int) ch[start + i] +
                                    " in the encoding '" +
                                    m_sEncoding +
                                    "' within a CDATA section");
        m_aWriter.write (ch, start, length);
      }
      else
      {
        final StringBuffer out = new StringBuffer (length);
        // output escaping
        for (int i = 0; i < length; i++)
          switch (ch[start + i])
          {
            case '&':
              out.append ("&amp;");
              break;
            case '<':
              out.append ("&lt;");
              break;
            case '>':
              out.append ("&gt;");
              break;
            default:
              i = encodeCharacters (ch, start + i, out) - start;
          }
        m_aWriter.write (out.toString ());
      }
      if (CSTX.DEBUG)
        log.debug ("'" + new String (ch, start, length) + "'");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }

  /**
   * SAX2-Callback
   */
  @Override
  public void startPrefixMapping (final String prefix, final String uri) throws SAXException
  {
    processLastElement (false);

    if ("".equals (prefix))
      nsDeclarations.append (" xmlns=\"");
    else
      nsDeclarations.append (" xmlns:").append (prefix).append ("=\"");
    nsDeclarations.append (uri).append ('\"');
  }

  /**
   * SAX2-Callback - Outputs a PI
   */
  @Override
  public void processingInstruction (final String target, final String data) throws SAXException
  {
    processLastElement (false);

    if (supportDisableOutputEscaping)
    {
      if (Result.PI_DISABLE_OUTPUT_ESCAPING.equals (target))
      {
        disabledOutputEscaping = true;
        return;
      }
      else
        if (Result.PI_ENABLE_OUTPUT_ESCAPING.equals (target))
        {
          disabledOutputEscaping = false;
          return;
        }
    }

    try
    {
      m_aWriter.write ("<?");
      m_aWriter.write (target);

      if (!data.equals (""))
      {
        m_aWriter.write (" ");
        m_aWriter.write (data);
      }

      m_aWriter.write ("?>");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }

  /**
   * SAX2-Callback - Notify the start of a CDATA section
   */
  @Override
  public void startCDATA () throws SAXException
  {
    processLastElement (false);

    try
    {
      m_aWriter.write ("<![CDATA[");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }

    insideCDATA = true;
  }

  /**
   * SAX2-Callback - Notify the end of a CDATA section
   */
  @Override
  public void endCDATA () throws SAXException
  {
    insideCDATA = false;
    try
    {
      m_aWriter.write ("]]>");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }

  /**
   * SAX2-Callback - Outputs a comment
   */
  @Override
  public void comment (final char [] ch, final int start, final int length) throws SAXException
  {
    processLastElement (false);

    try
    {
      m_aWriter.write ("<!--");
      m_aWriter.write (ch, start, length);
      m_aWriter.write ("-->");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }

  /**
   * SAX2-Callback - Outputs a document type declaration
   */
  @Override
  public void startDTD (final String name, final String publicId, final String systemId) throws SAXException
  {
    try
    {
      m_aWriter.write ("<!DOCTYPE ");
      m_aWriter.write (name);
      if (publicId != null)
      {
        m_aWriter.write (" PUBLIC \"");
        m_aWriter.write (publicId);
        m_aWriter.write ("\" \"");
        if (systemId != null)
        {
          m_aWriter.write (systemId);
        }
        m_aWriter.write ("\"");
      }
      else
        if (systemId != null)
        {
          m_aWriter.write (" SYSTEM \"");
          m_aWriter.write (systemId);
          m_aWriter.write ("\"");
        }
      // internal subset not supported yet
      m_aWriter.write (">\n");
    }
    catch (final IOException ex)
    {
      log.error ("Exception", ex);
      throw new SAXException (ex);
    }
  }
}
