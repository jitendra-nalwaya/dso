package weblogic.servlet.internal;

import java.util.List;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import weblogic.servlet.internal.session.SessionContext;

/**
 * This is a dummy class representing a real Weblogic class. It actually is being
 * used as a dummy for both the Weblogic 8 AND 9 versions, which have different
 * methods and fields.  
 * 
 * This class also has a bunch of methods that will be generated by ASM into the 
 * actual WL 8/9 class.  All of these methods start with "__tc_".  These methods 
 * are here so we can use the bytecode plugin to generate and maintain the 
 * necessary ASM code in WebAppServletContextAdapter.
 */
public class WebAppServletContext {
    // type stub only

	// === Dummy methods simulating real methods ===

	// Weblogic 8 only
	private List sessAttrListeners;
	private List sessListeners;
    private String sessionCookieComment;
    private String sessionCookieDomain;
    private int sessionCookieMaxAgeSecs;
    private String sessionCookieName;
    private String sessionCookiePath;
    private boolean sessionCookieSecure;
    private boolean sessionCookiesEnabled;
    private int sessionIDLength;
    private int sessionTimeoutSecs;
    private boolean sessionTrackingEnabled;
    private boolean sessionURLRewritingEnabled;

    // Weblogic 8 and 9
    private HttpServer httpServer;

    // Weblogic 9 only
	private SessionContext sessionContext;


	public String getName() {
        throw new AssertionError();
    }
    
	public String getContextPath() {
		throw new AssertionError();
	}
	
	public HttpServer getServer() {
		throw new AssertionError();
	}
	
	public EventsManager getEventsManager() {
		throw new AssertionError();
	}
	
	// === Methods simulating those added by ASM ==== 
	
	public boolean __tc_isWeblogic8() {
		try {
			this.getClass().getDeclaredField("sessionCookieName");
			return true;
		} catch(Exception e) {
		}
		return false;
	}
	
    public HttpSessionAttributeListener[] __tc_session_getHttpSessionAttributeListeners()
    {
    	if(__tc_isWeblogic8()) {
	        if(sessAttrListeners == null) {
	            return new HttpSessionAttributeListener[0];
	        } else {
	            return (HttpSessionAttributeListener[])sessAttrListeners.toArray(new HttpSessionAttributeListener[0]);
	        }
    	} else {
    		return getEventsManager().__tc_session_getHttpSessionAttributeListeners();
    	}
    }

    public HttpSessionListener[] __tc_session_getHttpSessionListener()
    {
    	if(__tc_isWeblogic8()) {
	        if(sessListeners == null) {
	            return new HttpSessionListener[0];
	        } else {
	            return (HttpSessionListener[])sessListeners.toArray(new HttpSessionListener[0]);
	        }
    	} else {
    		return getEventsManager().__tc_session_getHttpSessionListener();
    	}
    }
    
    public String __tc_session_getCookieComment()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookieComment;
    	} else {
    		return sessionContext.getConfigMgr().getCookieComment();
    	}
    }

    public String __tc_session_getCookieDomain()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookieDomain;
    	} else {
    		return sessionContext.getConfigMgr().getCookieDomain();
    	}
    }

    public int __tc_session_getCookieMaxAgeSecs()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookieMaxAgeSecs;
    	} else {
    		return sessionContext.getConfigMgr().getCookieMaxAgeSecs();
    	}
    }

    public String __tc_session_getCookieName()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookieName;
    	} else {
    		return sessionContext.getConfigMgr().getCookieName();
    	}
    }

    public String __tc_session_getCookiePath()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookiePath;
    	} else {
    		return sessionContext.getConfigMgr().getCookiePath();
    	}
    }

    public boolean __tc_session_getCookieSecure()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookieSecure;
    	} else {
    		return sessionContext.getConfigMgr().isCookieSecure();
    	}
    }

    public boolean __tc_session_getCookiesEnabled()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionCookiesEnabled;
    	} else {
    		return sessionContext.getConfigMgr().isSessionCookiesEnabled();
    	}
    }

    public int __tc_session_getIdLength()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionIDLength;
    	} else {
    		return sessionContext.getConfigMgr().getIDLength();
    	}
    }
    
    // same for both weblogic 8 and 9
    public String __tc_session_getServerId()
    {
        return httpServer.getServerHash();
    }

    // same for both wl 8 and 9
    public String __tc_session_getSessionDelimiter()
    {
        return "!";
    }

    public int __tc_session_getSessionTimeoutSecs()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionTimeoutSecs;
    	} else {
    		return sessionContext.getConfigMgr().getSessionTimeoutSecs();
    	}
    }

    public boolean __tc_session_getTrackingEnabled()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionTrackingEnabled;
    	} else {
    		return sessionContext.getConfigMgr().isSessionTrackingEnabled();
    	}
    }

    public boolean __tc_session_getURLRewritingEnabled()
    {
    	if(__tc_isWeblogic8()) {
    		return sessionURLRewritingEnabled;
    	} else {
    		return sessionContext.getConfigMgr().isUrlRewritingEnabled();
    	}
    }

}
