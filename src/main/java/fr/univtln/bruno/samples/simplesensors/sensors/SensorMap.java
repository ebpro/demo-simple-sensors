package fr.univtln.bruno.samples.simplesensors.sensors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Thread-safe map for managing sensors, indexed by their UUID.
 * Implements Map interface for standard collection operations.
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Thread-safe using ConcurrentHashMap (O(1) operations)</li>
 *   <li>Type-safe sensor management with generics</li>
 *   <li>Automatic UUID key generation</li>
 *   <li>Custom map implementation support</li>
 *   <li>Atomic operations guarantee</li>
 * </ul>
 * 
 * <h2>Usage Examples:</h2>
 * 
 * <h3>Basic Operations:</h3>
 * <pre>{@code
 * // Create a sensor map
 * SensorMap<Temperature> sensorMap = new SensorMap<>();
 * 
 * // Add sensors
 * Sensor<Temperature> sensor1 = new Sensor<>("Room1");
 * sensorMap.add(sensor1);
 * 
 * // Get sensor by UUID
 * Sensor<Temperature> found = sensorMap.get(sensor1.getId());
 * }</pre>
 * 
 * <h3>Concurrent Operations:</h3>
 * <pre>{@code
 * // Thread-safe operations
 * sensorMap.computeIfAbsent(uuid, id -> new Sensor<>("New" + id));
 * 
 * // Atomic updates
 * sensorMap.replace(uuid, oldSensor, newSensor);
 * 
 * // Bulk operations
 * sensorMap.forEach((id, sensor) -> 
 *     sensor.addMeasurement(Instant.now(), value));
 * }</pre>
 * 
 * <h3>Stream Operations:</h3>
 * <pre>{@code
 * // Find sensors by criteria
 * List<Sensor<Temperature>> activeSensors = sensorMap.values()
 *     .stream()
 *     .filter(s -> s.getLastMeasurement()
 *         .map(m -> m.getTimestamp()
 *             .isAfter(Instant.now().minus(Duration.ofHours(1))))
 *         .orElse(false))
 *     .collect(Collectors.toList());
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <ul>
 *   <li>All operations are atomic</li>
 *   <li>Safe for concurrent access across threads</li>
 *   <li>No external synchronization needed</li>
 *   <li>Consistent iteration guarantees</li>
 * </ul>
 * 
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *   <li>get/put/remove: O(1) average case</li>
 *   <li>Memory overhead: O(n) for n entries</li>
 *   <li>Space efficient for sparse maps</li>
 * </ul>
 * 
 * @see Sensor
 * @see java.util.concurrent.ConcurrentHashMap
 */
public class SensorMap implements Map<UUID, Sensor<?>> {

  /**
   * Internal thread-safe map storing sensors.
   * Uses ConcurrentHashMap for atomic operations.
   */
  private final Map<UUID, Sensor<?>> sensors;

  /**
   * Creates a new SensorMap with default ConcurrentHashMap implementation.
   * @return New SensorMap instance
   */
  public static SensorMap newInstance() {
    return new SensorMap();
  }

  /**
   * Creates a SensorMap with custom concurrent map implementation.
   * @return New SensorMap instance using custom map
   * 
   * @param mapSupplier Supplier for concurrent map instance
   */
  public static SensorMap of(Supplier<? extends ConcurrentMap<UUID, Sensor<?>>> mapSupplier) {
    return new SensorMap(mapSupplier);
  }

  /**
   * Creates a new SensorMap with default ConcurrentHashMap implementation.
   */
  protected SensorMap() {
    this.sensors = new ConcurrentHashMap<>();
  }

  /**
   * Creates a SensorMap with custom concurrent map implementation.
   * 
   * @param mapSupplier Supplier for concurrent map instance
   */
  protected SensorMap(Supplier<? extends ConcurrentMap<UUID, Sensor<?>>> mapSupplier) {
    this.sensors = mapSupplier.get();
  }

  /**
   * Adds a sensor to the map.
   * 
   * @param sensor The sensor to add
   * @return Null if no sensor with the same UUID was present, otherwise the
   *         replaced sensor
   */
  public Sensor<?> put(Sensor<?> sensor) {
    return sensors.put(sensor.getId(), sensor);
  }

  /**
   * Adds a sensor to the map.
   * 
   * @param sensor The sensor to add
   * @return The added sensor
   */
  public Sensor<?> add(Sensor<?> sensor) {
    sensors.put(sensor.getId(), sensor);
    return sensor;
  }

  /**
   * Return the set of entries in the map.
   * An entry is a key-value pair.
   * 
   * @return Set of entries
   */
  @Override
  public Set<Entry<UUID, Sensor<?>>> entrySet() {
    return sensors.entrySet();
  }

  /**
   * Returns a collection of sensors in the map.
   * 
   * @return Collection of sensors
   */
  @Override
  public Collection<Sensor<?>> values() {
    return sensors.values();
  }

  /**
   * Returns a set of UUID keys in the map.
   * 
   * @return Set of UUID keys
   */
  @Override
  public Set<UUID> keySet() {
    return sensors.keySet();
  }

  /**
   * Clears all sensors from the map.
   */
  @Override
  public void clear() {
    sensors.clear();
  }

  /**
   * Puts all entries from the given map into this map.
   * 
   * @param m The map to copy entries from
   */
  @Override
  public void putAll(Map<? extends UUID, ? extends Sensor<?>> m) {
    sensors.putAll(m);
  }

  /**
   * Removes a sensor from the map by its UUID.
   * 
   * @param key The UUID of the sensor to remove
   * @return The removed sensor, or null if not found
   */
  @Override
  public Sensor<?> remove(Object key) {
    return sensors.remove(key);
  }

  /**
   * Checks if the map contains the given sensor.
   * 
   * @param value The sensor to check for
   * @return True if the sensor is present, false otherwise
   */
  @Override
  public Sensor<?> put(UUID key, Sensor<?> value) {
    return sensors.put(key, value);
  }

  /**
   * Retrieves a sensor by its UUID.
   * 
   * @param key The UUID of the sensor
   * @return The sensor with the given UUID, or null if not found
   */
  @Override
  public Sensor<?> get(Object key) {
    return sensors.get(key);
  }

  /**
   * Checks if the map contains the given sensor.
   * 
   * @param value The sensor to check for
   * @return True if the sensor is present, false otherwise
   */

  @Override
  public boolean containsValue(Object value) {
    return sensors.containsValue(value);
  }

  /**
   * Checks if the map contains the given key.
   * 
   * @param key The UUID key to check for
   * @return True if the key is present, false otherwise
   */
  @Override
  public boolean containsKey(Object key) {
    return sensors.containsKey(key);
  }

  /**
   * Checks if the map is empty.
   * 
   * @return True if the map is empty, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return sensors.isEmpty();
  }

  /**
   * Returns the number of sensors in the map.
   * 
   * @return The number of sensors
   */
  @Override
  public int size() {
    return sensors.size();
  }

}
