/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class MetricData {
	static final Logger logger = LoggerFactory.getLogger(MetricData.class);

	/** Distance to ideal value */
	private Map<MetricState, Double> distanceMap = new HashMap<MetricState, Double>();

	/** Distribution of the results */
	private Map<MetricState, Distribution> distributionMap = new HashMap<MetricState, Distribution>();

	/** Raw results mapping every node to a value */
	private Map<MetricState, Results> resultsMap = new HashMap<MetricState, Results>();

	/** Concurrent access lock */
	private ReentrantLock lock = new ReentrantLock(false);

	/**
	 * 
	 */
	public void clear() {
		resultsMap.clear();
		distributionMap.clear();
		distanceMap.clear();
	}

	/**
	 * @param state
	 * @return
	 */
	public Distribution getDistribution(MetricState state) {
		if (distributionMap.get(state) == null) {
			Distribution distribution = new Distribution();
			for (Entry<Resource, Double> r : resultsMap.get(state).entrySet())
				distribution.increaseCounter(r.getValue());
			distributionMap.put(state, distribution);
		}
		return distributionMap.get(state);
	}

	/**
	 * @return
	 */
	public double getRatioDistanceChange() {
		if (distanceMap.get(MetricState.BEFORE) == 0)
			return 0;

		return 100 * (distanceMap.get(MetricState.AFTER) / distanceMap.get(MetricState.BEFORE));
	}

	/**
	 * Return the list of changes in the values of the nodes
	 * 
	 * @param maximumResults
	 *            The number of nodes to return
	 * @param resources
	 * @return an ordered list of the top suspicious nodes according to the
	 *         metric
	 */
	public Map<Resource, Double> getNodeChanges() {
		// Get the results
		Results resultsBefore = resultsMap.get(MetricState.BEFORE);
		Results resultsAfter = resultsMap.get(MetricState.AFTER);

		// Get the list of nodes for which we have before and after results
		Set<Resource> keys = new HashSet<Resource>(resultsBefore.keySet());
		keys.retainAll(resultsAfter.keySet());

		// Compare
		Map<Resource, Double> diffs = new HashMap<Resource, Double>();
		for (Resource key : keys) {
			//double ratio = 0;
			//if (resultsBefore.get(key) != 0)
			//	ratio = 100 * ((resultsAfter.get(key) - resultsBefore.get(key)) / resultsBefore.get(key));
			diffs.put(key, resultsAfter.get(key) - resultsBefore.get(key));
		}

		// Get the ordered list of nodes
		Map<Resource, Double> output = new LinkedHashMap<Resource, Double>();
		TreeSet<Double> scores = new TreeSet<Double>(diffs.values());
		Iterator<Double> it = scores.descendingIterator();
		while (it.hasNext()) {
			Double v = it.next();
			for (Entry<Resource, Double> entry : diffs.entrySet())
				if (entry.getValue().equals(v))
					output.put(entry.getKey(), entry.getValue());
		}
		
		return output;
	}

	/**
	 * @return
	 */
	public boolean isGreen() {
		return distanceMap.get(MetricState.AFTER) <= distanceMap.get(MetricState.BEFORE);
	}

	/**
	 * @param state
	 * @param distanceToIdealDistribution
	 */
	public void setDistanceToIdeal(MetricState state, double distanceToIdealDistribution) {
		distanceMap.put(state, distanceToIdealDistribution);
	}

	/**
	 * @param state
	 * @param node
	 * @param value
	 */
	public void setResult(MetricState state, Resource node, Double value) {
		lock.lock();

		// Get, and init if needed, the results table
		Results results = resultsMap.get(state);
		if (results == null) {
			results = new Results();
			resultsMap.put(state, results);
		}

		// Save the result
		results.put(node, value);

		// Invalidate the distributions and distances
		distributionMap.put(state, null);
		distanceMap.put(state, null);

		lock.unlock();
	}
}
