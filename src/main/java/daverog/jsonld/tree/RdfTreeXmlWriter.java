package daverog.jsonld.tree;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.hp.hpl.jena.rdf.model.Resource;

public class RdfTreeXmlWriter {
	
	public String asXml(RdfTree tree) {
		if (tree.isEmpty()) return "<List/>";
		
		tree.canonicalise();

		Document document = DocumentHelper.createDocument();
		
		if (tree.isList()) {
			populateXmlList(tree, document);
		} else {
			populateXml(tree, document, document);
		}
		
		return generateXml(document);
	}
	
	public String asHtml(RdfTree tree, String relativeUrlBase) {
		if (tree.isEmpty()) return "<html><body>No data</body></html>";
		
		tree.canonicalise();
		Document document = DocumentHelper.createDocument();
		
		Element html = document.addElement(new QName("html"));
		Element body = html.addElement(new QName("body"));
		
		if (tree.isList()) {
			populateHtmlList(tree, document, body, relativeUrlBase);
		} else {
			populateHtml(tree, document, body, relativeUrlBase);
		}
		
		return generateXml(document);
	}

	private String generateXml(Document document) {
		StringWriter output = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setSuppressDeclaration(true);
		format.setOmitEncoding(true);
		format.setEncoding("UTF-8");
		format.setNewLineAfterDeclaration(false);
		format.setExpandEmptyElements(false);
		XMLWriter writer = new XMLWriter(output, format);
        try {
			writer.write(document);
		} catch (IOException e) {
			throw new RuntimeException("Error during XML serialisation");
		}
        return output.toString().trim();
	}
	
	private void populateXmlList(RdfTree tree, Document document) {
		Element list = document.addElement(new QName("List"));

        Integer totalResults = tree.getTotalResults();
        if (totalResults != null) {
            list.addAttribute("totalResults", totalResults.toString());
        }
		
		for (RdfTree childTree: tree.getChildren()) {
			populateXml(childTree, document, list);
		}
	}

	private void populateXml(RdfTree tree, Document document, Branch branch) {
        QName typeQName = tree.getType() == null ? new QName("Thing") : resourceAsQName(document, tree, tree.getType());
   		Element root =  branch.addElement(typeQName);
   		
		root.addAttribute(new QName("id"), tree.getNameResolver().getName(tree.getNode().asResource()));

		for (RdfTree childTree: tree.getChildren()) {
			if (!childTree.isType()) {
				QName predicateQName = resourceAsQName(document, tree, childTree.getPredicate());
				document.getRootElement().add(predicateQName.getNamespace());
				Element childElement = root.addElement(predicateQName);
				if (childTree.isInverse()) childElement.addAttribute("inverse", "true");
				if (childTree.isChildlessResource()) {
					childElement.addAttribute(new QName("id"), tree.getNameResolver().getName(childTree.getNode().asResource()));
				} else if (childTree.getNode().isResource()) {
					populateXml(childTree, document, childElement);
				} else {
					childElement.addText(childTree.getNode().asLiteral().getLexicalForm());
				}
			}
		}
	}
	
	private void populateHtmlList(RdfTree tree, Document document, Branch branch, String relativeUrlBase) {
		Element list = branch.addElement("ol");
		
		for (RdfTree childTree: tree.getChildren()) {
			Element listItem = list.addElement("li");
			populateHtml(childTree, document, listItem, relativeUrlBase);
		}
	}
	
	private void populateHtml(RdfTree tree, Document document, Branch branch, String relativeUrlBase) {
		Element link =  branch.addElement("a");
		link.addAttribute(new QName("href"), createRelativeLinkToResource(tree.getNode().asResource().getURI(), relativeUrlBase));
		link.addAttribute(new QName("title"), tree.getNode().asResource().getURI());
		if (tree.getNode().asResource().getURI() == null) link.addText("Result");
		else link.addText(tree.getNameResolver().getName(tree.getNode().asResource()));
		if (!tree.getChildren().isEmpty()) {
			Element children =  branch.addElement("ul");
			for (RdfTree childTree: tree.getChildren()) {
				Element childElement = children.addElement("li");
				String childName = tree.getNameResolver().getName(childTree.getPredicate());
				Element predicate = childElement.addElement("a");
				predicate.addAttribute(new QName("href"), createRelativeLinkToResource(childTree.getPredicate().getURI(), relativeUrlBase));
				predicate.addAttribute(new QName("title"), childTree.getPredicate().getURI());
				predicate.addText(childName + (childTree.isInverse() ? " (inverse)" : "") +  ":");
				String style = "font-weight: bold;";
				if (childTree.isInverse()) style = style + " font-style: italic";
    			predicate.addAttribute("style", style);

				Element value = childElement.addElement("span");
				if (childTree.getNode().isResource()) {
					populateHtml(childTree, document, value, relativeUrlBase);
				} else {
					value.addText(childTree.getNode().asLiteral().getLexicalForm());
				}
			}
		} 
	}

	private String createRelativeLinkToResource(String uri, String relativeUrlBase) {
		if (uri == null) return null;
		try {
			return relativeUrlBase + URLEncoder.encode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 not supported");
		}
	}

	private QName resourceAsQName(Document document, RdfTree tree, Resource resource) {
		return new QName(tree.getNameResolver().getName(resource));
	}
	

}
