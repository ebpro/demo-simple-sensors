package fr.univtln.bruno.samples.simplesensors.sensors;

import lombok.*;
import lombok.extern.java.Log;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a sensor network that can contain multiple sensors of different
 * types.
 */
@Log
@ToString(onlyExplicitlyIncluded = true)
public class SensorNetwork {

  /**
   * The unique identifier for the sensor network.
   */
  @ToString.Include
  @Getter
  private final UUID id;

  /**
   * The name of the sensor network.
   */
  @Getter
  @Setter
  @ToString.Include
  @NonNull
  private String name;

  /**
   * Internal map of sensors in the network.
   */
  private final SensorMap sensors;

  /**
   * Creates a new sensor network with the given name and an empty sensor map.
   *
   * @param name The name of the sensor network
   * @return The created sensor network
   */
  public static SensorNetwork of(String name) {
    return new SensorNetwork(name, SensorMap.newInstance());
  }

  /**
   * Creates a new sensor network with the given name and an empty sensor map.
   * Internal use only.
   *
   * @param name        The name of the sensor network
   * @param mapSupplier Supplier for concurrent map instance that stores sensors
   * @return The created sensor network
   */
  public static SensorNetwork of(String name, Supplier<? extends ConcurrentMap<UUID, Sensor<?>>> mapSupplier) {
    return new SensorNetwork(name, SensorMap.of(mapSupplier));
  }

  /**
   * Creates a new sensor network with the given name and an empty sensor map.
   * Internal use only.
   *
   * @param sensorMap The sensor map to use
   * @param name The name of the sensor network
   */
  private SensorNetwork(String name, SensorMap sensorMap) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.sensors = sensorMap;
  }

  /**
   * Adds a sensor to the network.
   *
   * @param clazz      The quantity class of the sensor
   * @param sensorName The name of the sensor to add
   * @param <Q>        The quantity type of the sensor
   * @return The added sensor
   */
  public <Q extends Quantity<Q>> Sensor<Q> addSensor(Class<Q> clazz, String sensorName) {
    if (sensorName == null || sensorName.isBlank()) {
      throw new IllegalArgumentException("Sensor name cannot be null or empty");
    }
    Sensor<Q> sensor = new Sensor<>(clazz, sensorName);
    sensors.put(sensor);
    return sensor;
  }

  /**
   * Adds a temperature sensor to the network.
   *
   * @param sensorName The name of the temperature sensor
   * @return The added temperature sensor
   */
  public Sensor<Temperature> addTemperatureSensor(String sensorName) {
    return addSensor(Temperature.class, sensorName);
  }

  /**
   * Adds a pressure sensor to the network.
   *
   * @param sensorName The name of the pressure sensor
   * @return The added pressure sensor
   */
  public Sensor<Pressure> addPressureSensor(String sensorName) {
    return addSensor(Pressure.class, sensorName);
  }

  /**
   * Retrieves a sensor by its UUID.
   *
   * @param id The UUID of the sensor
   * @return The sensor with the given UUID, or null if not found
   */
  public Sensor<?> getSensor(UUID id) {
    return sensors.get(id);
  }

  /**
   * Removes a sensor from the network.
   *
   * @param sensor The sensor to remove
   */
  public void removeSensor(Sensor<?> sensor) {
    if (sensor != null) {
      sensors.remove(sensor.getId());
    }
  }

  /**
   * Removes a sensor from the network by its UUID.
   *
   * @param id The UUID of the sensor to remove
   */
  public void removeSensor(UUID id) {
    sensors.remove(id);
  }

  /**
   * Returns an iterator over the sensors in the network.
   *
   * @return An iterator over the sensors
   */
  public Iterator<? extends Sensor<?>> sensorIterator() {
    return sensors.values().iterator();
  }

  /**
   * Stream all sensors regardless of their quantity type
   *
   * @return a stream of sensors
   */
  public Stream<? extends Sensor<?>> sensorStream() {
    return sensors.values().stream();
  }

  /**
   * Stream all sensors regardless dependeing quantity type
   *
   * @param <Q>      the quantity type
   * @param quantity the quantity type to filter by
   * @return a stream of sensors filtered by quantity type
   */
  @SuppressWarnings("unchecked")
  public <Q extends Quantity<Q>> Stream<Sensor<Q>> sensorByQuantityTypeStream(Class<Q> quantity) {
    return sensors.values().stream()
        .filter(sensor -> quantity.isAssignableFrom(sensor.getQuantityClass()))
        .map(sensor -> (Sensor<Q>) sensor);
  }

  /** system line separator */
  public static final String LINE_SEPARATOR = System.lineSeparator();

  /**
   * Print the sensors and their measurements using iterators
   *
   * @param sensorNetwork the sensor network to print
   * @param out           the print stream to use
   */
  public static void printWithIterators(SensorNetwork sensorNetwork, PrintStream out) {
    out.println(sensorNetwork.getName() + "(" + sensorNetwork.getId() + ")");
    // Iterate over the sensors in the network
    Iterator<? extends Sensor<?>> sensorIterator = sensorNetwork.sensorIterator();
    while (sensorIterator.hasNext()) {
      Sensor<?> sensor = sensorIterator.next();
      out.println("\t" + sensor);

      // Iterate over the measurements of each sensor
      Iterator<? extends Measurement<?>> measurementIterator = sensor.measurementIterator();
      while (measurementIterator.hasNext()) {
        Measurement<?> measurement = measurementIterator.next();
        out.println("\t\t" + measurement);
      }
    }
  }

  /**
   * Print the sensors and their five last measurements using streams
   *
   * @param sensorNetwork the sensor network to print
   * @param out           the print stream to use
   */
  public static void printWithStreams(SensorNetwork sensorNetwork, PrintStream out) {
    out.println(sensorNetwork.prettyString());
  }

  /**
   * Print the sensors and their measurements using streams
   *
   * @return a string with the sensors and their measurements
   */
  public String prettyString() {
    String header = getName() + "(" + getId() + ")";
    // Print the sensors and their measurements
    String content = sensorStream()
        // .map(s -> "\t" + s.prettyString())
        .map(Sensor::toString)
        .collect(Collectors.joining(LINE_SEPARATOR));
    return header + LINE_SEPARATOR + content;
  }

  /**
   * Print the sensors and their statistics
   *
   * @return a string with the statistics
   */
  public String getStatistics() {
    return this.sensorStream()
        .collect(Collectors.groupingBy(Sensor::getQuantityClass)).entrySet().stream()
        .map(entry -> """
            == %s ==
            %s """.formatted(
            entry.getKey().getSimpleName(),
            entry.getValue().stream().map(Sensor::getStatistics).collect(Collectors.joining(LINE_SEPARATOR))))
        .collect(Collectors.joining(LINE_SEPARATOR));
  }

  /**
   * Stream measurements for given quantity type from any sensor in the network.
   *
   * @param quantityClass the quantity class to search for
   * @param <Q>           the quantity type
   * @return a stream of measurements
   */
  public <Q extends Quantity<Q>> Stream<Measurement<Q>> streamMeasurement(
      Class<Q> quantityClass) {
    return this.sensorByQuantityTypeStream(quantityClass)
        .flatMap(Sensor::measurementStream);
  }

  /**
   * Binary operator to compare two measurements by their value.
   */
  private static final Comparator<Measurement<?>> valueComparator = Comparator
      .comparingDouble(m -> m.getValue().getValue().doubleValue());

  /**
   * Find the minimum measurement for given quantity type in the network.
   *
   * @param <Q>           the quantity type
   * @param quantityClass the quantity class to search for
   * @return Optional containing minimum measurement if found
   */
  public <Q extends Quantity<Q>> Optional<Measurement<Q>> findMinMeasurement(
      Class<Q> quantityClass) {
    return streamMeasurement(quantityClass)
        .min(valueComparator);
  }

  /**
   * Find the maximum measurement for given quantity type in the network.
   *
   * @param <Q>           the quantity type
   * @param quantityClass the quantity class to search for
   * @return Optional containing maximum measurement if found
   */
  public <Q extends Quantity<Q>> Optional<Measurement<Q>> findMaxMeasurement(
      Class<Q> quantityClass) {
    return streamMeasurement(quantityClass)
        .max(valueComparator);
  }

  /**
   * Static initializer to load logging configuration.
   * If the logging configuration file is not found, a warning is logged.
   */
  static {
    Optional.of("logging.properties")
        .map(SensorNetwork.class.getClassLoader()::getResourceAsStream)
        .ifPresentOrElse(
            configFile -> {
              try {
                LogManager.getLogManager().readConfiguration(configFile);
                log.info("Logging configured successfully");
              } catch (IOException e) {
                log.warning("Failed to load logging config: " + e.getMessage());
              }
            },
            () -> log.warning("Logging configuration file not found"));
  }

  /**
   * Retrieves a sensor by its quantity type and name.
   * Searches through all sensors of the specified type and returns the one
   * matching the name.
   *
   * @param quantity The class of the quantity type (e.g., Temperature.class)
   * @param name     The unique name of the sensor to find
   * @param <Q>      The generic type of the quantity being measured
   * @return The matching sensor
   * @throws NoSuchElementException   if no sensor matches the criteria
   * @throws IllegalArgumentException if quantity or name is null
   */
  public <Q extends Quantity<Q>> Sensor<Q> sensorByQuantityTypeAndName(
      Class<Q> quantity,
      String name) {

    if (quantity == null || name == null) {
      throw new IllegalArgumentException(
          "Quantity class and name must not be null");
    }

    return sensorByQuantityTypeStream(quantity)
        .filter(s -> s.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException(
            "Sensor %s for %s not found".formatted(
                name,
                quantity.getSimpleName())));
  }

  /**
   * Returns the number of sensors in the network.
   *
   * @return The number of sensors
   */
  public Integer sensorCount() {
    return sensors.size();
  }

}
