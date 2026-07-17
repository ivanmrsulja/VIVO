package edu.cornell.mannlib.vivo.orcid.controller;

import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.vitro.webapp.config.ConfigurationProperties;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.RedirectResponseValues;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ContextModelAccess;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import edu.cornell.mannlib.vivo.orcid.service.OrcidSyncService;
import edu.cornell.mannlib.vivo.orcid.util.OrcidInternalOperationsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

public class OrcidSyncNowHandler extends OrcidAbstractHandler {

    private static final Log log = LogFactory.getLog(OrcidSyncNowHandler.class);


    public OrcidSyncNowHandler(VitroRequest vreq) {
        super(vreq);
    }

    public RedirectResponseValues exec() {
        String individualUri = vreq.getParameter("profileUri");

        log.info("Starting manual sync for: " + individualUri);

        String accessToken =
            getBoundTokenForIndividual(individualUri, OrcidInternalOperationsUtil.ACCESS_TOKEN_PROPERTY);

        ConfigurationProperties props = ConfigurationProperties.getInstance();
        new OrcidSyncService(
            props.getProperty("orcid.clientId"),
            props.getProperty("orcid.clientPassword"),
            props.getProperty("orcid.api"),
            vreq.getRDFService()
        ).syncOrcidIndividual(individualUri, accessToken);

        return new RedirectResponseValues(
            occ.getSetting(OrcidClientContext.Setting.WEBAPP_BASE_URL) + "individual?uri=" + individualUri);
    }

    private String getBoundTokenForIndividual(String individualURI, String tokenProperty) {
        OntModel displayModel = getOntModel();

        Resource individual = displayModel.getResource(individualURI);

        Statement allowPushStatement = displayModel.getProperty(
            individual,
            ResourceFactory.createProperty(OrcidInternalOperationsUtil.ALLOW_PUSH_PROPERTY)
        );

        if (allowPushStatement == null ||
            !allowPushStatement.getObject().isLiteral() ||
            !allowPushStatement.getBoolean()) {
            return null;
        }

        Statement tokenStatement = displayModel.getProperty(
            individual,
            ResourceFactory.createProperty(tokenProperty)
        );

        if (tokenStatement == null || !tokenStatement.getObject().isLiteral()) {
            return null;
        }

        return tokenStatement.getLiteral().getString();
    }

    private OntModel getOntModel() {
        ContextModelAccess cma = ModelAccess.getInstance();
        return cma.getOntModel(ModelNames.INTEGRATION_SETTINGS);
    }
}
