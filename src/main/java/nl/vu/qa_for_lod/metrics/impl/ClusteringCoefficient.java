/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Compute the local clustering coefficient of the nodes
 * 
 * Target distribution: flat line at 1
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class ClusteringCoefficient implements Metric {

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Clustering coefficient";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	// http://en.wikipedia.org/wiki/Clustering_coefficient
	public double getResult(Graph graph, Resource resource) {
		// Get all the neighbours, independently of the binding relation
		Set<Resource> neighbours = graph.getNeighbours(resource, null);

		double c = 0;
		if (neighbours.size() > 1) {
			for (Resource neighbourA : neighbours)
				for (Resource neighbourB : neighbours)
					if (!neighbourA.equals(neighbourB) && graph.containsEdge(neighbourA, neighbourB, true))
						c++;
			c = c / (neighbours.size() * (neighbours.size() - 1.0d));
		}

		return c;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getIdealDistribution(nl.vu.qa_for_lod
	 * .metrics.Distribution)
	 */
	public Distribution getIdealDistribution(Distribution inputDistribution) {
		// We want 0 for all the keys in the input distribution and the number
		// of nodes for a clustering coefficient of 1
		Distribution result = new Distribution();
		double total = 0;
		for (Double key : inputDistribution.keySet())
			total += inputDistribution.get(key);

		result.set(1, total);

		return result;
	}

}
