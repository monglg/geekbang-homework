package org.geektimes.projects.user.web.listener;

import org.geektimes.projects.user.web.context.ComponentContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DBConnectionInitializerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ComponentContext context = new ComponentContext();
        context.init(sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
