/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Distribution {
	public static enum DistributionAxis {
		X, Y
	}

	// FIXME Ugly trick to round the numbers
	private final static DecimalFormat df = new DecimalFormat("######.##");
	private final Map<Double, Double> data = new HashMap<Double, Double>();

	/**
	 * Compute the distance between the two distributions
	 * 
	 * @param other
	 *            the distribution to compare to
	 * @return the distance. 0 means equality, lower is better
	 * @throws Exception
	 */
	// http://en.wikipedia.org/wiki/Fr%C3%A9chet_distance
	// http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence
	public double distanceTo(Distribution other) {
		Set<Double> keys = new HashSet<Double>();
		keys.addAll(this.keySet());
		keys.addAll(other.keySet());

		// Measure the distance to that line
		double d = 0;
		for (Double key : keys)
			d += Math.abs(this.get(key) - other.get(key));

		return d;
	}

	/**
	 * @return
	 */
	public Set<Entry<Double, Double>> entrySet() {
		return data.entrySet();
	}

	/**
	 * @param key
	 * @return
	 */
	public double get(Double key) {
		return (data.containsKey(key) ? data.get(key) : 0);
	}

	/**
	 * @return
	 */
	public double getAverage() {
		// Compute the average value
		double numberElements = 0;
		double total = 0;
		for (Entry<Double, Double> entry : data.entrySet()) {
			total += entry.getKey() * entry.getValue(); // value * occurences
			numberElements += entry.getValue();
		}
		if (numberElements > 0)
			total /= numberElements;
		return total;
	}

	/**
	 * @param axis
	 * @return
	 */
	private double[] getValues(DistributionAxis axis) {
		Collection<Double> values = (axis.equals(DistributionAxis.X) ? data.keySet() : data.values());
		double[] numbers = new double[values.size()];
		int i = 0;
		for (Double v : values)
			numbers[i++] = v;
		return numbers;
	}

	/**
	 * @param x
	 * @param state
	 */
	public void increaseCounter(double x) {
		Double key = Double.valueOf(df.format(x));
		Double counter = (data.containsKey(key) ? data.get(key) + 1 : Double.valueOf(1));
		data.put(key, counter);
	}

	/**
	 * @return
	 */
	public Set<Double> keySet() {
		return data.keySet();
	}

	/**
	 * @param axis
	 * @return
	 */
	public Double max(DistributionAxis axis) {
		TreeSet<Double> values = new TreeSet<Double>(axis.equals(DistributionAxis.X) ? data.keySet() : data.values());
		return values.last();
	}

	/**
	 * @param axis
	 * @return
	 */
	public double mean(DistributionAxis axis) {
		Mean mean = new Mean();
		return mean.evaluate(getValues(axis));
	}

	/**
	 * @param axis
	 * @return
	 */
	public Double min(DistributionAxis axis) {
		TreeSet<Double> values = new TreeSet<Double>(axis.equals(DistributionAxis.X) ? data.keySet() : data.values());
		return values.first();
	}

	/**
	 * Divide all the Y values by their sum to get probabilities
	 */
	public void normalize() {
		// Count
		Double total = 0.0;
		for (Double v : data.values())
			total += v;

		// Rescale
		if (total != 0)
			for (Entry<Double, Double> entry : data.entrySet())
				data.put(entry.getKey(), entry.getValue() / total);
	}

	/**
	 * @param key
	 * @param i
	 */
	public void set(double x, double value) {
		Double key = Double.valueOf(df.format(x));
		data.put(key, value);
	}

	/**
	 * @return
	 */
	public int size() {
		return data.size();
	}

	/**
	 * @param axis
	 * @return
	 */
	public double standardDeviation(DistributionAxis axis) {
		StandardDeviation sd = new StandardDeviation();
		return sd.evaluate(getValues(axis));
	}

	/**
	 * Write the distribution to a file
	 * 
	 * @throws IOException
	 */
	public void writeToFile(String fileName) throws IOException {
		Set<Double> keys = new TreeSet<Double>();
		keys.addAll(data.keySet());

		Writer file = new FileWriter(fileName);
		for (Double key : keys) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(df.format(key)).append(" ").append(df.format(data.get(key)));
			file.write(buffer.append("\n").toString());
		}
		file.close();
	}

}
