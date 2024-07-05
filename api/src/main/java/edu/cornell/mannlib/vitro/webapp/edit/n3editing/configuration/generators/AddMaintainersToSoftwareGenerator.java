/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.jena.QueryUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.FirstAndLastNameValidator;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.validators.AntiXssValidation;
import edu.cornell.mannlib.vitro.webapp.i18n.I18n;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

/**
 * This is a slightly unusual generator that is used by Manage Maintainers on
 * information resources.
 *
 * It is intended to always be an add, and never an update.
 */
public class AddMaintainersToSoftwareGenerator extends VivoBaseGenerator implements EditConfigurationGenerator {
    public static Log log = LogFactory.getLog(AddMaintainersToSoftwareGenerator.class);

    @Override
    public EditConfigurationVTwo getEditConfiguration(VitroRequest vreq,
                                                      HttpSession session) {
        EditConfigurationVTwo editConfiguration = new EditConfigurationVTwo();
        initBasics(editConfiguration, vreq);
        initPropertyParameters(vreq, session, editConfiguration);

        //Overriding URL to return to
        setUrlToReturnTo(editConfiguration, vreq);

        //set variable names
        editConfiguration.setVarNameForSubject("software");
        editConfiguration.setVarNameForPredicate("predicate");
        editConfiguration.setVarNameForObject("maintainershipUri");

        // Required N3
        editConfiguration.setN3Required(list(getN3NewMaintainership()));

        // Optional N3
        editConfiguration.setN3Optional(generateN3Optional());

        editConfiguration.addNewResource("maintainershipUri", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("newPerson", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("vcardPerson", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("vcardName", DEFAULT_NS_TOKEN);

        //In scope
        setUrisAndLiteralsInScope(editConfiguration, vreq);

        //on Form
        setUrisAndLiteralsOnForm(editConfiguration, vreq);

        //Sparql queries
        setSparqlQueries(editConfiguration, vreq);

        //set fields
        setFields(editConfiguration, vreq, EditConfigurationUtils.getPredicateUri(vreq));

        //template file
        editConfiguration.setTemplate("addMaintainersToSoftware.ftl");
        //add validators
        editConfiguration.addValidator(new FirstAndLastNameValidator("personUri", I18n.bundle(vreq)));

        //Adding additional data, specifically edit mode
        addFormSpecificData(editConfiguration, vreq);

        editConfiguration.addValidator(new AntiXssValidation());

        //NOITCE this generator does not run prepare() since it
        //is never an update and has no SPARQL for existing

        return editConfiguration;
    }

    private void setUrlToReturnTo(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        editConfiguration.setUrlPatternToReturnTo(EditConfigurationUtils.getFormUrlWithoutContext(vreq));
    }

    /***N3 strings both required and optional***/

    public String getN3PrefixString() {
        return "@prefix core: <" + vivoCore + "> .\n" +
            "@prefix foaf: <" + foaf + "> .  \n";
    }

    private String getN3NewMaintainership() {
        return getN3PrefixString() +
            "?maintainershipUri a core:Maintainership ;\n" +
            "  core:relates ?software .\n" +
            "?software core:relatedBy ?maintainershipUri .";
    }

    private String getN3MaintainershipRank() {
        return getN3PrefixString() +
            "?maintainershipUri core:rank ?rank .";
    }

    //first name, middle name, last name, and new perseon for new maintainer being created, and n3 for existing person
    //if existing person selected as maintainer
    public List<String> generateN3Optional() {
        return list(
            getN3NewPersonFirstName(),
            getN3NewPersonMiddleName(),
            getN3NewPersonLastName(),
            getN3NewPerson(),
            getN3MaintainershipRank(),
            getN3ForExistingPerson());
    }


    private String getN3NewPersonFirstName() {
        return getN3PrefixString() +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?newPerson <http://purl.obolibrary.org/obo/ARG_2000028>  ?vcardPerson . \n" +
            "?vcardPerson <http://purl.obolibrary.org/obo/ARG_2000029>  ?newPerson . \n" +
            "?vcardPerson a <http://www.w3.org/2006/vcard/ns#Individual> . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a <http://www.w3.org/2006/vcard/ns#Name> . \n" +
            "?vcardName vcard:givenName ?firstName .";
    }

    private String getN3NewPersonMiddleName() {
        return getN3PrefixString() +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?newPerson <http://purl.obolibrary.org/obo/ARG_2000028>  ?vcardPerson . \n" +
            "?vcardPerson <http://purl.obolibrary.org/obo/ARG_2000029>  ?newPerson . \n" +
            "?vcardPerson a vcard:Individual . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a vcard:Name . \n" +
            "?vcardName <http://vivoweb.org/ontology/core#middleName> ?middleName .";
    }

    private String getN3NewPersonLastName() {
        return getN3PrefixString() +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?newPerson <http://purl.obolibrary.org/obo/ARG_2000028>  ?vcardPerson . \n" +
            "?vcardPerson <http://purl.obolibrary.org/obo/ARG_2000029>  ?newPerson . \n" +
            "?vcardPerson a <http://www.w3.org/2006/vcard/ns#Individual> . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a <http://www.w3.org/2006/vcard/ns#Name> . \n" +
            "?vcardName vcard:familyName ?lastName .";
    }

    private String getN3NewPerson() {
        return getN3PrefixString() +
            "?newPerson a foaf:Person ;\n" +
            "<" + RDFS.label.getURI() + "> ?label .\n" +
            "?maintainershipUri core:relates ?newPerson .\n" +
            "?newPerson core:relatedBy ?maintainershipUri . ";
    }

    private String getN3ForExistingPerson() {
        return getN3PrefixString() +
            "?maintainershipUri core:relates ?personUri .\n" +
            "?personUri core:relatedBy ?maintainershipUri .";
    }

    /**
     * Get new resources
     */
    //A new maintainership uri will always be created when an maintainer is added
    //A new person may be added if a person not in the system will be added as maintainer
    private Map<String, String> generateNewResources(VitroRequest vreq) {


        HashMap<String, String> newResources = new HashMap<String, String>();
        newResources.put("maintainershipUri", DEFAULT_NS_TOKEN);
        newResources.put("newPerson", DEFAULT_NS_TOKEN);
        newResources.put("vcardPerson", DEFAULT_NS_TOKEN);
        newResources.put("vcardName", DEFAULT_NS_TOKEN);
        return newResources;
    }

    /**
     * Set URIS and Literals In Scope and on form and supporting methods
     */
    private void setUrisAndLiteralsInScope(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        //Uris in scope always contain subject and predicate
        HashMap<String, List<String>> urisInScope = new HashMap<String, List<String>>();
        urisInScope.put(editConfiguration.getVarNameForSubject(),
                        Arrays.asList(new String[] {editConfiguration.getSubjectUri()}));
        urisInScope.put(editConfiguration.getVarNameForPredicate(),
                        Arrays.asList(new String[] {editConfiguration.getPredicateUri()}));
        editConfiguration.setUrisInScope(urisInScope);
        //no literals in scope
    }

    public void setUrisAndLiteralsOnForm(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        List<String> urisOnForm = new ArrayList<String>();
        //If an existing person is being used as an maintainer, need to get the person uri
        urisOnForm.add("personUri");
        editConfiguration.setUrisOnform(urisOnForm);

        //for person who is not in system, need to add first name, last name and middle name
        //Also need to store maintainership rank and label of maintainer
        List<String> literalsOnForm = list("firstName",
                                           "middleName",
                                           "lastName",
                                           "rank",
                                           "label");
        editConfiguration.setLiteralsOnForm(literalsOnForm);
    }

    /**
     * Set SPARQL Queries and supporting methods.
     */
    private void setSparqlQueries(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        //Sparql queries are all empty for existing values
        //This form is different from the others that it gets multiple maintainers on the same page
        //and that information will be queried and stored in the additional form specific data
        HashMap<String, String> map = new HashMap<String, String>();
        editConfiguration.setSparqlForExistingUris(new HashMap<String, String>());
        editConfiguration.setSparqlForExistingLiterals(new HashMap<String, String>());
        editConfiguration.setSparqlForAdditionalUrisInScope(new HashMap<String, String>());
        editConfiguration.setSparqlForAdditionalLiteralsInScope(new HashMap<String, String>());
    }

    /**
     * Set Fields and supporting methods
     */

    public void setFields(EditConfigurationVTwo editConfiguration, VitroRequest vreq, String predicateUri) {
        setLabelField(editConfiguration);
        setFirstNameField(editConfiguration);
        setMiddleNameField(editConfiguration);
        setLastNameField(editConfiguration);
        setRankField(editConfiguration);
        setPersonUriField(editConfiguration);
    }

    private void setLabelField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                                       setName("label").
                                       setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                                       setRangeDatatypeUri(RDF.dtLangString.getURI())
        );
    }


    private void setFirstNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                                       setName("firstName").
                                       setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                                       setRangeDatatypeUri(RDF.dtLangString.getURI())
        );
    }


    private void setMiddleNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                                       setName("middleName").
                                       setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                                       setRangeDatatypeUri(RDF.dtLangString.getURI())
        );
    }

    private void setLastNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                                       setName("lastName").
                                       setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                                       setRangeDatatypeUri(RDF.dtLangString.getURI())
        );
    }

    private void setRankField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                                       setName("rank").
                                       setValidators(list("nonempty")).
                                       setRangeDatatypeUri(XSD.xint.toString())
        );
    }


    private void setPersonUriField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                                       setName("personUri")
                                   //.setObjectClassUri(personClass)
        );
    }

    //Form specific data
    public void addFormSpecificData(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        HashMap<String, Object> formSpecificData = new HashMap<String, Object>();
        //Get the existing maintainerships
        formSpecificData.put("existingMaintainerInfo",
                             getExistingMaintainerships(editConfiguration.getSubjectUri(), vreq));
        formSpecificData.put("newRank", getMaxRank(editConfiguration.getSubjectUri(), vreq) + 1);
        formSpecificData.put("rankPredicate", "http://vivoweb.org/ontology/core#rank");
        editConfiguration.setFormSpecificData(formSpecificData);
    }

    private static String MAINTAINERSHIPS_MODEL = ""
        + "PREFIX core: <http://vivoweb.org/ontology/core#>\n"
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
        + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
        + "CONSTRUCT\n"
        + "{\n"
        + "    ?subject core:relatedBy ?maintainershipURI .\n"
        + "    ?maintainershipURI a core:Maintainership .\n"
        + "    ?maintainershipURI core:relates ?maintainerURI .\n"
        + "    ?maintainershipURI core:rank ?rank.\n"
        + "    ?maintainerURI a foaf:Person .\n"
        + "    ?maintainerURI rdfs:label ?maintainerName .\n"
        + "}\n"
        + "WHERE\n"
        + "{\n"
        + "    {\n"
        + "        ?subject core:relatedBy ?maintainershipURI .\n"
        + "        ?maintainershipURI a core:Maintainership .\n"
        + "        ?maintainershipURI core:relates ?maintainerURI .\n"
        + "        ?maintainerURI a foaf:Person .\n"
        + "    }\n"
        + "    UNION\n"
        + "    {\n"
        + "        ?subject core:relatedBy ?maintainershipURI .\n"
        + "        ?maintainershipURI a core:Maintainership .\n"
        + "        ?maintainershipURI core:relates ?maintainerURI .\n"
        + "        ?maintainerURI a foaf:Person .\n"
        + "        ?maintainerURI rdfs:label ?maintainerName .\n"
        + "    }\n"
        + "    UNION\n"
        + "    {\n"
        + "        ?subject core:relatedBy ?maintainershipURI .\n"
        + "        ?maintainershipURI a core:Maintainership .\n"
        + "        ?maintainershipURI core:rank ?rank.\n"
        + "    }\n"
        + "}\n";

    private static String MAINTAINERSHIPS_QUERY = ""
        + "PREFIX core: <http://vivoweb.org/ontology/core#> \n"
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
        + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
        +
        "SELECT ?maintainershipURI (REPLACE(STR(?maintainershipURI),\"^.*(#)(.*)$\", \"$2\") AS ?maintainershipName) " +
        "?maintainerURI ?maintainerName ?rank \n"
        + "WHERE { \n"
        + "?subject core:relatedBy ?maintainershipURI . \n"
        + "?maintainershipURI a core:Maintainership . \n"
        + "?maintainershipURI core:relates ?maintainerURI . \n"
        + "?maintainerURI a foaf:Person . \n"
        + "OPTIONAL { ?maintainerURI rdfs:label ?maintainerName } \n"
        + "OPTIONAL { ?maintainershipURI core:rank ?rank } \n"
        + "} ORDER BY ?rank";


    private List<MaintainershipInfo> getExistingMaintainerships(String subjectUri, VitroRequest vreq) {
        RDFService rdfService = vreq.getRDFService();

        List<Map<String, String>> maintainerships = new ArrayList<Map<String, String>>();
        try {
            String constructStr = QueryUtils.subUriForQueryVar(MAINTAINERSHIPS_MODEL, "subject", subjectUri);

            Model constructedModel = ModelFactory.createDefaultModel();
            rdfService.sparqlConstructQuery(constructStr, constructedModel);

            String queryStr = QueryUtils.subUriForQueryVar(this.getMaintainershipsQuery(), "subject", subjectUri);
            log.debug("Query string is: " + queryStr);

            QueryExecution qe = QueryExecutionFactory.create(queryStr, constructedModel);
            try {
                ResultSet results = qe.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode node = soln.get("maintainershipURI");
                    if (node.isURIResource()) {
                        maintainerships.add(QueryUtils.querySolutionToStringValueMap(soln));
                    }
                }
            } finally {
                qe.close();
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        log.debug("maintainerships = " + maintainerships);
        return getMaintainershipInfo(maintainerships);
    }

    private static String MAX_RANK_QUERY = ""
        + "PREFIX core: <http://vivoweb.org/ontology/core#> \n"
        + "SELECT DISTINCT ?rank WHERE { \n"
        + "    ?subject core:relatedBy ?maintainership . \n"
        + "    ?maintainership a core:Maintainership . \n"
        + "    ?maintainership core:rank ?rank .\n"
        + "} ORDER BY DESC(?rank) LIMIT 1";

    private int getMaxRank(String subjectUri, VitroRequest vreq) {

        int maxRank = 0; // default value
        String queryStr = QueryUtils.subUriForQueryVar(this.getMaxRankQueryStr(), "subject", subjectUri);
        log.debug("maxRank query string is: " + queryStr);
        try {
            ResultSet results = QueryUtils.getQueryResults(queryStr, vreq);
            if (results != null && results.hasNext()) { // there is at most one result
                QuerySolution soln = results.next();
                RDFNode node = soln.get("rank");
                if (node != null && node.isLiteral()) {
                    // node.asLiteral().getInt() won't return an xsd:string that
                    // can be parsed as an int.
                    int rank = Integer.parseInt(node.asLiteral().getLexicalForm());
                    if (rank > maxRank) {
                        log.debug("setting maxRank to " + rank);
                        maxRank = rank;
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.error("Invalid rank returned from query: not an integer value.");
        } catch (Exception e) {
            log.error(e, e);
        }
        log.debug("maxRank is: " + maxRank);
        return maxRank;
    }

    private List<MaintainershipInfo> getMaintainershipInfo(
        List<Map<String, String>> maintainerships) {
        List<MaintainershipInfo> info = new ArrayList<MaintainershipInfo>();
        String maintainershipUri = "";
        String maintainershipName = "";
        String maintainerUri = "";
        String maintainerName = "";

        for (Map<String, String> maintainership : maintainerships) {
            for (Entry<String, String> entry : maintainership.entrySet()) {
                if (entry.getKey().equals("maintainershipURI")) {
                    maintainershipUri = entry.getValue();
                } else if (entry.getKey().equals("maintainershipName")) {
                    maintainershipName = entry.getValue();
                } else if (entry.getKey().equals("maintainerURI")) {
                    maintainerUri = entry.getValue();
                } else if (entry.getKey().equals("maintainerName")) {
                    maintainerName = entry.getValue();
                }
            }

            MaintainershipInfo aaInfo =
                new MaintainershipInfo(maintainershipUri, maintainershipName, maintainerUri, maintainerName);
            info.add(aaInfo);
        }
        log.debug("info = " + info);
        return info;
    }

    //This is the information about maintainer the form will require
    public class MaintainershipInfo {
        //This is the maintainership node information
        private String maintainershipUri;
        private String maintainershipName;
        //Maintainer information for maintainership node
        private String maintainerUri;
        private String maintainerName;

        public MaintainershipInfo(String inputMaintainershipUri,
                                  String inputMaintainershipName,
                                  String inputMaintainerUri,
                                  String inputMaintainerName) {
            maintainershipUri = inputMaintainershipUri;
            maintainershipName = inputMaintainershipName;
            maintainerUri = inputMaintainerUri;
            maintainerName = inputMaintainerName;

        }

        //Getters - specifically required for Freemarker template's access to POJO
        public String getMaintainershipUri() {
            return maintainershipUri;
        }

        public String getMaintainershipName() {
            return maintainershipName;
        }

        public String getMaintainerUri() {
            return maintainerUri;
        }

        public String getMaintainerName() {
            return maintainerName;
        }
    }

    static final String DEFAULT_NS_TOKEN = null; //null forces the default NS

    protected String getMaxRankQueryStr() {
        return MAX_RANK_QUERY;
    }

    protected String getMaintainershipsQuery() {
        return MAINTAINERSHIPS_QUERY;
    }

}
