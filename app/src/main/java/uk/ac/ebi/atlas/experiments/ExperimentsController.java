package uk.ac.ebi.atlas.experiments;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

@Controller
public class ExperimentsController extends HtmlExceptionHandlingController {
    @RequestMapping(value = "/experiments", produces = "text/html;charset=UTF-8")
    public String getExperimentsListParameters(Model model) {
        model.addAttribute("mainTitle", "Experiments ");
        return "experiments";
    }
}
