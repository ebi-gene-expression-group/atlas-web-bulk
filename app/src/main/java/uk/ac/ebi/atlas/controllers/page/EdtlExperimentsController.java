package uk.ac.ebi.atlas.controllers.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

@Controller
public class EdtlExperimentsController extends HtmlExceptionHandlingController {
    @GetMapping(value = "/edtl/experiments", produces = "text/html;charset=UTF-8")
    public String getEdtlExperimentsPage(Model model) {
        model.addAttribute("title", "EDTL experiments");
        return "edtl-landing-page";
    }
}
