package fr.univtln.bruno.samples.simplesensors;

import fr.univtln.bruno.samples.simplesensors.data.Function;
import fr.univtln.bruno.samples.simplesensors.data.FunctionBasedMeasureGenerator;
import fr.univtln.bruno.samples.simplesensors.sensors.Sensor;
import fr.univtln.bruno.samples.simplesensors.sensors.SensorNetwork;
import lombok.extern.java.Log;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import java.time.Duration;
import java.time.Instant;

/**
 * Main application class for the Simple Sensors example.
 */
@Log
public class App {

  /**
   * Main method to run the application.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) {

    // Create a sensor network
    SensorNetwork sensorNetwork1 = SensorNetwork.of("Sensor network 1");

    // Add temperature and pressure sensors to the network
    Sensor<Temperature> sensor1 = sensorNetwork1
        .addTemperatureSensor("T_1");
    Sensor<Pressure> sensor2 = sensorNetwork1
        .addPressureSensor("P_1");

    // Add measurements to the sensors
    sensor1.addMeasurement(Instant.now(), Quantities.getQuantity(25, Units.CELSIUS))
        .addMeasurement(Instant.now().plusSeconds(15), Quantities.getQuantity(300.15, Units.KELVIN));
    sensor2.addMeasurement(Instant.now(), Quantities.getQuantity(1013.25, Units.PASCAL));

    // Create a function-based measure generator for temperature
    // Using an anonymous class for the measurement function
    // The simulation starts now and ends in 10 seconds
    // Measurements are generated every second
    // Variance is 2, outlier probability is 0.1
    FunctionBasedMeasureGenerator<Temperature> temperatureGenerator = FunctionBasedMeasureGenerator
        .<Temperature>builder()
        .measurementFunction(
            new Function() {
              @Override
              public double apply(long currentDuration) {
                // Simple linear function
                return 20.0 + 2.0 * currentDuration;
              }
            })
        .startTime(Instant.now())
        .endTime(Instant.now().plusSeconds(10))
        .timeStep(Duration.of(1, java.time.temporal.ChronoUnit.SECONDS))
        .variance(2)
        .outlierProbability(0.1)
        .build();

    // Generate measurements for the temperature sensor using the generator
    temperatureGenerator.generate(sensor1, Units.CELSIUS);

    // Create a function-based measure generator for pressure
    // Using lambda expression for the measurement function
    // The simulation starts now and ends in 10 seconds
    // Measurements are generated every second
    FunctionBasedMeasureGenerator<Pressure> pressureGenerator = FunctionBasedMeasureGenerator.<Pressure>builder()
        .measurementFunction(time -> {
          double p0 = 1000.0; // Base pressure
          double a = 100.0; // Amplitude
          double omega = 0.1; // Angular frequency
          double phi = 0.0; // Phase shift
          return p0 + a * Math.sin(omega * time + phi);
        })
        .startTime(Instant.now())
        .endTime(Instant.now().plusSeconds(10))
        .timeStep(Duration.ofSeconds(1))
        .variance(0.1)
        .outlierProbability(0.1)
        .build();

    // Generate measurements for the pressure sensor using the generator
    pressureGenerator.generate(sensor2, Units.PASCAL);

    // Add a temperature sensor T_2 and a pressure sensor P_2 to the network
    // Note: the sensors are added to the network without assigning them to
    // variables
    sensorNetwork1.addTemperatureSensor("T_2");
    sensorNetwork1.addPressureSensor("P_2");

    // find Temperature sensor T_2
    Sensor<Temperature> sensor_t2 = sensorNetwork1.sensorByQuantityTypeAndName(Temperature.class, "T_2");
    // Generate measurements for the temperature sensor T_2
    FunctionBasedMeasureGenerator.<Temperature>builder()
        .measurementFunction(time -> 20.0 + 2.0 * time)
        .startTime(Instant.now().minus(Duration.ofHours(1)))
        .endTime(Instant.now().plusSeconds(10))
        .timeStep(Duration.ofMinutes(1))
        .variance(1)
        .outlierProbability(0.1)
        .build()
        .generate(sensor_t2, Units.CELSIUS);

    // find Temperature sensor P_2
    Sensor<Pressure> sensor_p2 = sensorNetwork1.sensorByQuantityTypeAndName(Pressure.class, "P_2");
    // Generate measurements for the pressure sensor P_2
    FunctionBasedMeasureGenerator.<Pressure>builder()
        .measurementFunction(time -> 1000.0 + 100.0 * Math.sin(0.1 * time))
        .startTime(Instant.now().minus(Duration.ofHours(1)))
        .endTime(Instant.now().plusSeconds(10))
        .timeStep(Duration.ofMinutes(1))
        .variance(1)
        .outlierProbability(0.1)
        .build()
        .generate(sensor_p2, Units.PASCAL);


    // Log the sensor network, sensors and measures
    log.info(() -> """

    === Sensor network, sensors and measures ===
        %s
        """.formatted(sensorNetwork1));

    // Log the minimum and maximum temperature and pressure in the network  
    log.info(() -> """

    === Minimum and Maximum Temperature and Pressure in the network ===
          Temperature: min %s -- max: %s
          Pressure   : min %s -- max: %s
          """.formatted(
        sensorNetwork1.findMinMeasurement(Temperature.class).orElseThrow().getValue(),
        sensorNetwork1.findMaxMeasurement(Temperature.class).orElseThrow().getValue(),
        sensorNetwork1.findMinMeasurement(Pressure.class).orElseThrow().getValue(),
        sensorNetwork1.findMaxMeasurement(Pressure.class).orElseThrow().getValue()));

    // Log the statistics for the network
    log.info(() -> """

        === Sensors and statistics for the network ===
        %s
        """.formatted(sensorNetwork1.getStatistics()));

  }

}
