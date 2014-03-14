package daverog.jsonld.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class RdfTree implements Comparable<RdfTree> {
	
	public static final String DEFAULT_RESULT_ONTOLOGY_URI_PREFIX = "http://purl.org/ontology/rdf-result/";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDF_TYPE = RDF_PREFIX + "type";
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";

	private final RdfTree parent;
	private List<RdfTree> children = Lists.newArrayList();
	
	private final boolean inverse;
	private final boolean list;
	private final RDFNode node;
	private final Property predicate;
	private final Model model;
	private final NameResolver nameResolver;
	private Resource type;
  HashMap<RDFNode, Integer> mapFromChildToDepth = null;
	private boolean constructed = false;
	
	public RdfTree(Model model, NameResolver nameResolver, RdfTree parent, RDFNode node, Property predicate, boolean inverse, HashMap<RDFNode, Integer> mapFromChildToDepth) {
		this.model = model;
		this.nameResolver = nameResolver;
		this.parent = parent;
		this.node = node;
		this.predicate = predicate;
		this.inverse = inverse;
    this.mapFromChildToDepth = mapFromChildToDepth;
		list = false;
	}
	
	public RdfTree(Model model, NameResolver nameResolver, RDFNode rootNode, HashMap<RDFNode, Integer> mapFromChildToDepth) {
		this.model = model;
		this.nameResolver = nameResolver;
		this.node = rootNode;
    this.mapFromChildToDepth = mapFromChildToDepth;
    list = false;
		predicate = null;
		inverse = false;
		parent = null;
	}

	public RdfTree(Model model, NameResolver nameResolver, HashMap<RDFNode, Integer> mapFromChildToDepth) {
		this.model = model;
		this.nameResolver = nameResolver;
    this.mapFromChildToDepth = mapFromChildToDepth;
		list = true;
    predicate = null;
		inverse = false;
		parent = null;
		node = null;
	}

	public void addChild(Statement statement) {
		RDFNode childNode = statement.getSubject();
		boolean inverse = true;
		if (statement.getSubject().equals(node)) {
			childNode = statement.getObject();
			inverse = false;
		}
		
		//Rule 1: Do not follow inverse type predicates. 
		//This prevents commonly typed resources in a graph from creating overly large trees
		if (inverse && statement.getPredicate().getURI().equals(RDF_TYPE)) return;

		//Rule 2: If a node is present as a parent node, do not continue with more children
		if (hasParentWithNode(childNode)) return;

		//Rule 3: If a parent's node is present as a list item of the root node, do not continue with more children
		//This allows a single generation of children when a list item is encountered
		if (parent != null && parent.getNode() != null && !parent.isList() && hasListRootWithNode(getNode())) return;

		//Rule 4: Do not follow the inverse of properties just followed if they lead to nodes that are list items
		//This prevents 'reference data' from forming join-points in RDF lists
		if (isInverse() != inverse && parent != null && getPredicate() != null &&
        getPredicate().equals(statement.getPredicate()) && hasListRootWithNode(childNode)) return;

		//Rule 5: Do not follow inverse properties if they lead to nodes that are
		//        closer to the root (but not necessarily a parent)
		//This prevents 'reference data' from forming join-points in RDF lists
    if (inverse && parent != null && getPredicate() != null) {
      int depthOfPotentialChild = getDepth() + 1;
      boolean hasSiblingOrAncestorSibling = depthOfPotentialChild >= getDepthInTree(node);
      if (hasSiblingOrAncestorSibling) return;
    }

    RdfTree rdfTree = new RdfTree(model, nameResolver, this, childNode, statement.getPredicate(), inverse, mapFromChildToDepth);
    children.add(rdfTree);
    mapFromChildToDepth.put(rdfTree.getNode(), getDepth());
  }

  public void addListItem(Resource listItem) {
		children.add(new RdfTree(model, nameResolver, this, listItem, null, false, mapFromChildToDepth));
	}

	public Property getPredicate() {
		return predicate;
	}
	
	public RDFNode getNode() {
		return node;
	}


	public List<RdfTree> getChildren() {
		return children;
	}
	
	public DirectionalPredicate getDirectionalPredicate() {
		return new DirectionalPredicate(getPredicate(), inverse);
	}

	public void setType(Resource type) {
		this.type = type;
	}

  public boolean hasParentWithNode(RDFNode node) {
    if (parent == null) return false;
    if (parent.getNode() == null) return false;
    if (parent.getNode().equals(node)) return true;
    return parent.hasParentWithNode(node);
  }

  public boolean hasListRootWithNode(RDFNode node) {
		if (parent == null) return false;
		if (parent.isList()) return parent.hasListItemWithNode(node);
		return parent.hasListRootWithNode(node);
	}

  private int getDepthInTree(RDFNode node) {
    return getRoot().getDepthOfNode(node);
  }

  private int getDepthOfNode(RDFNode nodeArg) {
    if (node != null && node.equals(nodeArg)) return 0;
    Integer depth = mapFromChildToDepth.get(nodeArg);
    if (depth == null) return -1;
    return depth;
  }

  private RdfTree getRoot() {
		if (parent == null) return this;
		return parent.getRoot();
	}

	/**
	 * Returns 0 for the list, 1 for each tree root, 
	 * and +1 for each child thereafter
	 */
	private int getDepth() {
		if (parent == null) return 1;
		if (parent.isList()) return 0;
		return parent.getDepth() + 1;
	}

	private boolean hasListItemWithNode(RDFNode node) {
		if (!list) return false;
		for (RdfTree childTree: children) {
			if (childTree.getNode().equals(node)) return true;
		}
		return false;
	}

	public void canonicalise() {
		for (RdfTree childTree: children) {
			childTree.canonicalise();
		}
		if (!list) Collections.sort(children);
	}


	private boolean isLiteral() {
		return getNode().isLiteral();
	}

	public boolean isType() {
		return getPredicate() != null && getPredicate().getURI().equals(RDF_TYPE) && !inverse;
	}

	public boolean isInverse() {
		return inverse;
	}

	public Resource getType() {
		return type;
	}

	@Override
	public int compareTo(RdfTree tree) {
		//Rules to ensure a predictable ordering of children of a tree
		if (isType() && tree.isType()) return 0;
		if (isType() && !tree.isType()) return -1;
		if (!isType() && tree.isType()) return 1;

		if (inverse && !tree.inverse) return 1;
		if (!inverse && tree.inverse) return -1;

		if (isLiteral() && !tree.isLiteral()) return -1;
		if (!isLiteral() && tree.isLiteral()) return 1;
		
		if (isChildlessResource() && !tree.isChildlessResource()) return -1;
		if (!isChildlessResource() && tree.isChildlessResource()) return 1;
		
		if (getPredicate().equals(tree.getPredicate())) {
			if (isLiteral() && tree.isLiteral()) 
				return RdfTreeUtils.compareObjects(
						getNode().asLiteral().getValue(),
						tree.getNode().asLiteral().getValue());
			return RdfTreeUtils.compareObjects(
					getNode(),
					tree.getNode());
		}
		
		return nameResolver.compareNames(getPredicate(), tree.getPredicate());
	}

	public String asXml() {
		return new RdfTreeXmlWriter().asXml(this);
	}
	
	public String asHtml(String relativeUrlBase) {
		return new RdfTreeXmlWriter().asHtml(this, relativeUrlBase);
	}
	
	public String asJson() {
		return new RdfTreeJsonWriter().asJson(this);
	}

	public boolean isList() {
		return list;
	}

	public NameResolver getNameResolver() {
		return nameResolver;
	}

	public boolean isChildlessResource() {
		return node.isResource() && children.isEmpty();
	}

	public boolean isRoot() {
		return parent == null;
	}

	public boolean isEmpty() {
		return children.isEmpty() && node == null;
	}

	/**
	 * Get the 'depth' of the resource,
	 * indicated by the URI, in this tree.
	 * 
	 * 0 = The root node matches this URI
	 * 1 = One of the tree's immediate child nodes
	 *     matches the URI
	 * 2 = Grandchildren
	 * 
	 * etc..
	 * 
	 * This is a relative depth, rather
	 * than an absolute depth, where it makes
	 * no difference whether this tree is the root
	 * or not.
	 */
	public int getDepthOf(String uri) {
		return getDepthOfNode(model.createResource(uri));
	}

	/**
	 * A fully constructed tree has been constructed, and all its
	 * decendents have been constructed
	 */
	public boolean isFullyConstructed() {
		if (children.isEmpty()) return constructed;
		boolean fullyConstructed = true;
		for (RdfTree childTree: children) {
			if (!childTree.isFullyConstructed()) fullyConstructed = false;
		}
		return fullyConstructed;
	}

	/**
	 * To be 'constructed' means that, based on the graph,
	 * every child tree or literal value has been identified
	 * based on the triple statements.
	 * 
	 * It does not necessarily mean that the children 
	 * themselves have been 'constructed'.
	 */
	public boolean isConstructed() {
		return constructed;
	}
	
	public void markAsConstructed() {
		constructed = true;
	}
	
}
