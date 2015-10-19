package daverog.jsonld.tree;

import com.hp.hpl.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RdfTreeGeneratorXmlTest {
    private RdfTreeGenerator generator;

    @Before
    public void setUp() {
        generator = new RdfTreeGenerator();
    }

    @Test
    public void anEntirelyEmptyModelResultsInEmptyXml() throws RdfTreeException {
        assertEquals("<List/>", generator.generateRdfTree(ModelUtils.createJenaModel("")).asXml());
    }

    @Test
    public void resourceTripleIsRenderedInTree() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "result:this result:item <uri:a> . \n" +
                        "<uri:a> <uri:b> <uri:c> .");
        assertEquals(
                "<Thing id=\"uri:a\">\n" +
                        "  <uri:b id=\"uri:c\"/>\n" +
                        "</Thing>",
                generator.generateRdfTree(model).asXml());
    }

    @Test
    public void literalWithNonXmlCharactersTripleIsRenderedInXmlTree() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "result:this result:item <uri:a> . \n" +
                        "<uri:a> <uri:b> \"'string & - string'\" .");
        assertEquals(
                "<Thing id=\"uri:a\">\n" +
                        "  <uri:b>'string &amp; - string'</uri:b>\n" +
                        "</Thing>",
                generator.generateRdfTree(model).asXml());
    }

    @Test
    public void literalsWithIdenticalLocalNamesAreRenderedInXmlTree() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "" +
                        "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> ." +
                        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." +
                        "result:this result:item <uri:a> . \n" +
                        "<uri:a> rdfs:label \"RDFS label\" . \n" +
                        "<uri:a> skos:label \"SKOS label\" .");
        assertEquals(
                "<Thing id=\"uri:a\">\n" +
                        "  <label>RDFS label</label>\n" +
                        "  <skos_label>SKOS label</skos_label>\n" +
                        "</Thing>",
                generator.generateRdfTree(model).asXml());
    }

    @Test
    public void literalsWithIdenticalLocalNamesOneInNameOverridesAreRenderedInXmlTree() throws RdfTreeException {
        Map<String,String> nameOverrides = new HashMap<String,String>();
        nameOverrides.put("http://www.w3.org/2000/01/rdf-schema#label", "label");

        Model model = ModelUtils.createJenaModel(
                "" +
                        "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." +
                        "@prefix core: <http://www.bbc.co.uk/ontologies/coreconcepts/> ." +
                        "result:this result:item <uri:a> . \n" +
                        "<uri:a> rdfs:label \"RDFS label\" . \n" +
                        "<uri:a> core:label \"Core label\" . \n");
        assertEquals(
                "" +
                        "<Thing id=\"uri:a\">\n" +
                        "  <core_label>Core label</core_label>\n" +
                        "  <label>RDFS label</label>\n" +
                        "</Thing>",
                generator.generateRdfTree(model, nameOverrides).asXml());
    }

    @Test
    public void nameOverridesAreReservedInXmlTree() throws RdfTreeException {
        Map<String,String> nameOverrides = new HashMap<String,String>();
        nameOverrides.put("http://www.w3.org/2000/01/rdf-schema#label", "label");

        Model model = ModelUtils.createJenaModel(
                "" +
                        "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "@prefix core: <http://www.bbc.co.uk/ontologies/coreconcepts/> ." +
                        "result:this result:item <uri:a> . \n" +
                        "<uri:a> core:label \"Core label\" . \n");
        assertEquals(
                ""+
                        "<Thing id=\"uri:a\">\n" +
                        "  <core_label>Core label</core_label>\n" +
                        "</Thing>",
                generator.generateRdfTree(model, nameOverrides).asXml());
    }

    @Test
    public void loopTripleIsRenderedInTree() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "result:this result:item <uri:a> . \n" +
                        "<uri:a> <uri:b> <uri:a> .");
        assertEquals(
                "<Thing id=\"uri:a\">\n" +
                        "  <uri:b id=\"uri:a\"/>\n" +
                        "</Thing>",
                generator.generateRdfTree(model).asXml());
    }

    @Test
    public void resourceTripleOverInversePropertyIsRenderedInTree() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "result:this result:item <uri:c> . \n" +
                        "<uri:a> <uri:b> <uri:c> .");
        RdfTree rdfTree = generator.generateRdfTree(model);
        assertEquals(
                "<Thing id=\"uri:c\">\n" +
                        "  <uri:b inverse=\"true\" id=\"uri:a\"/>\n" +
                        "</Thing>",
                rdfTree.asXml());
    }

    @Test
    public void anAthleteIsRenderedAsATree() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie.ttl"));
        RdfTree rdfTree = generator.generateRdfTree(model);
        assertEquals(
                TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie.xml"),
                rdfTree.asXml());
    }

    @Test
    public void listsAreShownAsXmlLists() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "result:this result:next <uri:a> . \n" +
                        "<uri:a> result:next <uri:b> . \n" +
                        "<uri:a> <uri:p> \"value1\" . \n" +
                        "<uri:b> <uri:p> \"value2\" .");
        assertEquals(
                "<List>\n" +
                        "  <Thing id=\"uri:a\">\n" +
                        "    <uri:p>value1</uri:p>\n" +
                        "  </Thing>\n" +
                        "  <Thing id=\"uri:b\">\n" +
                        "    <uri:p>value2</uri:p>\n" +
                        "  </Thing>\n" +
                        "</List>",
                generator.generateRdfTree(model).asXml());
    }

    @Test
    public void listsWithTotalResultsAreShownAsXmlLists() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel(
                "@prefix result: <http://purl.org/ontology/rdf-result/> ." +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." +
                        "result:meta result:totalResults \"2\"^^xsd:integer ." +
                        "result:this result:next <uri:a> . \n" +
                        "<uri:a> result:next <uri:b> . \n" +
                        "<uri:a> <uri:p> \"value1\" . \n" +
                        "<uri:b> <uri:p> \"value2\" .");
        assertEquals(
                "<List totalResults=\"2\">\n" +
                        "  <Thing id=\"uri:a\">\n" +
                        "    <uri:p>value1</uri:p>\n" +
                        "  </Thing>\n" +
                        "  <Thing id=\"uri:b\">\n" +
                        "    <uri:p>value2</uri:p>\n" +
                        "  </Thing>\n" +
                        "</List>",
                generator.generateRdfTree(model).asXml());
    }
}
