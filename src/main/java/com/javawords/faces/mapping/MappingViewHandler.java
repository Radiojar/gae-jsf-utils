package com.javawords.faces.mapping;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class in combination with the MappingFilter will take care of the mapping.
 * @author Christos Fragoulides
 */
public class MappingViewHandler extends ViewHandler {

    private static final Logger logger = LoggerFactory.getLogger(MappingViewHandler.class);

    /**
     * The original handler we are extending.
     */
    private ViewHandler prevHandler = null;
    
    /** Creates a new instance of MappingViewHandler. By including
     * a parameter of the same type, we encourage the JSF framework
     * to pass a reference of the previously used ViewHandler. This way
     * we can use all the previous functionallity and override only the
     * method we are interested in (in this case, the getActionURL() method).
     */
    public MappingViewHandler(ViewHandler prevHandler) {
        this.prevHandler = prevHandler;
        logger.info("MappingViewHandler initialized.");
    }
    
    
    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public Locale calculateLocale(FacesContext context) {
        return prevHandler.calculateLocale(context);
    }

    @Override
    public String calculateCharacterEncoding(FacesContext context) {
        return prevHandler.calculateCharacterEncoding(context);
    }
    
    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public String calculateRenderKitId(FacesContext context) {
        return prevHandler.calculateRenderKitId(context);
    }

    @Override
    public void initView(FacesContext context) throws FacesException {
        prevHandler.initView(context);
    }
    
    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public UIViewRoot createView(FacesContext context, String viewId) {
        logger.debug("createView() called, viewId = [{}].", viewId);
        UIViewRoot result = prevHandler.createView(context, viewId);
        logger.debug("Created view's viewId = [{}].", result.getViewId());
        return result;
    }

    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId) {
        logger.debug("restoreView() called, viewId = [{}].", viewId);
        return prevHandler.restoreView(context, viewId);
    }

    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender)
        throws IOException, FacesException {
        logger.debug("renderView() called, viewId = [{}].", viewToRender.getViewId());
        prevHandler.renderView(context, viewToRender);
        logger.debug("renderView() complete.");
    }

    @Override
    public String deriveViewId(FacesContext context, String rawViewId) {
        logger.debug("deriveViewId() called, rawViewId = [{}].", rawViewId);
        return prevHandler.deriveViewId(context, rawViewId);
    }

    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(FacesContext context,
            String viewId) {
        logger.debug("getViewDeclarationLanguage() called, viewId = [{}].", viewId);
        return prevHandler.getViewDeclarationLanguage(context, viewId);
    }
    
    /**
     * This is the only method needed to be extended. First, we get the
     * normal URL form the original ViewHandler. Then we simply return
     * the same URL with the extension stripped of.
     */
    @Override
    public String getActionURL(FacesContext context, String viewId) {
        logger.debug("getActionURL() called, viewId = [{}].", viewId);
        String origURL = prevHandler.getActionURL(context, viewId);
        int dotIdx = origURL.lastIndexOf(".");
        if (dotIdx > 0) {
            return origURL.substring(0,dotIdx);
        }
        else return origURL;
    }
    
    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public String getResourceURL(FacesContext context, String path) {
        logger.debug("getResourceURL() called, path = [{}].", path);
        return prevHandler.getResourceURL(context, path);
    }

    @Override
    public String getBookmarkableURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams) {
        logger.debug("getBookmarkableURL() called, viewId = [{}].", viewId);
        String result = prevHandler.getBookmarkableURL(context, viewId, parameters,
                includeViewParams);
//        String replace = prevHandler.getActionURL(context, viewId);
//        result = getActionURL(context, viewId) + result.substring(replace.length());
        logger.debug("getBookmarkableURL() result: [{}].", result);
        return result;
    }

    @Override
    public String getRedirectURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams) {
        logger.debug("getRedirectURL() called, viewId = [{}].", viewId);
        return getActionURL(context, viewId);
    }
        
    /**
     * Delegate control to the original ViewHandler
     */
    @Override
    public void writeState(FacesContext context) throws IOException {
        prevHandler.writeState(context);
    }

    private String alterViewId(String viewId) {
        int dotIdx = viewId.lastIndexOf(".");
        if (dotIdx > 0) {
            return viewId.substring(0,dotIdx);
        } else return viewId;
    }
    
}
