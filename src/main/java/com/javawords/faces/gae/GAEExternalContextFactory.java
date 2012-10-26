package com.javawords.faces.gae;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextFactory;
import javax.faces.context.ExternalContextWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class GAEExternalContextFactory extends ExternalContextFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GAEExternalContextFactory.class);
    
    private static final String CTX_REQUEST_PARAM = GAEExternalContext.class.getName() + '.' + "context";
    
    private final ExternalContextFactory wrappedFactory;

    public GAEExternalContextFactory(ExternalContextFactory wrappedFactory) {
        
        LOGGER.info("{} created. Factory class provided by JSF runtime: {}", this.getClass().getSimpleName(),
                wrappedFactory.getClass().getName());
        
        this.wrappedFactory = wrappedFactory;
        
        // Set the view handler that will callback the session context when the response is complete
        // in order to persist touched entries.
        LifecycleFactory factory = (LifecycleFactory)
                FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        for (Iterator i = factory.getLifecycleIds(); i.hasNext();) {
            Lifecycle l = factory.getLifecycle((String) i.next());
            l.addPhaseListener(new GAEPhaseListener());
        }
    }

    @Override
    public ExternalContextFactory getWrapped() {
        return wrappedFactory;
    }    
    
    @Override
    public ExternalContext getExternalContext(Object context, Object request, Object response) 
            throws FacesException {
        
        ExternalContext wrappedContext = wrappedFactory.getExternalContext(context, request, response);
        ExternalContext result = new GAEExternalContext(wrappedContext);
        // Add the result object to ensure that GAEPhaseListener will be able to callback the
        // correct object.
        result.getRequestMap().put(CTX_REQUEST_PARAM, result);
        return result;
        
    }

    
    private static class GAEPhaseListener implements PhaseListener {
        
        private static final Set<Integer> AFTER_PHASES 
                = new HashSet<Integer>(Arrays.asList(PhaseId.RENDER_RESPONSE.getOrdinal(),
                                                     PhaseId.UPDATE_MODEL_VALUES.getOrdinal()));
        
        @Override
        public void beforePhase(PhaseEvent event) {
            if (!event.getPhaseId().equals(PhaseId.RESTORE_VIEW)) return;
            GAEExternalContext target = getContext();
            if (target != null) target.restore();
        }
        
        @Override
        public void afterPhase(PhaseEvent event) {
            if (!AFTER_PHASES.contains(event.getPhaseId().getOrdinal())) return;
            GAEExternalContext target = getContext();
            if (target != null) {
                if (event.getPhaseId().equals(PhaseId.RENDER_RESPONSE)) {
                    target.persist();
                    target.release();
                }
            }            
        }
        
        @Override
        public PhaseId getPhaseId() {
            return PhaseId.ANY_PHASE;
        }
        
        private GAEExternalContext getContext() {
            
            FacesContext ctx = FacesContext.getCurrentInstance();
            return (GAEExternalContext) ctx.getExternalContext().getRequestMap().get(CTX_REQUEST_PARAM);
        }
        
    }

    public static class GAEExternalContext extends ExternalContextWrapper {
        
        private final ExternalContext wrappedContext;
        
        private static final String NAMESPACE_PREFIX = GAEExternalContext.class.getPackage().getName();
        
        private static final String SESSION_MAP_SUFFIX = "SessionMap";
        
        private static final Map<String, Map<String, Object>> SESSION_MAPS = 
                new HashMap<String, Map<String, Object>>();
        
        private final MemcacheService memcache = 
                MemcacheServiceFactory.getMemcacheService(NAMESPACE_PREFIX + '.' + SESSION_MAP_SUFFIX);
        
        public GAEExternalContext(ExternalContext wrappedContext) {
            
            LOGGER.info("{} created, wrapped ExternalContext class is: {}", this.getClass().getSimpleName(),
                    wrappedContext.getClass().getName());
            
            this.wrappedContext = wrappedContext;
        }        
        

        @Override
        public ExternalContext getWrapped() {
            return wrappedContext;
        }

        @Override
        public Map<String, Object> getApplicationMap() {
            return super.getApplicationMap();
        }

        @Override
        public Map<String, Object> getSessionMap() {
            String sessionId = getSessionId();
            Map result = SESSION_MAPS.get(sessionId);
            if (result == null) {
                result = new HashMap<String, Object>();
                SESSION_MAPS.put(sessionId, result);
            }
            return result;
        }
        
        private String getSessionId() {            
            HttpSession session = (HttpSession) wrappedContext.getSession(true);
            return session.getId();
        }
        
        protected void restore() {
            LOGGER.info("********* restore() called!");
            String sessionId = getSessionId();            
            Map<String, Object> map = (Map<String, Object>) memcache.get(sessionId);
            SESSION_MAPS.put(sessionId, map);
        }
        
        protected void persist() {
            LOGGER.info("********* persist() called!");            
            String sessionId = getSessionId(); 
            Map<String, Object> map = SESSION_MAPS.get(sessionId);
            memcache.put(sessionId, map);
        }
        
        protected void release() {            
            LOGGER.info("********* release() called!");
            SESSION_MAPS.remove(getSessionId());
        }
    }
    
}
