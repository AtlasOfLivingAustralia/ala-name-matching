package org.ala.biocache.web;

import au.org.ala.biocache.Store;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.apache.log4j.Logger;

/**
 *
 * Provides administration web services for the biocache-service.
 *
 * All services should require an API key.
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
@Controller
public class AdminController extends AbstractSecureController {
    /** Logger initialisation */
    private final static Logger logger = Logger.getLogger(AdminController.class);
    
    /**
     * Optimises the SOLR index.  Use this API to optimise the index so that the biocache-service
     * can enter read only mode during this process.
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/admin/index/optimise", method = RequestMethod.POST)
	public void changeSearcherMode(HttpServletRequest request, 
	   HttpServletResponse response) throws Exception {
        String apiKey = request.getParameter("apiKey");
        if(shouldPerformOperation(apiKey, response)){
            String message = Store.optimiseIndex();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(message);
        }

    }
    /**
     * Reindexes the supplied dr based on modifications since the supplied date.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/admin/index/reindex", method = RequestMethod.POST)
    public void reindex(HttpServletRequest request, 
            HttpServletResponse response)throws Exception{
        String apiKey = request.getParameter("apiKey");
        if(shouldPerformOperation(apiKey, response)){
            String dataResource = request.getParameter("dataResource");
            String startDate = request.getParameter("startDate");
            logger.info("Reindexing data resource: " + dataResource + " starting at " + startDate);
            Store.reindex(dataResource, startDate);
            response.setStatus(HttpServletResponse.SC_OK);
        }
        
    }
    /**
     * Returns true when in service is in readonly mode.
     * @return
     */
    @RequestMapping(value="/admin/isReadOnly", method=RequestMethod.GET)
    public @ResponseBody boolean isReadOnly() {
        return Store.isReadOnly();
    }
}
