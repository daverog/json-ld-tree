package daverog.jsonld.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import daverog.jsonld.tree.ModelUtils;
import daverog.jsonld.tree.TestResourceLoader;

import com.hp.hpl.jena.rdf.model.Model;

public class RdfTreeGeneratorTest {

	private RdfTreeGenerator generator;
	
	@Before
	public void setUp() {
		generator = new RdfTreeGenerator();
	}
	
	@Test
	public void ifNoTreeStartIsPresentAnErrorIsThrown() {
		try {
			Model model = ModelUtils.createJenaModel(
					"<uri:a> <uri:b> <uri:c> .");
			generator.generateRdfTree(model).asXml();
			fail("RdfTreeException expected");
		} catch(RdfTreeException e) {
			assertEquals("result:this is not present as the subject of a statement, so an RDF tree cannot be generated", e.getMessage());
		}
	}
	
	@Test
	public void anEntirelyEmptyModelResultsInEmptyJson() throws RdfTreeException {
		assertEquals("{}", generator.generateRdfTree(ModelUtils.createJenaModel("")).asJson());
	}
	
	@Test
	public void anEntirelyEmptyModelResultsInEmptyXml() throws RdfTreeException {
		assertEquals("<List/>", generator.generateRdfTree(ModelUtils.createJenaModel("")).asXml());
	}
	
	@Test
	public void ifMoreThanOneResultThisIsPresentAnErrorIsThrown() {
		try {
			Model model = ModelUtils.createJenaModel(
					"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
					"result:this result:item <uri:a> . \n" +
					"result:this result:item <uri:c> . \n" +
					"<uri:a> <uri:b> <uri:c> .");
			generator.generateRdfTree(model).asXml();
			fail("RdfTreeException expected");
		} catch(RdfTreeException e) {
			assertEquals("More than one result:this subject was found for a single item result", e.getMessage());
		}
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
	public void literalWithDatatypeStringIsIgnored() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				"@prefix result: <http://purl.org/ontology/rdf-result/> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"result:this result:item <uri:a> . \n" +
				"<uri:a> <uri:b> \"string\"^^xsd:string .\n" + 
				"<uri:a> <uri:b> \"string2\"^^xsd:string .\n" + 
				"<uri:a> <uri:c> \"string\"^^xsd:string .");
		assertEquals(
				"<Thing id=\"uri:a\">\n" +
				"  <uri:b>string</uri:b>\n" +
				"  <uri:b>string2</uri:b>\n" +
				"  <uri:c>string</uri:c>\n" +
				"</Thing>", 
			generator.generateRdfTree(model).asXml());
		assertEquals(
				"{\n" +
				"  \"@id\": \"uri:a\",\n" +
				"  \"uri:b\": [\n" +
				"    \"string\",\n" +
				"    \"string2\"\n" +
				"  ],\n" + 
				"  \"uri:c\": \"string\"\n" +
				"}", 
			generator.generateRdfTree(model).asJson());
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
	public void typeIsNotFollowedAsAnInverseProperty() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> a <uri:Thingy> ." +
			"<uri:b> a <uri:Thingy> .");
		assertEquals(
			"<uri:Thingy id=\"uri:a\"/>",
			generator.generateRdfTree(model).asXml());
	}
	
	@Test
	public void orphanedGraphsAreNotIncluded() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> <uri:b> <uri:c> ." +
			"<uri:orphanA> <uri:orphanB> <uri:orphanC> .");
		assertEquals(
				"<Thing id=\"uri:a\">\n" +
				"  <uri:b id=\"uri:c\"/>\n" +
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
	public void resourceTripleOverInversePropertyIsRenderedInJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:c> . \n" +
			"<uri:a> <uri:b> <uri:c> .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				"{\n" +
				"  \"@id\": \"uri:c\",\n" +
				"  \"@reverse\": {\n" +
				"    \"uri:b\": \"uri:a\"\n" +
				"  }\n" +
				"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void aCurieWithUnderscoreIsUsedIfALocalnameAppearsMoreThanOnce() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"@prefix ns: <http://purl.org/ns/> ." +
			"@prefix ns2: <http://purl.org/ns2/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> ns:prop <uri:c> .\n" +
			"<uri:a> ns2:prop <uri:c> .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"@id\": \"uri:a\",\n" + 
			"  \"ns2_prop\": \"uri:c\",\n" + 
			"  \"prop\": \"uri:c\",\n" + 
			"  \"@context\": {\n" + 
			"    \"ns2_prop\": {\n" + 
			"      \"@id\": \"http://purl.org/ns2/prop\",\n" + 
			"      \"@type\": \"@id\"\n" + 
			"    },\n" + 
			"    \"prop\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/prop\",\n" + 
			"      \"@type\": \"@id\"\n" + 
			"    }\n" + 
			"  }\n" + 
			"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void aDataTypesAreRenderedAsCorrectTypesInJSON() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> <uri:b> \"1982-02-25\"^^xsd:date .\n" +
			"<uri:a> <uri:c> \"1\"^^xsd:int .\n" +
			"<uri:a> <uri:d> \"1.0\"^^xsd:float .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				"{\n" +
				"  \"@id\": \"uri:a\",\n" +
				"  \"uri:b\": \"1982-02-25\",\n" +
				"  \"uri:c\": 1,\n" +
				"  \"uri:d\": 1.0\n" +
				"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void multipleValuesForOnePredicateResultInAListInJson() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> <uri:b> \"Val1\" .\n" +
			"<uri:a> <uri:b> \"Val2\" .");
		assertEquals(
				"{\n" +
				"  \"@id\": \"uri:a\",\n" +
				"  \"uri:b\": [\n" +
				"    \"Val1\",\n" +
				"    \"Val2\"\n" +
				"  ]\n" +
				"}", 
			generator.generateRdfTree(model).asJson());
	}
	
	@Test
	public void depthOfRootIsZero() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> <uri:b> \"Val1\" .");
		assertEquals(0, 
			generator.generateRdfTree(model).getDepthOf("uri:a"));
	}
	
	@Test
	public void depthOfChildIsOne() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> <uri:b> \"Val1\" ." +
			"<uri:a> <uri:b> <uri:c> .");
		assertEquals(1, 
			generator.generateRdfTree(model).getDepthOf("uri:c"));
	}
	
	@Test
	public void depthOfGrandchildChildIsTwo() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> <uri:b> \"Val1\" ." +
			"<uri:a> <uri:b> <uri:c> ." +
			"<uri:c> <uri:b> <uri:d> .");
		assertEquals(2, 
			generator.generateRdfTree(model).getDepthOf("uri:d"));
	}
	
	@Test
	public void listsAreShownAsJsonArrays() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:next <uri:a> . \n" +
			"<uri:a> result:next <uri:b> . \n" +
			"<uri:a> <uri:p> \"value1\" . \n" +
			"<uri:b> <uri:p> \"value2\" .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				"{\n" +
				"  \"results\": [\n" +
				"    {\n" +
				"      \"@id\": \"uri:a\",\n" +
				"      \"uri:p\": \"value1\"\n" +
				"    },\n"+ 
				"    {\n" +
				"      \"@id\": \"uri:b\",\n" +
				"      \"uri:p\": \"value2\"\n" +
				"    }\n"+ 
				"  ]\n" +
				"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void referencesToListItemsCauseTheTreeToStopNestingOverInverseProperties() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:next <uri:a> . \n" +
			"<uri:a> result:next <uri:b> . \n" +
			"<uri:a> <uri:p> <uri:b> . \n" +
			"<uri:a> <uri:v> \"value1\" . \n" +
			"<uri:b> <uri:v> \"value2\" .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"results\": [\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:a\",\n" + 
			"      \"uri:v\": \"value1\",\n" + 
			"      \"uri:p\": \"uri:b\"\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:b\",\n" + 
			"      \"uri:v\": \"value2\",\n" + 
			"      \"@reverse\": {\n" +
			"        \"uri:p\": \"uri:a\"\n" +
			"      }\n" +
			"    }\n" + 
			"  ]\n" + 
			"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void referencesToChildrenOfItemsThatAreHigherInTheHierachyCauseTheTreeToStopNestingOverInverseProperties() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:next <uri:a> . \n" +
			"<uri:a> result:next <uri:b> . \n" +
			"<uri:a> <uri:p> <uri:ref> . \n" +
			"<uri:b> <uri:p> <uri:bb> . \n" +
			"<uri:bb> <uri:pp> <uri:ref> .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"results\": [\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:a\",\n" + 
			"      \"uri:p\": \"uri:ref\"\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:b\",\n" + 
			"      \"uri:p\": [\n" +
			"        {\n" +
			"          \"@id\": \"uri:bb\",\n" + 
			"          \"uri:pp\": \"uri:ref\"\n" + 
			"        }\n" + 
			"      ]\n" + 
			"    }\n" + 
			"  ]\n" + 
			"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void referencesToChildrenOfItemsThatAreHigherInTheHierachyCauseTheTreeToStopNestingOverInversePropertiesDespiteListOrder() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:next <uri:b> . \n" +
			"<uri:b> result:next <uri:a> . \n" +
			"<uri:a> <uri:p> <uri:ref> . \n" +
			"<uri:b> <uri:p> <uri:bb> . \n" +
			"<uri:bb> <uri:pp> <uri:ref> .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"results\": [\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:b\",\n" + 
			"      \"uri:p\": [\n" +
			"        {\n" + 
			"          \"@id\": \"uri:bb\",\n" + 
			"          \"uri:pp\": \"uri:ref\"\n" + 
			"        }\n" + 
			"      ]\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:a\",\n" + 
			"      \"uri:p\": \"uri:ref\"\n" + 
			"    }\n" + 
			"  ]\n" + 
			"}", 
			rdfTree.asJson());
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
	public void predicateAreInAPredictableOrderWhichIsAlphabeticalExceptNonSingularPredicatesAndInversePredicates() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"@prefix ns: <http://purl.org/ns/> ." +
			"result:this result:item <uri:a> . \n" +
			"<uri:a> ns:bbLiteralGroup \"valA\" . \n" +
			"<uri:a> ns:aFirst \"val\" . \n" +
			"<uri:z> ns:aaaInverse <uri:a> . \n"+
			"<uri:a> ns:aaResourceGroup <uri:c> . \n" +
			"<uri:a> a ns:Thing . \n" +
			"<uri:a> ns:bSecond \"val\" . \n" +
			"<uri:a> ns:aaResourceGroup <uri:c> . \n" +
			"<uri:a> ns:bbLiteralGroup \"valB\" . ");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"@id\": \"uri:a\",\n" + 
			"  \"@type\": [\n" +
			"    \"Thing\"\n" +
			"  ],\n" + 
			"  \"aFirst\": \"val\",\n" + 
			"  \"bSecond\": \"val\",\n" + 
			"  \"bbLiteralGroup\": [\n" + 
			"    \"valA\",\n" + 
			"    \"valB\"\n" + 
			"  ],\n" + 
			"  \"aaResourceGroup\": \"uri:c\",\n" + 
			"  \"@reverse\": {\n" + 
			"    \"aaaInverse\": \"uri:z\"\n" + 
			"  },\n" + 
			"  \"@context\": {\n" + 
			"    \"Thing\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/Thing\"\n" + 
			"    },\n" + 
			"    \"aFirst\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/aFirst\"\n" + 
			"    },\n" + 
			"    \"aaResourceGroup\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/aaResourceGroup\",\n" + 
			"      \"@type\": \"@id\"\n" + 
			"    },\n" + 
			"    \"aaaInverse\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/aaaInverse\",\n" + 
			"      \"@type\": \"@id\"\n" + 
			"    },\n" + 
			"    \"bSecond\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/bSecond\"\n" + 
			"    },\n" + 
			"    \"bbLiteralGroup\": {\n" + 
			"      \"@id\": \"http://purl.org/ns/bbLiteralGroup\"\n" + 
			"    }\n" + 
			"  }\n" + 
			"}",
			rdfTree.asJson());
	}

	@Test
	public void blankNodesAsObjectAreHandledCorrectly() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> .\n" + 
			"result:this result:item <urn:a> .\n" + 
			"<urn:a> a _:blankNode .");
		assertEquals(
			"{\n" + 
			"  \"@id\": \"urn:a\",\n" + 
			"  \"@type\": [\n" +
			"    \"@blank\"\n" +
			"  ]\n" + 
			"}", 
			generator.generateRdfTree(model).asJson());
	}
	
	@Test
	public void blankNodesAsSubjectsAreHandledCorrectly() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> .\n" + 
					"result:this result:item _:blankNode .\n" + 
			"_:blankNode a <urn:a>  .");
		assertEquals(
			"{\n" + 
			"  \"@type\": [\n" +
			"    \"urn:a\"\n" +
			"  ]\n" + 
			"}", 
			generator.generateRdfTree(model).asJson());
	}
	
	
	@Test
	public void anAthleteIsRenderedAsATree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie.xml"), 
		rdfTree.asXml());
		System.out.println(rdfTree.asJson());
	}
	
	@Test
	public void anAthleteIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie.json"), 
				rdfTree.asJson());
	}
	
	@Test
	public void athletesInAListAreRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/athletes.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/athletes.json"), 
				rdfTree.asJson());
	}
	
	@Test
	public void anSportTeamIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie-different-tree-start.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/ben-ainslie-different-tree-start.json"), 
				rdfTree.asJson());
	}	
	
	@Test
	public void anCreativeWorkIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/creativework.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/creativework.json"), 
				rdfTree.asJson());
	}	
	
	@Test
	public void aCreativeWorkIsRenderedAsHtml() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/creativework.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/creativework.html"), 
				rdfTree.asHtml("/things?uri="));
	}	
	
	@Test
	public void aListOfCreativeWorksIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/creative-works.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/creative-works.json"), 
				rdfTree.asJson());
	}	
	
	
	@Test
	public void aListDescribedAsASetOfItemsWithAnOrderByPredicateIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:listItem <uri:a> . \n" +
			"result:this result:listItem <uri:b> . \n" +
			"result:this result:listItem <uri:c> . \n" +
			"result:this result:orderByPredicate <uri:p> . \n" +
			"<uri:a> <uri:p> \"ccc\" . \n" +
			"<uri:b> <uri:p> \"bbb\" . \n" +
			"<uri:b> <uri:p> \"zzz\" . \n" +
			"<uri:c> <uri:p> \"aaa\" ."); // Note how the value order is reverse to the uri letter
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"results\": [\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:c\",\n" + 
			"      \"uri:p\": \"aaa\"\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:b\",\n" + 
			"      \"uri:p\": [\n" +
			"        \"bbb\",\n" + 
			"        \"zzz\"\n" +
			"      ]\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:a\",\n" + 
			"      \"uri:p\": \"ccc\"\n" + 
			"    }\n" + 
			"  ]\n" + 
			"}", 
			rdfTree.asJson());
	}
	
	
	@Test
	public void aListDescribedAsASetOfItemsWithAnOrderByPredicateAndDesendingSortOrderIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"result:this result:listItem <uri:a> . \n" +
			"result:this result:listItem <uri:b> . \n" +
			"result:this result:listItem <uri:c> . \n" +
			"result:this result:orderByPredicate <uri:p> . \n" +
			"result:this result:sortOrder result:DescendingOrder . \n" +
			"<uri:a> <uri:p> \"ccc\" . \n" +
			"<uri:b> <uri:p> \"bbb\" . \n" +
			"<uri:b> <uri:p> \"zzz\" . \n" +
			"<uri:c> <uri:p> \"aaa\" ."); 
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"results\": [\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:a\",\n" + 
			"      \"uri:p\": \"ccc\"\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:b\",\n" + 
			"      \"uri:p\": [\n" +
			"        \"bbb\",\n" + 
			"        \"zzz\"\n" +
			"      ]\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:c\",\n" + 
			"      \"uri:p\": \"aaa\"\n" + 
			"    }\n" + 
			"  ]\n" + 
			"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void aListOfDatesDescribedAsASetOfItemsWithAnOrderByPredicateIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
			"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." +
			"result:this result:listItem <uri:a> . \n" +
			"result:this result:listItem <uri:b> . \n" +
			"result:this result:orderByPredicate <uri:p> . \n" +
			"result:this result:sortOrder result:DescendingOrder . \n" +
			"<uri:a> <uri:p> \"2013-07-12T10:10:34+00:00\"^^xsd:datetime . \n" +
			"<uri:b> <uri:p> \"2013-07-10T15:07:53+00:00\"^^xsd:datetime .");
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
			"{\n" + 
			"  \"results\": [\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:b\",\n" + 
			"      \"uri:p\": \"2013-07-10T15:07:53+00:00\"\n" + 
			"    },\n" + 
			"    {\n" + 
			"      \"@id\": \"uri:a\",\n" + 
			"      \"uri:p\": \"2013-07-12T10:10:34+00:00\"\n" + 
			"    }\n" + 
			"  ]\n" + 
			"}", 
			rdfTree.asJson());
	}
	
	@Test
	public void aListDescribedAsASetOfItemsWithNoOrderByPredicateIsOrderedByAnyAvailableStringFollowedByLiteralsAndThenResourcesAndIsRenderedAsAJsonTree() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				"@prefix result: <http://purl.org/ontology/rdf-result/> ." +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"result:this result:listItem <uri:a> . \n" +
				"result:this result:listItem <uri:b> . \n" +
				"result:this result:listItem <uri:c> . \n" +
				"<uri:a> <uri:p> \"ccc\" . \n" +
				"<uri:b> <uri:p> \"bbb\" . \n" +
				"<uri:b> <uri:p> \"zzz\" . \n" +
				"<uri:b> <uri:p> <a:a>. \n" +
				"<uri:b> <uri:p> \"2\"^^xsd:integer. \n" +
				"<uri:c> <uri:p> \"aaa\" ."); // Note how the value order is reverse to the uri letter
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				"{\n" + 
						"  \"results\": [\n" + 
						"    {\n" + 
						"      \"@id\": \"uri:c\",\n" + 
						"      \"uri:p\": \"aaa\"\n" + 
						"    },\n" + 
						"    {\n" + 
						"      \"@id\": \"uri:b\",\n" + 
						"      \"uri:p\": [\n" +
						"        2,\n" + 
						"        \"bbb\",\n" + 
						"        \"zzz\",\n" + 
						"        \"a:a\"\n" +
						"      ]\n" + 
						"    },\n" + 
						"    {\n" + 
						"      \"@id\": \"uri:a\",\n" + 
						"      \"uri:p\": \"ccc\"\n" + 
						"    }\n" + 
						"  ]\n" + 
						"}", 
						rdfTree.asJson());
	}
	
	@Test
	public void correct_order_for_10_creative_works() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/10-creative-works.ttl"));
		RdfTree rdfTree = generator.generateRdfTree(model);
		assertEquals(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/10-creative-works.json"), 
				rdfTree.asJson());
	}

}
