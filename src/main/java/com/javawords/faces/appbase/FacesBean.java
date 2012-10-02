package com.javawords.faces.appbase;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.validator.ValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base class having the functionality shared across all JSF managed
 * beans.
 * @author xfragoulidis
 *
 */
public class FacesBean {

    private static final Logger logger = LoggerFactory.getLogger(FacesBean.class);
    /**
     * Default outcome string when an action succeeds.
     */
    public static final String OUTCOME_SUCCESS = "success";
    /**
     * Default outcome string when an action fails.
     * We set this to null so the same page will be displayed
     * during navigation.
     */
    public static final String OUTCOME_FAILURE = null;

    public FacesBean() {
        logger.debug("New " + this.getClass().getSimpleName() + " instance constructor called.");
    }

    /**
     * <p>Return the <code>FacesContext</code> instance for the current
     * request.</p>
     */
    protected static FacesContext getFacesContext() {

        return FacesContext.getCurrentInstance();

    }

    /**
     * <p>Return the <code>ExternalContext</code> instance for the
     * current request.</p>
     */
    protected static ExternalContext getExternalContext() {

        return FacesContext.getCurrentInstance().getExternalContext();

    }

    /**
     * <p>Return the <code>Application</code> instance for the current
     * web application.</p>
     */
    protected static Application getApplication() {

        return FacesContext.getCurrentInstance().getApplication();

    }

    /**
     * <p>Return the <code>Lifecycle</code> instance utilized by JSF.</p>
     */
    protected static Lifecycle getLifecycle() {
        String lifecycleId =
                getExternalContext().getInitParameter("javax.faces.LIFECYCLE_ID");
        if (lifecycleId == null || lifecycleId.length() == 0) {
            lifecycleId = LifecycleFactory.DEFAULT_LIFECYCLE;
        }
        LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        return lifecycleFactory.getLifecycle(lifecycleId);
    }

    /**
     * <p>Searches for <code>UIInput</code> components under the provided root
     * <code>UIComponent</code> and resets their values. If the root is also
     * a <code>UIInput</code> component, will reset it's value too.
     * In the case that a component is equal to the except parameter, it will
     * be ignored.</p>
     * @param root
     * @param except
     */
    protected void resetUIInput(UIComponent root, UIComponent except) {
        if (root instanceof UIInput && !root.equals(except)) {
            UIInput c = (UIInput) root;
            logger.trace("Found UIInput Component in the tree: ID = " + c.getClientId(getFacesContext()) + ", Class = " + c.getClass().getSimpleName());
            logger.trace("    Submitted Value: " + c.getSubmittedValue());
            logger.trace("        Local Value: " + c.getLocalValue());
            logger.trace("  Validator Message: " + c.getValidatorMessage());
            logger.trace("           Is Valid: " + c.isValid());
            logger.trace(" Is Local Value Set: " + c.isLocalValueSet());
            c.resetValue();
        }
        for (UIComponent c : root.getChildren()) {
            resetUIInput(c, except);
        }
    }

    /**
     * <p>Same as <code>resetUIInput(UIComponent root, UIComponent except)</code>
     * but effective for all found <code>UIInput</code> components.</p>
     * @param root
     */
    protected void resetUIInput(UIComponent root) {
        resetUIInput(root, null);
    }

    /**
     * <p>Return any attribute stored in request scope, session scope, or
     * application scope under the specified name.  If no such
     * attribute is found, and if this name is the registered name of a
     * managed bean, cause a new instance of this managed bean to be created
     * (and stored in an appropriate scope, if necessary) and returned.
     * If no attribute exists, and no managed bean was created, return
     * <code>null</code>.</p>
     *
     * @param name Name of the attribute to be retrieved
     */
    protected static Object getBean(String name) {
        return getApplication().getELResolver().getValue(getFacesContext().getELContext(), null, name);
    }

    /**
     * Adds an internal error message to the queue.
     */
    protected static void addInternalErrorMessage() {
        FacesMessage errMsg =
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Internal Error Occured.",
                " The error has been logged, please try your request in a minute.");
        getFacesContext().addMessage(null, errMsg);
    }

    protected static void addErrorMessage(String summary, String detail) {
        addErrorMessage(null, summary, detail);
    }

    protected static void addErrorMessage(String clientId, String summary, String detail) {
        FacesMessage errMsg =
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, ' ' + detail);
        getFacesContext().addMessage(clientId, errMsg);
    }

    protected static void throwValidationError(String message) throws ValidatorException {
        FacesMessage m = new FacesMessage(message);
        ValidatorException e = new ValidatorException(m);
        throw e;
    }
}
