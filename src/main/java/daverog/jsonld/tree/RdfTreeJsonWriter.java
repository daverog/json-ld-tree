package daverog.jsonld.tree;

import java.util.*;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import daverog.jsonld.tree.NameResolver.TypedResource;

import javax.annotation.Nullable;


public class RdfTreeJsonWriter {
		
	public String asJson(RdfTree tree) {
		tree.canonicalise();
		
		if (tree.isEmpty()) {
			return "{}";
		} if (tree.isList()) {
			LinkedHashMap<String, Object> json = Maps.newLinkedHashMap();
			
			List<Object> list = Lists.newArrayList();
			populateJsonArray(tree, list);
			json.put("results", list);
			
            SortedMap<String, SortedMap<String, String>> nameUriMap = Maps.newTreeMap(getPrefixedNameUriMap(tree));
            nameUriMap.put("results", new TreeMap(ImmutableMap.of("@id", "@graph")));

			json.put("@context", nameUriMap);
			
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			return gson.toJson(json);
		} else {
			LinkedHashMap<String, Object> json = Maps.newLinkedHashMap();
			populateJsonObject(tree, json);
			Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();
			return gson.toJson(json);
		}
	}
	
	private void populateJsonArray(RdfTree tree, List<Object> array) {
		for (RdfTree childTree: tree.getChildren()) {
			LinkedHashMap<String, Object> arrayItem = Maps.newLinkedHashMap();
			populateJsonObject(childTree, arrayItem);
			array.add(arrayItem);
		}
	}
	
	private String getName(RdfTree tree, Resource resource) {
		String name = tree.getNameResolver().getName(resource);
		if (name.equals("type")) name = "@type";
        else name = tree.getNameResolver().getPrefixedName(resource);
		return name;
	}

	private void populateJsonObject(RdfTree tree, LinkedHashMap<String, Object> json) {
		json.put("@id", tree.getNode().asResource().getURI());
		
		for (List<RdfTree> childTrees: getGroupedChildren(tree).values()) {
			if (!childTrees.isEmpty()) {
				RdfTree firstChildTree = childTrees.get(0);
				
				if (childTrees.size() == 1) {
					if (firstChildTree.isChildlessResource()) {
						if (firstChildTree.getPredicate() != null && firstChildTree.getPredicate().getURI().equals(RdfTree.RDF_TYPE)) {
							addPredicateValue(json, tree, firstChildTree, Lists.newArrayList(getName(tree, firstChildTree.getNode().asResource())));
						} else {
							addPredicateValue(json, tree, firstChildTree, getName(tree, firstChildTree.getNode().asResource()));
						}
					} else if (firstChildTree.getNode().isResource()) {
						LinkedHashMap<String, Object> child = Maps.newLinkedHashMap();
						populateJsonObject(firstChildTree, child);
						ArrayList<Object> array = Lists.newArrayList((Object)child);
						addPredicateValue(json, tree, firstChildTree, array);
					} else {
						addPredicateValue(json, tree, firstChildTree, convertToJsonLiteral(firstChildTree.getNode().asLiteral()));
					}
				} else {
					ArrayList<Object> array = Lists.newArrayList();
					for (RdfTree childTree: childTrees) {
						if (childTree.isChildlessResource()) {
							array.add(getName(tree, childTree.getNode().asResource()));
						} else if (childTree.getNode().isResource()) {
							LinkedHashMap<String, Object> child = Maps.newLinkedHashMap();
							populateJsonObject(childTree, child);
							array.add(child);
						} else {
							array.add(convertToJsonLiteral(childTree.getNode().asLiteral()));
						}
					}
					addPredicateValue(json, tree, firstChildTree, array);
				}
			}
		}
		
		SortedMap<String, SortedMap<String, String>> nameUriMap = getPrefixedNameUriMap(tree);
		if (tree.isRoot() && !nameUriMap.isEmpty()) {
			json.put("@context", nameUriMap);
		}
	}

	private void addPredicateValue(LinkedHashMap<String, Object> json,
			RdfTree tree, RdfTree childTree, Object value) {
		String predicateName = getName(tree, childTree.getPredicate());
		if (childTree.isInverse()) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Map<String, Object> reverse = (Map)json.get("@reverse");
			if (reverse == null) {
				reverse = Maps.<String, Object>newHashMap();
				json.put("@reverse", reverse);
			}
			reverse.put(predicateName, value);
		} else {
			json.put(predicateName, value);
		}
	}

	private Object convertToJsonLiteral(Literal literal) {
		if (Number.class.isAssignableFrom(literal.getValue().getClass())) {
			return literal.getValue();
		}
		return literal.getLexicalForm();
	}

	public Map<DirectionalPredicate, List<RdfTree>> getGroupedChildren(RdfTree tree) {
		Map<DirectionalPredicate, List<RdfTree>> groupedChildren = Maps.newLinkedHashMap();
		for (RdfTree childTree: tree.getChildren()) {
			List<RdfTree> childrenForDirectionalPredicate = groupedChildren.get(childTree.getDirectionalPredicate());
			if (childrenForDirectionalPredicate == null) {
				childrenForDirectionalPredicate = Lists.<RdfTree>newArrayList();
				groupedChildren.put(childTree.getDirectionalPredicate(), childrenForDirectionalPredicate);
			}
			
			childrenForDirectionalPredicate.add(childTree);
		}
		return groupedChildren;
	}
	
	public SortedMap<String, SortedMap<String, String>> getNameUriMap(RdfTree tree) {
		return Maps.transformValues(tree.getNameResolver().getMappedResources(),
                new Function<TypedResource, SortedMap<String, String>>() {
                    public SortedMap<String, String> apply(TypedResource resource) {
                        SortedMap<String, String> uriData = Maps.newTreeMap();
                        uriData.put("@id", resource.getResource().getNameSpace());
                        switch (resource.getType()) {
                            case VOCAB:
                                uriData.put("@type", "@vocab");
                                break;
                            case ID:
                                uriData.put("@type", "@id");
                                break;
                        }
                        return uriData;
                    }
                });
	}

    public SortedMap<String, SortedMap<String, String>> getPrefixedNameUriMap(RdfTree tree) {
        SortedMap<String, TypedResource> prefixedNameUriMap = new TreeMap<String, TypedResource>();
        for(Map.Entry<String, TypedResource> entry: tree.getNameResolver().getMappedResources().entrySet()) {
            Resource resource = entry.getValue().getResource();
            String name = tree.getNameResolver().getPrefixedName(resource);
            prefixedNameUriMap.put(name, entry.getValue());
        }

        return Maps.transformValues(prefixedNameUriMap,
                new Function<TypedResource, SortedMap<String, String>>() {
                    public SortedMap<String, String> apply(TypedResource resource) {
                        SortedMap<String, String> uriData = Maps.newTreeMap();
                        uriData.put("@id", resource.getResource().getURI());
                        switch (resource.getType()) {
                            case VOCAB:
                                uriData.put("@type", "@vocab");
                                break;
                            case ID:
                                uriData.put("@type", "@id");
                                break;
                        }
                        return uriData;
                    }
                });
    }

}
