package daverog.jsonld.tree;

import com.hp.hpl.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RdfTreeGeneratorXmlTest {
    private RdfTreeGenerator generator;

    @Before
    public void setUp() {
        generator = new RdfTreeGenerator();
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
