package uk.ac.ebi.atlas.controllers.page;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

@Profile("!cli")
@Controller
public class StaticPageController extends HtmlExceptionHandlingController {

    @RequestMapping("/{pageName}.html")
    public String getStaticPage(@PathVariable String pageName) {
        return pageName;
    }

    @RequestMapping("/help/{pageName}.html")
    public String getHelpPage(@PathVariable String pageName) {
        return "help/" + pageName;
    }
}
