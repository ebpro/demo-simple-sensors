/**
 * Simple Sensor Framework - A package for simulating and managing sensor networks.
 * 
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link fr.univtln.bruno.samples.simplesensors.sensors.Sensor} - Base sensor class</li>
 *   <li>{@link fr.univtln.bruno.samples.simplesensors.sensors.SensorNetwork} - Network management</li>
 *   <li>{@link fr.univtln.bruno.samples.simplesensors.data.Function} - Measurement generation functions</li>
 * </ul>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Create a sensor network
 * SensorNetwork network = SensorNetwork.of("Lab1");
 * 
 * // Add temperature sensor
 * Sensor<Temperature> tempSensor = network.addTemperatureSensor("T1");
 * 
 * // Generate measurements
 * FunctionBasedMeasureGenerator<Temperature> generator = 
 *     FunctionBasedMeasureGenerator.<Temperature>builder()
 *         .function(t -> 20 + Math.sin(t/3600.0))
 *         .build();
 * }</pre>
 * 
 * <h2>Dependencies:</h2>
 * <ul>
 *   <li>JSR-385 Units of Measurement API</li>
 *   <li>Lombok for boilerplate reduction</li>
 *   <li>SLF4J for logging</li>
 * </ul>
 * 
 * @author Emmanuel Bruno
 * @version 1.0
 * @see fr.univtln.bruno.samples.simplesensors.sensors.Sensor
 * @see fr.univtln.bruno.samples.simplesensors.sensors.SensorNetwork
 */
package fr.univtln.bruno.samples.simplesensors;