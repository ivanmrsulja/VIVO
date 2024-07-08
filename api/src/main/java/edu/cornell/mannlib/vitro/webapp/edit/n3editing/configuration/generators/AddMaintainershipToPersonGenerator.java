/* $This file is distributed under the terms of the license in LICENSE$ */
package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpSession;

import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldOptions;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.validators.AntiXssValidation;
import edu.cornell.mannlib.vitro.webapp.i18n.I18n;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils.EditMode;
import edu.cornell.mannlib.vitro.webapp.utils.generators.EditModeUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

public class AddMaintainershipToPersonGenerator extends VivoBaseGenerator implements
                                                                          EditConfigurationGenerator {

    public AddMaintainershipToPersonGenerator() {
    }

    @Override
    public EditConfigurationVTwo getEditConfiguration(VitroRequest vreq, HttpSession session) throws Exception {

        if (EditConfigurationUtils.getObjectUri(vreq) == null) {
            return doAddNew(vreq, session);
        } else {
            return doSkipToSoftware(vreq);
        }
    }

    private EditConfigurationVTwo doSkipToSoftware(VitroRequest vreq) {
        Individual maintainershipNode = EditConfigurationUtils.getObjectIndividual(vreq);

        //try to get the software
        String softwareQueryStr = "SELECT ?obj \n" +
            "WHERE { <" + maintainershipNode.getURI() + "> <http://vivoweb.org/ontology/core#relates> ?obj . \n" +
            "    ?obj a <http://purl.obolibrary.org/obo/ERO_0000071> . } \n";
        Query softwareQuery = QueryFactory.create(softwareQueryStr);
        QueryExecution qe = QueryExecutionFactory.create(softwareQuery, ModelAccess.on(vreq).getOntModel());
        try {
            ResultSetMem rs = new ResultSetMem(qe.execSelect());
            if (!rs.hasNext()) {
                return doBadMaintainershipNoSoftware(vreq);
            } else if (rs.size() > 1) {
                return doBadMaintainershipMultipleSoftware(vreq);
            } else {
                //skip to software
                RDFNode objNode = rs.next().get("obj");
                if (!objNode.isResource() || objNode.isAnon()) {
                    return doBadMaintainershipNoSoftware(vreq);
                }
                EditConfigurationVTwo editConfiguration = new EditConfigurationVTwo();
                editConfiguration.setSkipToUrl(UrlBuilder.getIndividualProfileUrl(((Resource) objNode).getURI(), vreq));
                return editConfiguration;
            }
        } finally {
            qe.close();
        }
    }

    protected EditConfigurationVTwo doAddNew(VitroRequest vreq,
                                             HttpSession session) throws Exception {

        EditConfigurationVTwo conf = new EditConfigurationVTwo();

        initBasics(conf, vreq);
        initPropertyParameters(vreq, session, conf);
        initObjectPropForm(conf, vreq);

        conf.setTemplate("addMaintainershipToPerson.ftl");

        conf.setVarNameForSubject("person");
        conf.setVarNameForPredicate("predicate");
        conf.setVarNameForObject("maintainership");

        conf.setN3Required(Arrays.asList(n3ForNewMaintainership));
        conf.setN3Optional(Arrays.asList(n3ForNewSoftwareAssertion,
                                         n3ForExistingSoftwareAssertion));

        conf.addNewResource("maintainership", DEFAULT_NS_FOR_NEW_RESOURCE);
        conf.addNewResource("newSoftware", DEFAULT_NS_FOR_NEW_RESOURCE);

        conf.setUrisOnform(Arrays.asList("existingSoftware", "softwareType"));
        conf.setLiteralsOnForm(Arrays.asList("softwareLabel", "softwareLabelDisplay"));

        conf.addSparqlForExistingLiteral("softwareLabel", softwareLabelQuery);

        conf.addSparqlForExistingUris("softwareType", softwareTypeQuery);
        conf.addSparqlForExistingUris("existingSoftware", existingSoftwareQuery);

        conf.addField(new FieldVTwo().
                          setName("softwareType").
                          setValidators(list("nonempty")).
                          setOptions(getSoftwareTypeLiteralOptions(vreq)));

        conf.addField(new FieldVTwo().
                          setName("softwareLabel").
                          setRangeDatatypeUri(RDF.dtLangString.getURI()).
                          setValidators(list("datatype:" + RDF.dtLangString.getURI()))
        );

        conf.addField(new FieldVTwo().
                          setName("softwareLabelDisplay").
                          setRangeDatatypeUri(XSD.xstring.toString()));

        conf.addValidator(new AntiXssValidation());
        addFormSpecificData(conf, vreq);

        prepare(vreq, conf);
        return conf;
    }

    /* N3 assertions  */

    final static String n3ForNewMaintainership =
        "@prefix vivo: <" + vivoCore + "> . \n" +
            "?person ?predicate ?maintainership . \n" +
            "?maintainership a  vivo:Maintainership . \n" +
            "?maintainership vivo:relates ?person . ";

    final static String n3ForNewSoftwareAssertion =
        "@prefix vivo: <" + vivoCore + "> . \n" +
            "?maintainership vivo:relates ?newSoftware . \n" +
            "?newSoftware a ?softwareType . \n" +
            "?newSoftware <" + label + "> ?softwareLabel. ";

    final static String n3ForExistingSoftwareAssertion =
        "@prefix vivo: <" + vivoCore + "> . \n" +
            "?maintainership vivo:relates ?existingSoftware . \n" +
            "?existingSoftware a ?softwareType . ";

    /* Queries for editing an existing entry */

    final static String softwareTypeQuery =
        "PREFIX vitro: <" + VitroVocabulary.vitroURI + "> \n" +
            "PREFIX vivo: <" + vivoCore + "> . \n" +
            "PREFIX bibo: <http://purl.org/ontology/bibo/> . \n" +
            "SELECT ?softwareType WHERE { \n" +
            "  ?maintainership vivo:relates ?existingSoftware . \n" +
            "  ?existingSoftware a <http://purl.obolibrary.org/obo/ERO_0000071> . \n" +
            "  ?existingSoftware vitro:mostSpecificType ?softwareType . \n" +
            "}";

    final static String softwareLabelQuery =
        "PREFIX vitro: <" + VitroVocabulary.vitroURI + "> \n" +
            "PREFIX vivo: <" + vivoCore + "> . \n" +
            "PREFIX bibo: <http://purl.org/ontology/bibo/> . \n" +
            "SELECT ?softwareLabel WHERE { \n" +
            "  ?maintainership vivo:relates ?existingSoftware . \n" +
            "  ?existingSoftware a <http://purl.obolibrary.org/obo/ERO_0000071> . \n" +
            "  ?existingSoftware <" + label + "> ?softwareLabel . \n" +
            "}";

    final static String existingSoftwareQuery =
        "PREFIX vitro: <" + VitroVocabulary.vitroURI + "> \n" +
            "PREFIX vivo: <" + vivoCore + "> . \n" +
            "PREFIX bibo: <http://purl.org/ontology/bibo/> . \n" +
            "SELECT ?existingSoftware WHERE { \n" +
            "  ?maintainership vivo:relates ?existingSoftware . \n" +
            "  ?existingSoftware a <http://purl.obolibrary.org/obo/ERO_0000071> . \n" +
            "}";

    //Adding form specific data such as edit mode
    public void addFormSpecificData(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        HashMap<String, Object> formSpecificData = new HashMap<String, Object>();
        formSpecificData.put("editMode", getEditMode(vreq).name().toLowerCase());
        editConfiguration.setFormSpecificData(formSpecificData);
    }

    public EditMode getEditMode(VitroRequest vreq) {
        List<String> predicates = new ArrayList<String>();
        predicates.add("http://vivoweb.org/ontology/core#relates");
        return EditModeUtils.getEditMode(vreq, predicates);
    }

    private EditConfigurationVTwo doBadMaintainershipMultipleSoftware(VitroRequest vreq) {
        return null;
    }

    private EditConfigurationVTwo doBadMaintainershipNoSoftware(VitroRequest vreq) {
        return null;
    }

    private FieldOptions getSoftwareTypeLiteralOptions(VitroRequest vreq) throws Exception {
        return GeneratorUtil.buildResourceAndLabelFieldOptions(
            vreq.getRDFService(), vreq.getWebappDaoFactory(), "",
            I18n.bundle(vreq).text("select_type"),
            "http://purl.obolibrary.org/obo/ERO_0000071");
    }

}
