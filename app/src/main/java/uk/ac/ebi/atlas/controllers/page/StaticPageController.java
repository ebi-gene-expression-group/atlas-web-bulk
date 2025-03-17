package uk.ac.ebi.atlas.controllers.page;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.ServletContextResourceLoader;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;

import javax.inject.Inject;
import javax.servlet.ServletContext;

@Profile("!cli")
@Controller
public class StaticPageController extends HtmlExceptionHandlingController {
    private final ServletContextResourceLoader servletContextResourceLoader;

    @Inject
    public StaticPageController(ServletContext servletContext) {
        servletContextResourceLoader = new ServletContextResourceLoader(servletContext);
    }

    @RequestMapping("/{pageName}.html")
    public String getStaticPage(@PathVariable String pageName) {
        checkPageExists(String.format("/resources/html/%s.html", pageName), pageName);
        return pageName;
    }

    @RequestMapping("/help/{pageName}.html")
    public String getHelpPage(@PathVariable String pageName) {
        checkPageExists(String.format("/resources/html/help/%s.html", pageName), pageName);
        return "help/" + pageName;
    }

    private void checkPageExists(String path, String pageName) {
        Resource resource = servletContextResourceLoader.getResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Resource " + pageName + " does not exist");
        }
    }
}
