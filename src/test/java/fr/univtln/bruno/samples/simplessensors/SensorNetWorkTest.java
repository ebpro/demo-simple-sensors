package fr.univtln.bruno.samples.simplessensors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.junit.jupiter.api.*;

import fr.univtln.bruno.samples.simplesensors.sensors.Sensor;
import fr.univtln.bruno.samples.simplesensors.sensors.SensorNetwork;

@DisplayName("Sensor Network Unit Tests")
class SensorNetworkTest {

    private SensorNetwork network;
    private static final int SENSOR_COUNT = 100;
    private static final int TIMEOUT_SECONDS = 5;

    @BeforeEach
    void setUp() {
        network = SensorNetwork.of("TestNetwork");
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentTests {
        
        @Test
        @DisplayName("Should handle concurrent sensor additions")
        void testConcurrentSensorAddition() throws Exception {
            var addedSensors = new ConcurrentLinkedQueue<Sensor<?>>();

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = new ArrayList<Future<?>>();

                // Add sensors concurrently
                for (int i = 0; i < SENSOR_COUNT; i++) {
                    final int id = i;
                    futures.add(executor.submit(() -> {
                        Sensor<?> sensor = (id % 2 == 0) 
                            ? network.addTemperatureSensor("Temp" + id)
                            : network.addPressureSensor("Press" + id);
                        addedSensors.add(sensor);
                    }));
                }

                // Wait for completion
                for (Future<?> future : futures) {
                    future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
            }

            // Verify results
            assertAll(
                () -> assertEquals(SENSOR_COUNT, addedSensors.size()),
                () -> assertEquals(SENSOR_COUNT, network.sensorCount()),
                () -> assertEquals(SENSOR_COUNT/2, 
                    network.sensorByQuantityTypeStream(Temperature.class).count()),
                () -> assertEquals(SENSOR_COUNT/2, 
                    network.sensorByQuantityTypeStream(Pressure.class).count())
            );
        }
    }
}