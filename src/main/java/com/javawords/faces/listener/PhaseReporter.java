package com.javawords.faces.listener;


import java.util.Iterator;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ListenerFor;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: check the issue described below when an update of JSF 2.0 will be available
/**
 * A <code>SystemEventListener</code> implementation which will conditionally
 * register a <code>PhaseListener</code> reporting phase events, based on
 * the development phase of the project.
 * <strong>Note:</strong> The class is annotated as a SystemEventListenr listener, but the
 * current implementation does not respect this annotation so it has to be
 * added in faces-config.xml.
 * @author xfragoulidis
 */
@ListenerFor(systemEventClass=PostConstructApplicationEvent.class, 
        sourceClass=Application.class)
public class PhaseReporter implements PhaseListener, SystemEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PhaseReporter.class);

    public PhaseReporter(){
        logger.debug("New instance created.");
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        logger.info("AFTER " + event.getPhaseId());
        
        if (event.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
            logger.info("---------------------- Lifecycle End: [{}] ----------------------",
                    event.getFacesContext().getViewRoot().getViewId());
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        if(event.getPhaseId().equals(PhaseId.RESTORE_VIEW))
            logger.info("------------------------ Lifecycle Begin ------------------------");
        
        logger.info("BEFORE " + event.getPhaseId());
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    /**
     * Implementation of <code>SystemEventListener</code>'s method, will
     * be called after the application's constuction and will register
     * this class as a <code>PhaseListener</code> of the <code>Lifecycle</code>.
     * @param event
     * @throws AbortProcessingException
     */
    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        logger.debug("Application Constructed");
        LifecycleFactory factory = (LifecycleFactory)
                FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        for (Iterator i = factory.getLifecycleIds(); i.hasNext();) {
            Lifecycle l = factory.getLifecycle((String) i.next());
            l.addPhaseListener(this);
        }
        // Uninstall the listener - no need to be included during usual
        // runtime processing.
        Application application = (Application) event.getSource();
        application.unsubscribeFromEvent(PostConstructApplicationEvent.class, this);

    }

    /**
     * Implementation of <code>SystemEventListener</code>'s method, will
     * return true only if the application in in development stage.
     * @param source
     * @return
     */
    @Override
    public boolean isListenerForSource(Object source) {
        if (source instanceof Application) {
            Application application = (Application) source;
            return (application.getProjectStage() == ProjectStage.Development);
        }
        logger.info("Application not in Development Stage, PhaseReporter will " +
                "not be registered as a listener.");
        return false;
    }

}
