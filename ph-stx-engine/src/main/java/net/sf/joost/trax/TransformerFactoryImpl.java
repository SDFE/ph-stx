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
package net.sf.joost.trax;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import net.sf.joost.CSTX;
import net.sf.joost.IOutputURIResolver;
import net.sf.joost.ITransformerHandlerResolver;
import net.sf.joost.emitter.AbstractStreamEmitter;
import net.sf.joost.emitter.IStxEmitter;
import net.sf.joost.stx.Processor;
import net.sf.joost.trace.ParserListenerMgr;

/**
 * This class implements the TransformerFactory-Interface for TraX. With the
 * help of this factory you can get a templates-object or directly a
 * transformer-object for the transformation process. If you use a SAXResult you
 * can simply downcast to SAXTransformerFactory and use it like a Sax-Parser.
 *
 * @author Zubow
 */
public class TransformerFactoryImpl extends SAXTransformerFactory
{

  // Define a static logger variable so that it references the
  // Logger instance named "TransformerFactoryImpl".
  private static final Logger log = LoggerFactory.getLogger (TransformerFactoryImpl.class);

  // Member
  private URIResolver m_aURIResolver;
  private ErrorListener m_aErrorListener;
  protected ITransformerHandlerResolver m_aTHResolver;
  protected IOutputURIResolver m_aOutputUriResolver;
  protected boolean m_bAllowExternalFunctions = true;

  // init default errorlistener
  // visible for TemplatesImpl
  protected ConfigurationErrListener m_aDefaultErrorListener = new ConfigurationErrListener ();

  // indicates if the transformer is working in debug mode
  private boolean m_bDebugmode = false;

  // indicates which Emitter class for stx:message output should be used
  private IStxEmitter m_aMsgEmitter;

  // Synch object to guard against setting values from the TrAX interface
  // or reentry while the transform is going on.
  private final Boolean m_aReentryGuard = new Boolean (true);

  /**
   * The parserlistener manager for tracing purpose.
   */
  private final ParserListenerMgr m_aParserListenerMgr = new ParserListenerMgr ();

  /**
   * The default constructor.
   */
  public TransformerFactoryImpl ()
  {
    try
    {
      // initialize default messageEmitter
      m_aMsgEmitter = AbstractStreamEmitter.newEmitter (System.err, null);
      ((AbstractStreamEmitter) m_aMsgEmitter).setOmitXmlDeclaration (true);
    }
    catch (final UnsupportedEncodingException e)
    {
      // must not and cannot happen since outputProperties was null
      throw new TransformerFactoryConfigurationError (e);
    }
  }

  // *************************************************************************
  // IMPLEMENTATION OF TransformerFactory
  // *************************************************************************

  /**
   * Returns the <code>Source</code> of the stylesheet associated with the
   * xml-document. Feature is not supported.
   *
   * @param source
   *        The <code>Source</code> of the xml-document.
   * @param media
   *        Matching media-type.
   * @param title
   *        Matching title-type.
   * @param charset
   *        Matching charset-type.
   * @return A <code>Source</code> of the stylesheet.
   * @throws TransformerConfigurationException
   */
  @Override
  public Source getAssociatedStylesheet (final Source source,
                                         final String media,
                                         final String title,
                                         final String charset) throws TransformerConfigurationException
  {

    final TransformerConfigurationException tE = new TransformerConfigurationException ("Feature not supported");

    m_aDefaultErrorListener.fatalError (tE);
    return null;
  }

  /**
   * Allows the user to retrieve specific attributes of the underlying
   * implementation.
   *
   * @param name
   *        The attribute name.
   * @return An object according to the attribute-name
   * @throws IllegalArgumentException
   *         When such a attribute does not exists.
   */
  @Override
  public Object getAttribute (final String name) throws IllegalArgumentException
  {

    if (CTrAX.KEY_TH_RESOLVER.equals (name))
      return m_aTHResolver;
    if (CTrAX.KEY_OUTPUT_URI_RESOLVER.equals (name))
      return m_aOutputUriResolver;
    if (CTrAX.MESSAGE_EMITTER_CLASS.equals (name))
      return m_aMsgEmitter;
    if (CTrAX.KEY_XSLT_FACTORY.equals (name))
      return System.getProperty (CTrAX.KEY_XSLT_FACTORY);
    if (CTrAX.ALLOW_EXTERNAL_FUNCTIONS.equals (name))
      return Boolean.valueOf (m_bAllowExternalFunctions);
    if (CTrAX.DEBUG_FEATURE.equals (name))
      return Boolean.valueOf (m_bDebugmode);

    log.warn ("Feature not supported: " + name);
    throw new IllegalArgumentException ("Feature not supported: " + name);
  }

  /**
   * Allows the user to set specific attributes on the underlying
   * implementation. An attribute in this context is defined to be an option
   * that the implementation provides.
   *
   * @param name
   *        Name of the attribute (key)
   * @param value
   *        Value of the attribute.
   * @throws IllegalArgumentException
   */
  @Override
  public void setAttribute (final String name, final Object value) throws IllegalArgumentException
  {

    if (CTrAX.KEY_TH_RESOLVER.equals (name))
    {
      m_aTHResolver = (ITransformerHandlerResolver) value;
    }
    else
      if (CTrAX.KEY_OUTPUT_URI_RESOLVER.equals (name))
      {
        m_aOutputUriResolver = (IOutputURIResolver) value;
      }
      else
        if (CTrAX.MESSAGE_EMITTER_CLASS.equals (name))
        {
          // object is of type string, so use reflection
          if (value instanceof String)
          {
            try
            {
              m_aMsgEmitter = buildMessageEmitter ((String) value);
            }
            catch (final TransformerConfigurationException e)
            {
              log.error (e.getMessage (), e);
              throw new IllegalArgumentException (e.getMessage ());
            }
          }
          else
            if (value instanceof IStxEmitter)
            { // already instantiated
              m_aMsgEmitter = (IStxEmitter) value;
            }
            else
            {
              throw new IllegalArgumentException ("Emitter is of wrong type," +
                                                  "should be either a String or a StxEmitter");
            }
        }
        else
          if (CTrAX.KEY_XSLT_FACTORY.equals (name))
          {
            System.setProperty (CTrAX.KEY_XSLT_FACTORY, (String) value);
          }
          else
            if (CTrAX.ALLOW_EXTERNAL_FUNCTIONS.equals (name))
            {
              this.m_bAllowExternalFunctions = ((Boolean) value).booleanValue ();
            }
            else
              if (CTrAX.DEBUG_FEATURE.equals (name))
              {
                this.m_bDebugmode = ((Boolean) value).booleanValue ();
              }
              else
              {
                log.warn ("Feature not supported: " + name);
                throw new IllegalArgumentException ("Feature not supported: " + name);
              }
  }

  /**
   * Getter for {@link #m_aErrorListener}
   *
   * @return The registered <code>ErrorListener</code>
   */
  @Override
  public ErrorListener getErrorListener ()
  {
    return m_aErrorListener;
  }

  /**
   * Setter for {@link #m_aErrorListener}
   *
   * @param errorListener
   *        The <code>ErrorListener</code> object.
   * @throws IllegalArgumentException
   */
  @Override
  public void setErrorListener (final ErrorListener errorListener) throws IllegalArgumentException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
        log.debug ("setting ErrorListener");
      if (errorListener == null)
      {
        throw new IllegalArgumentException ("ErrorListener is null");
      }
      this.m_aErrorListener = errorListener;
      m_aDefaultErrorListener.setUserErrorListener (errorListener);
    }
  }

  /**
   * Getter for {@link #m_aURIResolver}
   *
   * @return The registered <code>URIResolver</code>
   */
  @Override
  public URIResolver getURIResolver ()
  {
    return m_aURIResolver;
  }

  /**
   * Setter for {@link #m_aURIResolver}
   *
   * @param resolver
   *        The <code>URIResolver</code> object.
   */
  @Override
  public void setURIResolver (final URIResolver resolver)
  {

    synchronized (m_aReentryGuard)
    {
      this.m_aURIResolver = resolver;
    }
  }

  /**
   * see
   * {@link javax.xml.transform.TransformerFactory#setFeature(java.lang.String, boolean)}
   */
  @Override
  public void setFeature (final String name, final boolean value) throws TransformerConfigurationException
  {
    // TODO compare with xalan/saxon
    throw new IllegalArgumentException ("Not yet implemented");
  }

  /**
   * Supplied features.
   *
   * @param name
   *        Name of the feature.
   * @return true if feature is supported.
   */
  @Override
  public boolean getFeature (final String name)
  {

    if (name.equals (SAXSource.FEATURE))
    {
      return true;
    }
    if (name.equals (SAXResult.FEATURE))
    {
      return true;
    }
    if (name.equals (DOMSource.FEATURE))
    {
      return true;
    }
    if (name.equals (DOMResult.FEATURE))
    {
      return true;
    }
    if (name.equals (StreamSource.FEATURE))
    {
      return true;
    }
    if (name.equals (StreamResult.FEATURE))
    {
      return true;
    }
    if (name.equals (SAXTransformerFactory.FEATURE))
    {
      return true;
    }
    if (name.equals (SAXTransformerFactory.FEATURE_XMLFILTER))
    {
      return true;
    }

    final String errMsg = "Unknown feature " + name;
    final TransformerConfigurationException tE = new TransformerConfigurationException (errMsg);

    try
    {
      m_aDefaultErrorListener.error (tE);
      return false;
    }
    catch (final TransformerException e)
    {
      throw new IllegalArgumentException (errMsg);
    }
  }

  /**
   * Creates a new Templates for Transformations.
   *
   * @param source
   *        The <code>Source</code> of the stylesheet.
   * @return A <code>Templates</code> object or <code>null</code> when an error
   *         occured (no user defined ErrorListener)
   * @throws TransformerConfigurationException
   */
  @Override
  public Templates newTemplates (final Source source) throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
      {
        if (log.isDebugEnabled ())
          log.debug ("get a Templates-instance from Source " + source.getSystemId ());
      }
      try
      {
        final SAXSource saxSource = TrAXHelper.getSAXSource (source, m_aErrorListener);
        final Templates template = new TemplatesImpl (saxSource.getXMLReader (), saxSource.getInputSource (), this);
        return template;
      }
      catch (final TransformerException tE)
      {
        m_aDefaultErrorListener.fatalError (tE);
        return null;
      }
    }
  }

  /**
   * Creates a new Transformer object that performs a copy of the source to the
   * result.
   *
   * @return A <code>Transformer</code> object for an identical transformation.
   * @throws TransformerConfigurationException
   */
  @Override
  public Transformer newTransformer () throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      final StreamSource streamSrc = new StreamSource (new StringReader (CTrAX.IDENTITY_TRANSFORM));
      return newTransformer (streamSrc);
    }
  }

  /**
   * Gets a new Transformer object for transformation.
   *
   * @param source
   *        The <code>Source</code> of the stylesheet.
   * @return A <code>Transformer</code> object according to the
   *         <code>Templates</code> object.
   * @throws TransformerConfigurationException
   */
  @Override
  public Transformer newTransformer (final Source source) throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
        log.debug ("get a Transformer-instance");
      final Templates templates = newTemplates (source);
      final Transformer transformer = templates.newTransformer ();
      return (transformer);
    }
  }

  // *************************************************************************
  // IMPLEMENTATION OF SAXTransformerFactory
  // *************************************************************************

  /**
   * Gets a <code>TemplatesHandler</code> object that can process SAX
   * ContentHandler events into a <code>Templates</code> object. Implementation
   * of the {@link SAXTransformerFactory}
   *
   * @see SAXTransformerFactory
   * @return {@link TemplatesHandler} ready to parse a stylesheet.
   * @throws TransformerConfigurationException
   */
  @Override
  public TemplatesHandler newTemplatesHandler () throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
        log.debug ("create a TemplatesHandler-instance");
      final TemplatesHandlerImpl thandler = new TemplatesHandlerImpl (this);
      return thandler;
    }
  }

  /**
   * Gets a <code>TransformerHandler</code> object that can process SAX
   * ContentHandler events into a Result. The transformation is defined as an
   * identity (or copy) transformation, for example to copy a series of SAX
   * parse events into a DOM tree. Implementation of the
   * {@link SAXTransformerFactory}
   *
   * @return {@link TransformerHandler} ready to transform SAX events.
   * @throws TransformerConfigurationException
   */
  @Override
  public TransformerHandler newTransformerHandler () throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
        log.debug ("get a TransformerHandler " + "(identity transformation or copy)");
      final StreamSource streamSrc = new StreamSource (new StringReader (CTrAX.IDENTITY_TRANSFORM));
      return newTransformerHandler (streamSrc);
    }
  }

  /**
   * Gets a <code>TransformerHandler</code> object that can process SAX
   * ContentHandler events into a Result, based on the transformation
   * instructions specified by the argument. Implementation of the
   * {@link SAXTransformerFactory}
   *
   * @param src
   *        The Source of the transformation instructions
   * @return {@link TransformerHandler} ready to transform SAX events.
   * @throws TransformerConfigurationException
   */
  @Override
  public TransformerHandler newTransformerHandler (final Source src) throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
        if (log.isDebugEnabled ())
          log.debug ("get a TransformerHandler-instance from Source " + src.getSystemId ());
      final Templates templates = newTemplates (src);
      return newTransformerHandler (templates);
    }
  }

  /**
   * Gets a <code>TransformerHandler</code> object that can process SAX
   * ContentHandler events into a Result, based on the Templates argument.
   * Implementation of the {@link SAXTransformerFactory}
   *
   * @param templates
   *        - The compiled transformation instructions.
   * @return {@link TransformerHandler} ready to transform SAX events.
   * @throws TransformerConfigurationException
   */
  @Override
  public TransformerHandler newTransformerHandler (final Templates templates) throws TransformerConfigurationException
  {

    synchronized (m_aReentryGuard)
    {
      if (CSTX.DEBUG)
        log.debug ("get a TransformerHandler-instance from Templates");
      final Transformer internal = templates.newTransformer ();
      final TransformerHandlerImpl thandler = new TransformerHandlerImpl (internal);
      return thandler;
    }
  }

  /**
   * Creates an <code>XMLFilter</code> that uses the given <code>Source</code>
   * as the transformation instructions. Implementation of the
   * {@link SAXTransformerFactory}
   *
   * @param src
   *        - The Source of the transformation instructions.
   * @return An {@link XMLFilter} object, or <code>null</code> if this feature
   *         is not supported.
   * @throws TransformerConfigurationException
   */
  @Override
  public XMLFilter newXMLFilter (final Source src) throws TransformerConfigurationException
  {

    if (CSTX.DEBUG)
      if (log.isDebugEnabled ())
        log.debug ("getting SAXTransformerFactory.FEATURE_XMLFILTER " + "from Source " + src.getSystemId ());
    XMLFilter xFilter = null;
    try
    {
      final Templates templates = newTemplates (src);
      // get a XMLReader
      final XMLReader parser = Processor.createXMLReader ();
      xFilter = newXMLFilter (templates);
      xFilter.setParent (parser);
      return xFilter;
    }
    catch (final SAXException ex)
    {
      final TransformerConfigurationException tE = new TransformerConfigurationException (ex.getMessage (), ex);
      m_aDefaultErrorListener.fatalError (tE);
      return null;
    }
  }

  /**
   * Creates an XMLFilter, based on the Templates argument. Implementation of
   * the {@link SAXTransformerFactory}
   *
   * @param templates
   *        - The compiled transformation instructions.
   * @return An {@link XMLFilter} object, or null if this feature is not
   *         supported.
   */
  @Override
  public XMLFilter newXMLFilter (final Templates templates)
  {

    if (CSTX.DEBUG)
      log.debug ("getting SAXTransformerFactory.FEATURE_XMLFILTER " + "from Templates");

    // Implementation
    return new TrAXFilter (templates);
  }

  /** returns the value of {@link #m_aParserListenerMgr} */
  public ParserListenerMgr getParserListenerMgr ()
  {
    return m_aParserListenerMgr;
  }

  /** returns the value of {@link #m_aMsgEmitter} */
  public IStxEmitter getMessageEmitter ()
  {
    return m_aMsgEmitter;
  }

  /**
   * Method creates a new Emitter for stx:message output
   *
   * @param emitterClass
   *        the name of the emitter class
   * @return a <code>StxEmitter</code>
   * @throws TransformerConfigurationException
   *         in case of errors
   */
  public IStxEmitter buildMessageEmitter (final String emitterClass) throws TransformerConfigurationException
  {

    Object emitter = null;
    try
    {
      emitter = loadClass (emitterClass).newInstance ();
      if (!(emitter instanceof IStxEmitter))
      {
        throw new TransformerConfigurationException (emitterClass + " is not an StxEmitter");
      }
    }
    catch (final InstantiationException ie)
    {
      throw new TransformerConfigurationException (ie.getMessage (), ie);
    }
    catch (final IllegalAccessException ile)
    {
      throw new TransformerConfigurationException (ile.getMessage (), ile);
    }

    return (IStxEmitter) emitter;
  }

  // classloader helper
  private Class <?> loadClass (final String className) throws TransformerConfigurationException
  {
    try
    {
      final ClassLoader loader = Thread.currentThread ().getContextClassLoader ();
      if (loader != null)
      {
        try
        {
          return loader.loadClass (className);
        }
        catch (final Exception ex)
        {
          return Class.forName (className);
        }
      }
      return Class.forName (className);
    }
    catch (final Exception e)
    {
      throw new TransformerConfigurationException ("Failed to load " + className, e);
    }
  }
}
