package uk.ac.ebi.atlas.experimentpage;

import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.atlas.model.experiment.Experiment;

import javax.servlet.http.HttpServletRequest;

public class ExperimentDispatcherUtils {
    protected ExperimentDispatcherUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean alreadyForwardedButNoOtherControllerHandledTheRequest(HttpServletRequest request) {
        return StringUtils.startsWith(request.getQueryString(), "type=");
    }

    public static String buildForwardURL(HttpServletRequest request, Experiment experiment) {
        String requestURL = getRequestURL(request);
        requestURL += "?type=" + experiment.getType();

        return requestURL;
    }

    private static String getRequestURL(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();

        return StringUtils.substringAfter(requestURI, contextPath);
    }
}
