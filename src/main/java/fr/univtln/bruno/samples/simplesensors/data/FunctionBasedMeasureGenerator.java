package fr.univtln.bruno.samples.simplesensors.data;

import fr.univtln.bruno.samples.simplesensors.sensors.Sensor;
import lombok.Builder;
import lombok.extern.java.Log;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.Unit;
import java.time.Duration;
import java.time.Instant;

/**
 * Generates measurements for a sensor based on a mathematical function.
 * The function is applied to the time elapsed since the start time.
 * Random variations are added to the measurements to simulate real-world data.
 * Outliers can be generated with a specified probability.
 * 
 * @param <Q> The type of quantity measured by the sensor
 * 
 */
@Builder
@Log
public class FunctionBasedMeasureGenerator<Q extends Quantity<Q>> {

  /**
   * The measurement function to generate measurements based on time.
   */
  @Builder.Default
  //private final Function<Long, Double> measurementFunction = x -> x;
  private final fr.univtln.bruno.samples.simplesensors.data.Function measurementFunction = x -> x;
  
  // Start time for generating measurements
  /**
   * The start time for generating measurements.
   */
  @Builder.Default
  private final Instant startTime = Instant.now();
  
  /**
    * The end time for generating measurements.
    */
  @Builder.Default
  private final Instant endTime = Instant.now().plus(Duration.ofHours(1));
  
  /**
   * The time step between each measurement.
   */
  @Builder.Default
  private final Duration timeStep = Duration.ofMinutes(1);
  
  /**
   * The variance for random variation in measurements.
   */
  @Builder.Default
  private final double variance = 0 ;
  
  /** 
   * The probability of generating an outlier.
   */
  @Builder.Default
  private final double outlierProbability = 0;

  /**
   * The current duration from the start time.
   */
  @Builder.Default
  private Duration currentDuration = Duration.ZERO;

  /**
   * Generates measurements for the given sensor and unit.
   *
   * @param sensor The sensor to add measurements to
   * @param unit   The unit of the measurements
   */
  public void generate(Sensor<Q> sensor, Unit<Q> unit) {
    while (startTime.plus(currentDuration).isBefore(endTime)) {
      double result = generateMeasurement();
      boolean isOutlier = checkForOutlier();
      result += applyRandomVariation(isOutlier);
      sensor.addMeasurement(startTime.plus(currentDuration), Quantities.getQuantity(result, unit), isOutlier);
      currentDuration = currentDuration.plus(timeStep);
    }
  }

  /**
   * Generates a measurement based on the current duration.
   *
   * @return the generated measurement
   */
  private double generateMeasurement() {
    return measurementFunction.apply(currentDuration.getSeconds());
  }

  /**
   * Checks if an outlier should be generated based on the outlier probability.
   *
   * @return true if an outlier should be generated
   */
  private boolean checkForOutlier() {
    boolean isOutlier = false;
    if (Math.random() < outlierProbability) {
      isOutlier = true;
    }
    return isOutlier;
  }

  /**
   * Applies random variation to the measurement.
   *
   * @param isOutlier true if the variation should be an outlier
   * @return the measurement with random variation
   */
  private double applyRandomVariation(boolean isOutlier) {
    double randomVariation = (Math.random() * 2 - 1) * variance;
    if (isOutlier) {
      double outlierVariation = randomVariation * Math.random() * 20;
      log.finer(() -> "Outlier generated with variation: " + outlierVariation);
      randomVariation = outlierVariation;
    }
    return randomVariation;
  }
}
