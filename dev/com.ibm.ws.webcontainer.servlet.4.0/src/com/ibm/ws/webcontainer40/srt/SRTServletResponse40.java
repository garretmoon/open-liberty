/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.srt;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.websphere.servlet40.IResponse40;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer31.srt.SRTServletResponse31;
import com.ibm.ws.webcontainer40.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.srt.SRTConnectionContext40;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.EncodingUtils;
import com.ibm.wsspi.webcontainer40.WCCustomProperties40;

/**
 * The Servlet Runtime Response object
 *
 * The SRTServletResponse class handles response object functions that involve the input and output streams. This class
 * contains no WebApp level information, and should not be hacked to include any. A
 * WebAppDispatcherResponse object will proxy this response and handle method calls that need
 * path or webapp information.
 *
 * @author The Unknown Programmer
 *
 */
public class SRTServletResponse40 extends SRTServletResponse31 implements HttpServletResponse {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer40.srt");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer40.srt.SRTServletResponse40";

    ArrayList<Cookie> addedCookies;

    private Supplier<Map<String, String>> trailerFieldSupplier;

    public SRTServletResponse40(SRTConnectionContext40 context) {
        super(context);
    }

    @Override
    public void initForNextResponse(IResponse resp) {
        super.initForNextResponse(resp);
        addedCookies = null;
    }

    /*
     * Return the default "X-Powered-By" header value for Servlet 4.0
     */
    @Override
    protected String getXPoweredbyHeader() {
        return WebContainerConstants.X_POWERED_BY_DEFAULT_VALUE40;
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.logp(Level.FINE, CLASS_NAME, "addCookie", "Adding cookie --> " + cookie.getName(), "[" + this + "]");
        }
        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
                logger.logp(Level.FINE, CLASS_NAME, "addCookie", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"),
                            "addCookie cookie --> " + cookie.getName()); //311717
        } else {
            if (!_ignoreStateErrors && isCommitted()) {
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "addCookie");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME, "addCookie", "Cannot set header.  Response already committed.");
                }
            } else {
                _response.addCookie(cookie);
                if (addedCookies == null) {
                    addedCookies = new ArrayList<Cookie>();
                }
                addedCookies.add(cookie);
            }
        }
    }

    public Cookie[] getAddedCookies() {

        if (addedCookies != null) {
            Cookie[] cookies = new Cookie[addedCookies.size()];
            return addedCookies.toArray(cookies);
        }
        return null;

    }

    @Override
    public void setTrailerFields(Supplier<Map<String, String>> supplier) throws IllegalStateException {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.logp(Level.FINE, CLASS_NAME, "setTrailerFields", "[" + this + "]");
        }

        if (isCommitted()) {
            throw new IllegalStateException();
        } else {
            trailerFieldSupplier = supplier;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
                logger.logp(Level.FINE, CLASS_NAME, "finish", "supplier is empty : " + trailerFieldSupplier.get().isEmpty());
            }
        }

    }

    @Override
    public Supplier<Map<String, String>> getTrailerFields() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "getTrailerFields", "trailerFieldSupplier = " + trailerFieldSupplier + " [" + this + "]");
        }

        return trailerFieldSupplier;
    }

    @Override
    public void finish() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            logger.entering(CLASS_NAME, "finish", " trailerFieldSupplier = " + trailerFieldSupplier + "[" + this + "]");

        if ((trailerFieldSupplier != null)) {
            if (!trailerFieldSupplier.get().isEmpty()) {

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "finish", " set trailer fields [" + this + "]");
                }
                ((IResponse40) _response).setTrailers(trailerFieldSupplier.get());
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "finish", "supplier is empty");
                }

            }

        }
        super.finish();
    }

    @Override
    protected String getSpecLevelEncoding(String encoding, WebApp webApp) {
        String servlet40encoding = null;

        if (webApp != null) {
            servlet40encoding = webApp.getConfiguration().getModuleResponseEncoding();
            if (servlet40encoding != null && EncodingUtils.isCharsetSupported(encoding)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "getSpecLevelEncoding", "Encoding from web module [" + encoding + "]");
            } else {
                servlet40encoding = null;
            }
        }

        if (servlet40encoding == null) {
            servlet40encoding = WCCustomProperties40.SERVER_RESPONSE_ENCODING;
            if (servlet40encoding != null && !encoding.isEmpty() && EncodingUtils.isCharsetSupported(encoding)) {
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "getSpecLevelEncoding", "Encoding from WC property->[" + encoding + "]");
            } else {
                servlet40encoding = null;
            }
        }

        if (servlet40encoding == null) {
            servlet40encoding = encoding;
        }

        return servlet40encoding;
    }

}
