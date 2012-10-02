package com.javawords.faces.listener;

import javax.faces.application.Application;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class AppListener implements ServletContextListener{

    private static Logger logger = LoggerFactory.getLogger(AppListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        String appName = sce.getServletContext().getServletContextName();

        logger.info("Application '" + appName + "' started.");

        String jsfTitle = Application.class.getPackage().getImplementationTitle();
        String jsfVendor = Application.class.getPackage().getImplementationVendor();
        String jsfVersion = Application.class.getPackage().getImplementationVersion();

        logger.info("JSF Info:");
        logger.info("Implementation Title:\t" + jsfTitle);
        logger.info("Implementation Vendor:\t" + jsfVendor);
        logger.info("Implementation Version:\t" + jsfVersion);

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        String appName = sce.getServletContext().getServletContextName();
        logger.info("Application '" + appName + "' ended.");
    }

}
