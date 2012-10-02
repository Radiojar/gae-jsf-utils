
package com.javawords.faces.auth;

import java.io.IOException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class AuthPhaseListener implements PhaseListener {

    private static Logger logger = LoggerFactory.getLogger(AuthPhaseListener.class);
    private boolean redirect = false;

    public void setForward(boolean f) {
        this.redirect = f;
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent evnt) {

    }

    @Override
    public void afterPhase(PhaseEvent evnt) {
        FacesContext ctx = evnt.getFacesContext();
        String viewId = 
                (ctx == null || ctx.getViewRoot() == null) ? null : ctx.getViewRoot().getViewId();
        
        logger.debug("afterPhase() called, " + evnt.getPhaseId() + " , View ID: " + viewId);
        logger.debug("RenderResponse: " + Boolean.toString(ctx.getRenderResponse()) +
                ", ResponseComplete: " + Boolean.toString(ctx.getResponseComplete()));

        if (redirect) {
            // Reset forwarding.
            redirect = false;
            // Get and clear original uri.
            String originalURI = getOriginalURI(ctx);
            if (originalURI != null && !ctx.getResponseComplete()) {
                logger.debug("originalURI set, trying to redirect.");
                // Try to forward.
                try {
                    ctx.getExternalContext().redirect(originalURI);
                    ctx.responseComplete();
                } catch (IllegalArgumentException iaex) {
                // Do nothing, originally requested resource could not exist
                // while logicaly belonging to the auth area.
                } catch (IOException ioex) {
                    logger.error("Error in afterPhase(), originalURI is: " + originalURI, ioex);
                }
            }
        }


    }

    private String getOriginalURI(FacesContext ctx) {
        logger.debug("Getting '" + AuthFilter.ORIGINAL_URI_PROPERTY_NAME + "' attribute.");
        // Get variable.
        String uri = (String) ctx.getExternalContext().getSessionMap().get(AuthFilter.ORIGINAL_URI_PROPERTY_NAME);
        // Clear variable.
        if (uri != null) {
            ctx.getExternalContext().getSessionMap().remove(AuthFilter.ORIGINAL_URI_PROPERTY_NAME);
            logger.debug("Reseting '" + AuthFilter.ORIGINAL_URI_PROPERTY_NAME + "' attribute.");
        }
        return uri;
    }
}
