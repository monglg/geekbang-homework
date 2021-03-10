package org.geektimes.projects.user.web.listener;

import org.geektimes.projects.user.web.context.ComponentContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class TestListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Object component = ComponentContext.getComponentContext().getComponent("jdbc/UserPlatformDB");
        sce.getServletContext().log(component.toString());
        Object component2 = ComponentContext.getComponentContext().getComponent("jdbc/UserPlatformDB");
        sce.getServletContext().log(component2.toString());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
