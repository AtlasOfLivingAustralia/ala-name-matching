package org.ala.biocache.web;

import au.org.ala.biocache.ErrorCode;
import au.org.ala.biocache.Store;
import au.org.ala.biocache.QualityAssertion;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * This controller provides webservices for assertion creation/deletion.
 *
 * TODO Add support for API keys so that only registered applications can
 * use these functions.
 */
@Controller
public class AssertionController {

    private final static Logger logger = Logger.getLogger(AssertionController.class);
    /**
     * Retrieve an array of the assertion codes in use by the processing system
     *
     * @return an array of codes
     * @throws Exception
     */
    @RequestMapping(value = {"/assertions/codes"}, method = RequestMethod.GET)
	public @ResponseBody ErrorCode[] showCodes() throws Exception {
        return Store.retrieveAssertionCodes();
    }

    @RequestMapping(value = {"/assertions/geospatial/codes"}, method = RequestMethod.GET)
	public @ResponseBody ErrorCode[] showGeospatialCodes() throws Exception {
        return Store.retrieveGeospatialCodes();
    }

    @RequestMapping(value = {"/assertions/taxonomic/codes"}, method = RequestMethod.GET)
	public @ResponseBody ErrorCode[] showTaxonomicCodes() throws Exception {
        return Store.retrieveTaxonomicCodes();
    }

    @RequestMapping(value = {"/assertions/temporal/codes"}, method = RequestMethod.GET)
	public @ResponseBody ErrorCode[] showTemporalCodes() throws Exception {
        return Store.retrieveTemporalCodes();
    }

    @RequestMapping(value = {"/assertions/miscellaneous/codes"}, method = RequestMethod.GET)
	public @ResponseBody ErrorCode[] showMiscellaneousCodes() throws Exception {
        return Store.retrieveMiscellaneousCodes();
    }

    @RequestMapping(value = {"/assertions/user/codes"}, method = RequestMethod.GET)
	public @ResponseBody ErrorCode[] showUserCodes() throws Exception {
        return Store.retrieveUserAssertionCodes();
    }

    /**
     * add an assertion
     */
    @RequestMapping(value = {"/occurrences/{recordUuid}/assertions/add"}, method = RequestMethod.POST)
	public void addAssertion(
       @PathVariable(value="recordUuid") String recordUuid,
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        String code = request.getParameter("code");
        String comment = request.getParameter("comment");
        String userId = request.getParameter("userId");
        String userDisplayName = request.getParameter("userDisplayName");

        try {
            logger.debug("Adding assertion to:"+recordUuid+", code:"+code+", comment:"+comment
                    + ", userId:" +userId + ", userDisplayName:" +userDisplayName);

            QualityAssertion qa = au.org.ala.biocache.QualityAssertion.apply(Integer.parseInt(code));
            qa.setComment(comment);
            qa.setUserId(userId);
            qa.setUserDisplayName(userDisplayName);

            Store.addUserAssertion(recordUuid, qa);

            String server = request.getSession().getServletContext().getInitParameter("serverName");
            response.setHeader("Location", server + "/occurrences/" + recordUuid + "/assertions/" + qa.getUuid());
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch(Exception e){
            logger.error(e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Remove an assertion
     */
    @RequestMapping(value = {"/occurrences/{recordUuid}/assertions/delete"}, method = RequestMethod.POST)
	public void deleteAssertion(
        @PathVariable(value="recordUuid") String recordUuid,
        @RequestParam(value="assertionUuid", required=true) String assertionUuid,
        HttpServletResponse response) throws Exception {
        Store.deleteUserAssertion(recordUuid, assertionUuid);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Get single assertion
     */
    @RequestMapping(value = {"/occurrences/{recordUuid}/assertions/{assertionUuid}"}, method = RequestMethod.GET)
	public @ResponseBody QualityAssertion getAssertion(
        @PathVariable(value="recordUuid") String recordUuid,
        @PathVariable(value="assertionUuid") String assertionUuid,
        HttpServletResponse response) throws Exception {
        QualityAssertion qa = Store.getUserAssertion(recordUuid, assertionUuid);
        if(qa!=null){
            return qa;
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    /**
     * Get systemAssertions
     */
    @RequestMapping(value = {"/occurrences/{recordUuid}/assertions/"}, method = RequestMethod.GET)
	public @ResponseBody List<QualityAssertion> getAssertions(
        @PathVariable(value="recordUuid") String recordUuid) throws Exception {
        return Store.getUserAssertions(recordUuid);
    }
}
