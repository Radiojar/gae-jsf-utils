package com.javawords.faces.mapping;

import java.io.IOException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;
//import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Filter will map requests of URIs ending with "/{anyword}"
 * where {anyword} is the file name of a JSP page without the
 * extension, to the corresponding JSF URL. It must be declared
 * in web.xml configuration file with an initialization parameter
 * named "faces extension" having the extension used to map the
 * FacesServlet.<br />
 * <strong>Note:</strong> Updated Dec 18 2009. Added annotations
 * to configure the filter without the use of <em>web.xml</em>
 */
//@WebFilter(
//    filterName="Faces Mapping Filter",
//    urlPatterns={"/*"},
//    initParams={
//        @WebInitParam(name="faces extension", value=".jsf"),
//        @WebInitParam(name="excludes", value="/excluded"),
//        @WebInitParam(name="lookup extension", value=".xhtml")
//    }
//)
public class MappingFilter extends HttpServlet implements Filter {
    
    private FilterConfig filterConfig;
    
    private String facesExtension;

    private String lookupExtension;

    private List<String> excludes = new ArrayList<String>(0);
    
    private static Logger logger = LoggerFactory.getLogger(MappingFilter.class);
    /**
     * Process the request/response pair. Check if the requested URI
     * ends with the desired pattern and if does so, forward the
     * request to the FacesServlet.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        logger.debug("doFilter() called..");
        HttpServletRequest httpReq = (HttpServletRequest) request;
        // Get the requested URI
        String uri = httpReq.getRequestURI();
        logger.debug("Requested URI is " + uri);
        try{
            // Check if the URI matches mapping creteria.
            boolean matches = false;
            boolean excluded = false;
            for(String s : excludes){
                if(uri.startsWith(s)){
                    excluded = true;
                    logger.debug("URI '" + uri + "' is exluded from filtering.");
                    break;
                }
            }
            if(!excluded) matches = uri.matches(".*/[\\w]+/*");

            if(matches && !excluded) {
                // Remove any trailing slashes from the requested URI
                while (uri.endsWith("/")) {
                    uri = uri.substring(0, uri.length() - 1);
                    logger.debug("Removed trailing slash from the requested URI: {}", uri);
                }
                
                ServletContext context = filterConfig.getServletContext();
                // Strip context path from the requested URI
                String path = context.getContextPath();
                if (path.length() > 0 && uri.startsWith(path)){
                    uri = uri.substring(path.length());
                    logger.debug("Stripped the context path from the requested URI: {}", uri);
                }
                // Check if there is actually a file to handle the forward.
                URL url = context.getResource(uri+lookupExtension);
                if(url != null) {
                    // Generate the forward URI
                    String forwardURI = uri + facesExtension;
                    // Get the request dispatcher
                    RequestDispatcher rd =
                        context.getRequestDispatcher(forwardURI);
                    if(rd != null){
                        logger.debug("Forwarding to " + forwardURI);
                        // Forward the request to FacesServlet
                        rd.forward(request, response);
                        return;
                    }
                    logger.debug("Cannot get a request dispatcher for the generated URI: " +
                            forwardURI);
                }
                logger.debug("Resource " + uri + lookupExtension + " does not exist.");
            } 
            // We are not interested for this request, pass it to the FilterChain.
            logger.debug("Passing URI '" + uri + "' down the filter chain..");
            chain.doFilter(request, response);
            
        } catch (ServletException sx) {
            filterConfig.getServletContext().log(sx.getMessage());
        } catch (IOException iox) {
            filterConfig.getServletContext().log(iox.getMessage());
        }
        
    }
    
    /**
     * Handle the passed-in FilterConfig
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        logger.info("MappingFilter initializing..");
        filterConfig = config;
        facesExtension = config.getInitParameter("faces extension");
        logger.info("Faces extension to filter is: " + facesExtension);
        lookupExtension = config.getInitParameter("lookup extension");
        logger.info("File lookup extension is: " + lookupExtension);
        String exclParam = config.getInitParameter("excludes");
        for(String s : Arrays.asList(exclParam.split(";"))){
            excludes.add(s.trim());
            logger.info("Directory '" + s + "' will be excluded from filtering.");
        }
    }
    
    /**
     * Clean up resources
     */
    @Override
    public void destroy() {
        filterConfig = null;
    }
    
}
