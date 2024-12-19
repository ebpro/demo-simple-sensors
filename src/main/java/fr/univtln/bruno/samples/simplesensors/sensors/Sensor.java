package fr.univtln.bruno.samples.simplesensors.sensors;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.Unit;

import fr.univtln.bruno.samples.simplesensors.statistics.MeasurementsUtils;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * Represents a sensor that can take measurements of a specific physical
 * quantity.
 *
 * @param <Q> The type of physical quantity this sensor measures
 */
@Log
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class Sensor<Q extends Quantity<Q>> {

  /**
   * Error message when no measurements are found
   */
  private static final String NO_MEASUREMENTS_FOUND = "No measurements found";

  /**
   * Unique identifier for the sensor, automatically generated
   */
  @ToString.Include
  private final UUID id = UUID.randomUUID();

  /**
   * The class of the quantity measured by the sensor
   */
  private final Class<Q> quantityClass;

  /**
   * Human-readable name of the sensor
   */
  @ToString.Include
  private final String name;

  /**
   * Sorted set of measurements, ordered by timestamp.
   * Using TreeSet ensures:
   * 1. No duplicate measurements at the same timestamp
   * 2. Measurements are always in chronological order
   */
  private final SortedSet<Measurement<Q>> measurements = new ConcurrentSkipListSet<>();

  /**
   * The unit of the quantity measured by the sensor
   */
  private Unit<Q> unit;

  /**
   * Creates a new sensor with a name dedicated to the given quantity class.
   *
   * @param quantityClass The class of the quantity measured by the sensor
   * @param name          The name of the sensor
   */
  public Sensor(Class<Q> quantityClass, String name) {
    this.quantityClass = quantityClass;
    this.name = name;
  }

  /**
   * Returns the unit of the quantity measured by the sensor.
   * If the unit is not set, it is inferred from the first measurement.
   *
   * @return The unit of the quantity measured by the sensor
   */
  public Unit<Q> getUnit() {
    if (unit == null) {
      Optional<Measurement<Q>> firstMeasurement = measurements.stream().findFirst();
      firstMeasurement.ifPresent(measurement -> unit = measurement.getValue().getUnit());
    }
    return unit;
  }

  /**
   * The time of the last change to the sensor (creation or last measurement
   * added).
   * Used to invalidate cached statistics and other derived data.
   */
  private volatile LocalDateTime lastChange = LocalDateTime.now();

  /**
   * Adds a measurement to the sensor.
   *
   * @param timestamp The time at which the measurement was taken
   * @param value     The value of the measurement
   * @return The sensor instance (for method chaining)
   */
  public Sensor<Q> addMeasurement(Instant timestamp, Quantity<Q> value) {
    if (timestamp == null || value == null) {
      throw new IllegalArgumentException("Timestamp and value must not be null");
    }
    // Convert the value to its system unit
    Quantity<Q> metricValue = value.toSystemUnit(); // Convert the value to its system unit
    Measurement<Q> measurement = Measurement.of(this, timestamp, metricValue); // Create a new measurement

    if (measurements.add(measurement)) { // Atomic operation
      notifyObservers(measurement); // Thread-safe notification
      lastChange = LocalDateTime.now(); // Update last change time
      invalidateCache(); // Invalidate cached statistics
    }

    return this;
  }

  /**
   * Invalidates the cache for statistical computations.
   */
  private synchronized void invalidateCache() {
    cacheAverageValue = null;
    cacheMinValue = null;
    cacheMaxValue = null;
    cacheVarianceValue = null;
    cacheStandardDeviationValue = null;
    cacheMedianValue = null;
}

  /**
   * Adds a measurement to the sensor.
   *
   * @param instant   The time at which the measurement was taken
   * @param quantity  The value of the measurement
   * @param isOutlier True if the measurement is an outlier (trust only if true,
   *                  false otherwise). Setted by a generator.
   */
  public void addMeasurement(Instant instant, Quantity<Q> quantity, boolean isOutlier) {
    if (isOutlier) {
      generatedOutliers.incrementAndGet();
    }
    addMeasurement(instant, quantity);
  }

  /**
   * Returns an iterator over the measurements.
   *
   * @return An iterator over the measurements
   */
  public Iterator<Measurement<Q>> measurementIterator() {
    return measurements.iterator();
  }

  /**
   * Returns a stream of the measurements.
   *
   * @return A stream of the measurements
   */
  public Stream<Measurement<Q>> measurementStream() {
    return measurements.stream();
  }

  /**
   * Returns a stream of the measurement values.
   *
   * @return A stream of the measurement values
   */
  public DoubleStream measurementValueStream() {
    return measurementStream()
        .map(Measurement::getValue)
        .map(Quantity::getValue)
        .mapToDouble(Number::doubleValue);
  }

  /*
   * Cache for statistical computations.
   * All fields are volatile to ensure thread safety and visibility across
   * threads.
   */

  /** Cache for computed average value */
  private Quantity<Q> cacheAverageValue;
  /** Cache for minimum value */
  private Quantity<Q> cacheMinValue;
  /** Cache for maximum value */
  private Quantity<Q> cacheMaxValue;
  /** Cache for variance */
  private Quantity<?> cacheVarianceValue;
  /** Cache for standard deviation */
  private Quantity<Q> cacheStandardDeviationValue;
  /** Cache for median value */
  private Quantity<Q> cacheMedianValue;

  /**
   * Manages cached computation of sensor values using the cache-aside pattern.
   * This method implements a caching strategy to avoid expensive recomputations.
   *
   * @param cacheValue      Previously cached value (can be null)
   * @param computeValue    Supplier function that computes the new value if cache
   *                        invalid
   * @return Either cached value if valid, or newly computed value
   * @throws IllegalStateException if sensor has no measurements
   **/
  public Quantity<Q> useCacheOrCompute(Quantity<Q> cacheValue,
      Supplier<Quantity<Q>> computeValue) {

    if (cacheValue != null) {
      return cacheValue;
    }  else {
      return computeValue.get();
    }
  }

  /**
   * Returns the number of measurements taken by the sensor.
   *
   * @return The number of measurements taken by the sensor
   */
  public int getMeasurementCount() {
    return measurements.size();
  }

  /**
   * The number of generated outliers.
   */
  @Getter
  private final AtomicInteger generatedOutliers = new AtomicInteger(0);

  /**
   * Map of functions to compute metadata for the sensor.
   *
   */
  private final Map<String, Supplier<Number>> metadatasMap = Map.of(
      "generated outliers", this::getGeneratedOutliers,
      "measurement count", this::getMeasurementCount);

  /**
   * Map of functions to compute statistics for the sensor.
   *
   */
  private final Map<String, Supplier<Quantity<?>>> statisticsMap = Map.of(
      "average", this::getAverageMeasurement,
      "min", this::getMinMeasurement,
      "max", this::getMaxMeasurement,
      "variance", this::getVariance,
      "standard deviation", this::getStandardDeviationMeasurement,
      "median", this::getMedianMeasurementValue);

  /**
   * Formats for pretty-printing sensor statistics header.
   */
  private static final String STATS_HEADER_FORMAT = "= %s(%s) =";

  /**
   * Formats for pretty-printing sensor statistics line.
   */
  private static final String STATS_LINE_FORMAT = "\t %s: %.2f %s";

  /**
   * Line separator for pretty-printing sensor statistics.
   */
  private static final String LINE_SEP = System.lineSeparator();

  /**
   * Pretty-prints sensor statistics with measurements and units.
   *
   * @return A formatted string with sensor statistics
   */
  public String getStatistics() {

    String metadatas = metadatasMap.entrySet().stream()
        .map(entry -> String.format("\t %s: %s",
            entry.getKey(),
            entry.getValue().get().doubleValue()))
        .collect(Collectors.joining(LINE_SEP));

    String statistics = statisticsMap.entrySet().stream()
        .map(entry -> String.format(STATS_LINE_FORMAT,
            entry.getKey(),
            entry.getValue().get().getValue().doubleValue(),
            entry.getValue().get().getUnit()))
        .collect(Collectors.joining(
            LINE_SEP));

    return String.format(STATS_HEADER_FORMAT, getName(), getId()) + LINE_SEP + metadatas + LINE_SEP + statistics;
  }

  /** MAKE SENSORS OBSERVABLE */

  /**
   * Observer pattern for receiving measurements from sensors.
   * The interface is functional, so it can be implemented as a lambda.
   * callback function to be called when a new measurement is received.
   *
   * @param <Q> The type of quantity measured by the sensor
   */
  @FunctionalInterface
  public interface MeasurementObserver<Q extends Quantity<Q>> {

    /**
     * Called when a new measurement is received.
     *
     * @param measurement The new measurement
     */
    void onMeasurement(Measurement<Q> measurement);
  }

  /**
   * Set of observers for the sensor.
   */
  private final Set<MeasurementObserver<Q>> observers = ConcurrentHashMap.newKeySet();

  /**
   * Subscribes an observer to receive measurements from the sensor.
   *
   * @param observer The observer to subscribe
   */
  public void subscribe(MeasurementObserver<Q> observer) {
    observers.add(observer);
  }

  /**
   * Unsubscribes an observer from receiving measurements from the sensor.
   *
   * @param observer The observer to unsubscribe
   */
  public void unsubscribe(MeasurementObserver<Q> observer) {
    observers.remove(observer);
  }

  /**
   * Notifies all observers of a new measurement.
   *
   * @param measurement The new measurement
   */
  public void notifyObservers(Measurement<Q> measurement) {
    observers.forEach(observer -> observer.onMeasurement(measurement));
  }

  /****** STATISTICS */

  private MeasurementsUtils<Q> measurementsUtils = new MeasurementsUtils<>();

  /**
   * Returns the average measurement value.
   *
   * @return The average measurement value
   */
  public Quantity<Q> getAverageMeasurement() {
    return useCacheOrCompute(cacheAverageValue,
        () -> measurementsUtils.getAverageMeasurementQuantity(measurementValueStream(), getUnit()));
  }

  /**
   * Returns the minimum measurement value.
   *
   * @return The minimum measurement value
   */
  public Quantity<Q> getMinMeasurement() {
    return useCacheOrCompute(cacheMinValue,
        () -> measurementsUtils.getMinMeasurementValue(measurementValueStream(), getUnit()));
  }

  /**
   * Returns the maximum measurement value.
   *
   * @return The maximum measurement value
   */
  public Quantity<Q> getMaxMeasurement() {
    return useCacheOrCompute(cacheMinValue,
        () -> measurementsUtils.getMaxMeasurementValue(measurementValueStream(), getUnit()));
  }

  /**
   * Returns the variance of the measurement values.
   *
   * @return The variance of the measurement values
   */
  public Quantity<?> getVariance() {
    return measurementsUtils.getVariance(getAverageMeasurement(), measurementValueStream(), getUnit());
  }

  /**
   * Returns the standard deviation of the measurement values.
   *
   * @return The standard deviation of the measurement values
   */
  public Quantity<Q> getStandardDeviationMeasurement() {
    return useCacheOrCompute(cacheStandardDeviationValue,
        () -> measurementsUtils.getStandardDeviation(getVariance(), getUnit()));
  }

  /**
   * Calculates the median measurement value.
   * Takes advantage of TreeSet being already sorted.
   *
   * @return The median value
   * @throws IllegalStateException if no measurements exist
   */
  public Quantity<Q> getMedianMeasurementValue() {
    return useCacheOrCompute(
        cacheMedianValue,
        this::computeMedian);
  }

  /**
   * Computes the median value of the measurements.
   *
   * @return The median value
   * @throws IllegalStateException if no measurements exist
   */
  private Quantity<Q> computeMedian() {
    if (measurements.isEmpty()) {
      throw new IllegalStateException(NO_MEASUREMENTS_FOUND);
    }

    List<Measurement<Q>> sortedMeasurements = new ArrayList<>(measurements);
    int size = sortedMeasurements.size();
    double median;

    if (size % 2 == 0) {
      // Even number of measurements
      Measurement<Q> m1 = sortedMeasurements.get((size / 2) - 1);
      Measurement<Q> m2 = sortedMeasurements.get(size / 2);
      median = (m1.getValue().getValue().doubleValue() +
          m2.getValue().getValue().doubleValue()) / 2.0;
    } else {
      // Odd number of measurements
      median = sortedMeasurements.get(size / 2)
          .getValue()
          .getValue()
          .doubleValue();
    }

    return Quantities.getQuantity(median, getUnit());
  }

  /**
   * Returns the timestamp of the first measurement taken by the sensor.
   * @return The timestamp of the first measurement taken by the sensor
   */
  public Object getLastMeasurement() {
    return measurements.getFirst();
  }

}
