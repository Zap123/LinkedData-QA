/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.vu.qa_for_lod.graph.Graph;

import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.preview.api.ColorizerFactory;
import org.gephi.preview.api.EdgeColorizer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.gephi.statistics.plugin.PageRank;
import org.openide.util.Lookup;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://gephi.org/docs/toolkit/org/gephi/statistics/spi/Statistics.html
// http://wiki.gephi.org/index.php/Toolkit_-_Statistics_and_Metrics
public class GephiGraph implements Graph {
	private DirectedGraph directedGraph;
	private GraphModel graphModel;
	private final Map<Node, Resource> nodeToResource = new HashMap<Node, Resource>();
	private final RDFDataProvider fetcher;

	/**
	 * Init Gephi and create a graph
	 */
	public GephiGraph() {
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		directedGraph = graphModel.getDirectedGraph();

		// Get a data fetcher
		fetcher = new RDFDataProvider();
	}

	/**
	 * @param resource
	 * @return
	 */
	private Node addNode(Resource resource) {
		Node node = graphModel.factory().newNode(resource.getURI());
		node.getNodeData().setLabel(resource.getURI());
		directedGraph.addNode(node);
		nodeToResource.put(node, resource);
		return node;
	}

	/**
	 * @param statement
	 * @param size
	 *            Use size to mark seed VS extra nodes
	 */
	public void addStatement(Statement statement) {
		Resource subject = statement.getSubject();
		Property predicate = statement.getPredicate();
		RDFNode object = statement.getObject();
		if (!(object instanceof Resource))
			return;

		// Add the triple to the Gephi graph
		Node sNode = graphModel.getGraph().getNode(subject.getURI());
		if (sNode == null)
			sNode = addNode(subject);
		Node oNode = graphModel.getGraph().getNode(object.asResource().getURI());
		if (oNode == null)
			oNode = addNode(object.asResource());
		Edge edge = graphModel.factory().newEdge(sNode, oNode, 1.0f, true);
		edge.getEdgeData().setLabel(predicate.getURI());
		directedGraph.addEdge(edge);
	}

	/**
	 * @param statements
	 */
	public void addStatements(Collection<Statement> statements) {
		for (Statement statement : statements)
			this.addStatement(statement);
	}

	/**
	 * 
	 */
	public void clear() {
		directedGraph.clear();
	}

	/**
	 * @param resource
	 * @return
	 */
	public boolean containsResource(Resource resource) {
		if (resource == null)
			return false;

		return (graphModel.getGraph().getNode(resource.getURI()) != null);
	}

	/**
	 * @param string
	 * 
	 */
	public void dump(String string) {
		// Configure the rendering of the graph
		// (from http://wiki.gephi.org/index.php/Toolkit_-_Export_graph)
		PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
		model.getNodeSupervisor().setShowNodeLabels(Boolean.TRUE);
		ColorizerFactory colorizerFactory = Lookup.getDefault().lookup(ColorizerFactory.class);
		model.getUniEdgeSupervisor().setColorizer(
				(EdgeColorizer) colorizerFactory.createCustomColorMode(Color.LIGHT_GRAY));
		model.getBiEdgeSupervisor().setColorizer((EdgeColorizer) colorizerFactory.createCustomColorMode(Color.GRAY));
		model.getUniEdgeSupervisor().setEdgeScale(0.1f);
		model.getBiEdgeSupervisor().setEdgeScale(0.1f);
		model.getNodeSupervisor().setBaseNodeLabelFont(model.getNodeSupervisor().getBaseNodeLabelFont().deriveFont(8));

		// Export the graph in gexf and pdf
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		try {
			ec.exportFile(new File(string + ".pdf"));
			ec.exportFile(new File(string + ".gexf"));
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
	}

	/**
	 * @param resource
	 * @return
	 */
	public double getDegree(Resource resource) {
		Node n = directedGraph.getNode(resource.toString());
		return directedGraph.getDegree(n);
	}

	/**
	 * @param resource
	 * @return
	 */
	public double getPopularity(Resource resource) {
		// Get graph model and attribute model of current workspace
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();

		// Compute PageRank
		PageRank pageRank = new PageRank();
		pageRank.setUndirected(false);
		pageRank.execute(graphModel, attributeModel);

		// Get the result for the requested resource
		int col = attributeModel.getNodeTable().getColumn(PageRank.PAGERANK).getIndex();
		Node n = directedGraph.getNode(resource.toString());
		Double popularity = (Double) n.getNodeData().getAttributes().getValue(col);

		return popularity.doubleValue();
	}

	/**
	 * Output some stats
	 */
	public String getStats() {
		int deadEnds = 0;
		for (Node node : directedGraph.getNodes())
			if (directedGraph.getInDegree(node) > 1)
				if (directedGraph.getOutDegree(node) == 0)
					deadEnds++;

		StringBuffer buffer = new StringBuffer();
		buffer.append("Nodes: ").append(directedGraph.getNodeCount());
		buffer.append(", Edges: ").append(directedGraph.getEdgeCount());
		buffer.append(", Dead end: ").append(deadEnds);

		return buffer.toString();
	}

	/**
	 * @param resource
	 */
	// TODO Parallelise this
	public void initFromResource(Resource resource) throws NotFoundException {

		// Load the data around the resource
		Collection<Statement> statements = fetcher.get(resource);
		if (statements.size() == 0)
			throw new NotFoundException("No information found for " + resource);
		addStatements(statements);

		// Connect the neighbours among them if possible, otherwise will create
		// new nodes
		Node n = directedGraph.getNode(resource.getURI());
		Edge[] edges = directedGraph.getEdges(n).toArray();
		System.out.println(edges.length);
		for (Edge edge : edges) {
			Node neighbour = (edge.getSource().equals(n) ? edge.getTarget() : edge.getSource());
			for (Statement stmt : fetcher.get(nodeToResource.get(neighbour)))
				addStatement(stmt);
			// if (this.containsResource(stmt.getObject().asResource()))
		}
	}

	/**
	 * @param resource
	 * @return
	 */
	public Set<Resource> getNeighbours(Resource resource) {
		Set<Resource> neighbours = new HashSet<Resource>();
		Node n = directedGraph.getNode(resource.getURI());
		for (Edge edge : directedGraph.getEdges(n)) {
			Node neighbour = (edge.getSource().equals(n) ? edge.getTarget() : edge.getSource());
			neighbours.add(nodeToResource.get(neighbour));
		}
		return neighbours;
	}

	/**
	 * @param resource
	 * @return
	 */
	public Set<Resource> getNeighbours(Resource resource, String property) {
		Set<Resource> neighbours = new HashSet<Resource>();
		Node n = directedGraph.getNode(resource.getURI());
		for (Edge edge : directedGraph.getEdges(n)) {
			Node neighbour = (edge.getSource().equals(n) ? edge.getTarget() : edge.getSource());
			if (edge.getEdgeData().getLabel().equals(property))
				neighbours.add(nodeToResource.get(neighbour));
		}
		return neighbours;
	}

	/**
	 * @param neighbourA
	 * @param neighbourB
	 * @return
	 */
	public boolean containsEdge(Resource fromResource, Resource toResource) {
		Node nodeFrom = directedGraph.getNode(fromResource.getURI());
		Node nodeTo = directedGraph.getNode(toResource.getURI());

		boolean res = false;
		for (Edge edge : directedGraph.getEdges(nodeFrom))
			if (edge.getSource().equals(nodeFrom) && edge.getTarget().equals(nodeTo))
				res = true;

		return res;
	}

	/**
	 * 
	 */
	public void close() {
		// Close the data fetcher
		fetcher.close();
	}

	/**
	 * @param resource
	 * @param seedFile
	 */
	public void addExtraLinksForResource(Resource resource, ExtraLinks seedFile) {
		for (Statement statement : seedFile.getStatements()) {
			Resource other = null;
			if (statement.getSubject().equals(resource))
				other = statement.getObject().asResource();
			else if (statement.getObject().asResource().equals(resource))
				other = statement.getSubject();

			if (other != null) {
				boolean expand = false;

				if (!containsResource(resource))
					expand = true;
				addStatement(statement);
				if (expand) 
					for (Statement stmt : fetcher.get(other))
						addStatement(stmt);
			}
		}
	}

	/* (non-Javadoc)
	 * @see nl.vu.qa_for_lod.graph.Graph#getNumberNodes()
	 */
	public int getNumberNodes() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see nl.vu.qa_for_lod.graph.Graph#getNumberEdges()
	 */
	public int getNumberEdges() {
		// TODO Auto-generated method stub
		return 0;
	}
}

// Clean the graph from the nodes which are dead ends
/*
 * Collection<Node> nodes = new ArrayList<Node>(); do { nodes.clear(); for (Node
 * node : directedGraph.getNodes()) if (directedGraph.getInDegree(node) > 0) if
 * (directedGraph.getOutDegree(node) == 0) nodes.add(node); for (Node node :
 * nodes) directedGraph.removeNode(node); } while (nodes.size() != 0);
 */
