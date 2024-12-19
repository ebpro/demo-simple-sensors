package fr.univtln.bruno.samples.simplesensors.sensors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.measure.Quantity;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a measurement taken by a sensor at a specific time.
 * 
 * @param <Q> the type of quantity measured by the sensor
 */
@Getter // Generates getters for all fields
// Creates static factory method 'of'
@RequiredArgsConstructor(staticName = "of") // Creates static factory method 'of'
public class Measurement<Q extends Quantity<Q>> implements Comparable<Measurement<Q>> {

  /** The sensor that produced this measurement */
  private final Sensor<Q> source;

  /** The timestamp when the measurement was taken (UTC) */
  private final Instant timestamp;

  /**
   * The measured value with its unit.
   * Not included in equals/hashCode as measurements with same timestamp
   * from same sensor are considered equal regardless of value.
   * 
   */
  private final Quantity<Q> value;

  /**
   * Returns a string representation of the measurement.
   */
  @Override
  public String toString() {
    return "Measurement(timestamp=%s, value=%.2f %s)".formatted(
        timestamp.toString(),
        value.getValue().doubleValue(),
        value.getUnit());
  }

  /**
   * Checks equality based on source sensor and timestamp only.
   * Note: value is not included in equality check as per domain model.
   *
   * @param o object to compare with
   * @return true if same sensor and timestamp
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Measurement<?> that))
      return false;
    return Objects.equals(source, that.source) &&
        Objects.equals(timestamp, that.timestamp);
  }

  /**
   * Compares measurements primarily by sensor ID, then by timestamp.
   * This ensures a consistent ordering for measurements from different sensors.
   *
   * @param measurement the measurement to compare with
   * @return negative if this is earlier, positive if later, 0 if same time
   */
  @Override
  public int compareTo(Measurement<Q> measurement) {
    // First compare by sensor ID for consistent ordering across sensors
    if (this.source == null || measurement.source == null) {
        throw new NullPointerException("Source sensor cannot be null");
    }
    int sourceComparison = this.source.getId().compareTo(measurement.source.getId());
    if (sourceComparison != 0) {
      return sourceComparison;
    }
    // Then compare by timestamp for reverse chronological ordering (newest first)
    return -this.timestamp.compareTo(measurement.timestamp);
  }

  /**
   * Generates hash code based on source sensor and timestamp only.
   * Must be consistent with equals() method.
   *
   * @return hash code value
   */
  @Override
  public int hashCode() {
    return Objects.hash(source, timestamp);
  }
}
