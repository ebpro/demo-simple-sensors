package fr.univtln.bruno.samples.simplessensors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import fr.univtln.bruno.samples.simplesensors.data.FunctionBasedMeasureGenerator;
import fr.univtln.bruno.samples.simplesensors.sensors.Sensor;
import fr.univtln.bruno.samples.simplesensors.sensors.SensorNetwork;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.time.Instant;
import javax.measure.quantity.*;

@DisplayName("Sensor Network Integration Tests")
class SensorNetworkIT {
    
    @Nested
    @DisplayName("Network Operations")
    class NetworkOperationsTests {
        @Test
        @DisplayName("Should perform full sensor network lifecycle")
        void testFullSensorNetworkOperation() {
            // Setup
            SensorNetwork network = SensorNetwork.of("TestNetwork");
            Sensor<Temperature> tempSensor = network.addTemperatureSensor("Temp1");
            Sensor<Pressure> pressSensor = network.addPressureSensor("Press1");

            // Add measurements
            var tempValue = Quantities.getQuantity(25.0, Units.CELSIUS);
            var pressValue = Quantities.getQuantity(1013.25, Units.PASCAL);
            
            tempSensor.addMeasurement(Instant.now(), tempValue);
            pressSensor.addMeasurement(Instant.now(), pressValue);

            // Assert
            assertAll(
                () -> assertEquals(2, network.sensorCount()),
                () -> assertTrue(network.findMaxMeasurement(Temperature.class).isPresent()),
                () -> assertEquals(
                    tempValue.toSystemUnit().getValue().doubleValue(),
                    network.findMaxMeasurement(Temperature.class)
                           .orElseThrow()
                           .getValue()
                           .getValue()
                           .doubleValue(),
                    0.01)
            );
        }
    }

    @Nested
    @DisplayName("Measurement Generation")
    class MeasurementGenerationTests {
        @Test
        @DisplayName("Should generate measurements using function")
        void testMeasurementGeneration() {
            // Setup
            SensorNetwork network = SensorNetwork.of("GeneratorTest");
            Sensor<Temperature> sensor = network.addTemperatureSensor("TempGen");

            // Configure generator
            FunctionBasedMeasureGenerator<Temperature> generator = 
                FunctionBasedMeasureGenerator.<Temperature>builder()
                    .measurementFunction(t -> 20.0 + Math.sin(t))
                    .variance(2.0)
                    .outlierProbability(0.1)
                    .build();

            // Generate measurements
            generator.generate(sensor, Units.CELSIUS);

            // Assert
            assertAll(
                () -> assertTrue(sensor.getMeasurementCount() > 0),
                () -> assertNotNull(sensor.getStatistics()),
                () -> assertTrue(
                    sensor.measurementStream()
                          .mapToDouble(m -> m.getValue().getValue().doubleValue())
                          .average()
                          .orElse(0.0) > 0.0)
            );
        }
    }
}