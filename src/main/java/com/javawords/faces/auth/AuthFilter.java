package com.javawords.faces.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>BlogCounts Authorization Filter - A Filter that controls the
 * access to application resources requiring authorized access.</p>
 * 
 * <p>This filter accepts the following initialization parameters:</p>
 * <ul>
 *  <li><code>adminResources:</code> Comma-separated list of resources
 * requiring administrative rights to be accessed.
 *  </li>
 *  <li><code>userResources:</code> Comma-separated list of resources
 * requiring user rights to be accessed. Unauthorized users trying to access
 * these areas will receive a 404 HTTP error.
 *  </li>
 *  <li><code>forwardTo:</code> Forward URI, relative to the application's
 * context path. Unauthorized users will be forwarded to this URI. The original
 * requested URI will be added to the <code>HttpServletRequest</code> as an attribute with the
 * name <code><em>gr.xfrag.blogcounts.authFilter.originalURI</em></code>.
 *  </li>
 * </ul>
 * 
 * TODO: In order to generalize this class, we have to create an Interface
 * named "AuthUser" or something similar, and require users of this library
 * to implement it. Then we'll lookup current session for an instance of
 * this class to determine if the current user is authenticated.
 * @author Christos Fragoulides
 */
public class AuthFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    /**
     * The <code>FilterConfig</code> provided during initialization.
     */
    private FilterConfig config;
    /**
     * Resources requiring admin user rights to be accessed.
     */
    private List<String> adminResources = Collections.emptyList();
    /**
     * Resources requiring registered user rights to be accessed.
     */
    private List<String> userResources = Collections.emptyList();
    /**
     * Forward url for unauthorized requests.
     */
    private String forwardTo = null;
    /**
     * Web application context path.
     */
    private String contextPath;
    /**
     * {@code InheritableThreadLocal} keeping track of active requests.
     * When this {@code Filter} is invoked, it will store a reference of
     * the active request in this object. This reference can be used later
     * by the static utility methods of this class, allowing interaction
     * with the request, session and application scopes.
     */
    private static InheritableThreadLocal<HttpServletRequest> requestStore =
            new InheritableThreadLocal<HttpServletRequest>();
    
    public static final String AUTH_USER_ATTRIBUTE = "com.javawords.faces.auth.AuthUser";
    public static final String ORIGINAL_URI_PROPERTY_NAME = 
            "com.javawords.faces.auth.originalURI";

    /**
     * Filter Initialization - Extracts configuration from the
     * provided <code>FilterConfig</code> and initializes properties.
     * @param config
     * @throws javax.servlet.ServletException
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        logger.info("AuthFilter initializing..");

        // Keep reference for later use.
        this.config = config;

        // Get context path.
        contextPath = config.getServletContext().getContextPath();
        logger.debug("Context path is " + contextPath);

        // Get init parameters.
        String adminParam = config.getInitParameter("adminResources");
        String userParam = config.getInitParameter("userResources");
        forwardTo = config.getInitParameter("forwardTo");
        if (forwardTo == null) {
            forwardTo = "/";
        }


        // Extract admin areas.
        if (adminParam == null) {
            logger.info("adminResources parameter not set.");
        } else {
            adminResources = Arrays.asList(adminParam.split(","));
            logger.info(!adminResources.isEmpty() ? "Admin Resources:" : "No Admin Resources set or syntax error.");
            for (String area : adminResources) {
                logger.info(area);
            }
        }

        // Extract user areas.
        if (userParam == null) {
            logger.info("userResources parameter not set.");
        } else {
            userResources = Arrays.asList(userParam.split(","));
            logger.info(!userResources.isEmpty() ? "User Resources:" : "No User Resources set or syntax error.");
            for (String area : userResources) {
                logger.info(area);
            }
        }

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpReq = (HttpServletRequest) request;
        // Keep a reference of the active request.
        requestStore.set(httpReq);
        // This flag will remain true if the outcome of the following procees
        // is an error or a redirection.
        boolean clearRequestStore = true;
        
        // Get the requested URI
        String uri = httpReq.getRequestURI();
        logger.debug("Filtering requested URI: " + uri);
        try {

            // Check the request
            if (requiresAuth(uri, adminResources) && !isAdminUser(httpReq)) {
                // Hide the resource
                HttpServletResponse httpRes = (HttpServletResponse) response;
                httpRes.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else if (requiresAuth(uri, userResources) && !isUser(httpReq)) {
                // Forward to the location specified in parameters.
                storeOriginalURI(httpReq);
                RequestDispatcher rd = config.getServletContext()
                                             .getRequestDispatcher(forwardTo);
                
                rd.forward(request, response);
                return;
            }
            // If the following line gets executed, we can safely keep the
            // request reference object in the store.
            clearRequestStore = false;
        } catch (Exception e) {
            logger.error("Error trying to authenticate access to " + uri, e);
            throw new ServletException("Error trying to authenticate access to " + uri, e);
        } finally {
            // Clear reference of active request.
            if (clearRequestStore)                
                requestStore.set(null);
        }
        // Let the other filters perform filtering.
        chain.doFilter(request, response);
        // Request processing complete, clear the request store.
        requestStore.set(null);
    }

    @Override
    public void destroy() {
        config = null;
    }

    private void storeOriginalURI(HttpServletRequest req) {
        String uri = req.getRequestURI();//.substring(contextPath.length());
        String query = req.getQueryString();
        if (query != null) {
            uri += "?" + query;
        }
        req.getSession().setAttribute(ORIGINAL_URI_PROPERTY_NAME, uri);
        logger.debug("Set '" + ORIGINAL_URI_PROPERTY_NAME + "' Session attribute, value: " + uri);
    }

    private boolean requiresAuth(String uri, List<String> resources) {
        boolean needs = false;
        for (String resrc : resources) {
            if (resrc.startsWith("*.")) {
                needs |= uri.endsWith(resrc.substring(2));
            } else {
                needs |= uri.startsWith(contextPath + resrc);
            }
        }
        return needs;
    }

    private boolean isAdminUser(HttpServletRequest req) {
        // TODO: Set a user type attribute so we can check here if user is admin.
        logger.debug("Request requires admin authorization.");
        AuthUser user = getUser(req);
        logger.debug("User is " + (user == null ? "not" : "") + " authorized.");
        if (user != null) {
            logger.debug("User" + (user.isAdmin() ? "" : " do not") + " have administrative priviledges.");
            return user.isAdmin();
        }
        return false;
    }

    private boolean isUser(HttpServletRequest req) {
        logger.debug("Request requires user authorization.");
        AuthUser user = getUser(req);
        logger.debug("User is " + (user == null ? "not" : "") + " authorized.");
        return user != null;
    }

    private AuthUser getUser(HttpServletRequest req) {
//        AccountBean account = (AccountBean) req.getSession().getAttribute("accountBean");
//        if (account == null) {
//            return null;
//        }
//        return account.getUser();
        Object val =  req.getSession().getAttribute(AUTH_USER_ATTRIBUTE);
        if((val != null) && (val instanceof AuthUser)){
            return (AuthUser) val;
        }
        return null;
    }
    
    /**
     * Registers the passed in {@code AuthUser} as the logged-in user
     * of the current session context.
     * @param user the {@code AuthUser} to register.
     */
    public static void registerAuthUser(AuthUser user) {
        requestStore.get().getSession().setAttribute(AUTH_USER_ATTRIBUTE, user);
    }
    
    public static void clearAuthUser() {
        requestStore.get().getSession().removeAttribute(AUTH_USER_ATTRIBUTE);
    }
}
