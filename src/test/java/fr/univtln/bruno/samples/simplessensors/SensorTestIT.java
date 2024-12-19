package fr.univtln.bruno.samples.simplessensors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import fr.univtln.bruno.samples.simplesensors.sensors.Measurement;
import fr.univtln.bruno.samples.simplesensors.sensors.Sensor;
import fr.univtln.bruno.samples.simplesensors.sensors.SensorNetwork;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import javax.measure.quantity.Temperature;

/**
 * Demonstrates concurrent testing patterns using virtual threads (Java 21+).
 * Tests thread safety of Sensor class operations.
 */
@Nested
@DisplayName("Concurrent Operations")
class ConcurrentTests {
    // Number of concurrent measurements to simulate
    private static final int MEASUREMENT_COUNT = 100;
    // Maximum time to wait for all operations to complete
    private static final int TIMEOUT_SECONDS = 10;
    // Starting temperature for measurements
    private static final double BASE_TEMPERATURE = 20.0;
    
    /**
     * Tests concurrent measurement additions using virtual threads.
     * Demonstrates:
     * 1. Virtual thread per task executor
     * 2. CountDownLatch for synchronization
     * 3. ConcurrentLinkedQueue for thread-safe collection
     * 4. Observer pattern with concurrent notifications
     */
    @Test
    @DisplayName("Should handle concurrent measurements with virtual threads")
    void testConcurrentMeasurementsWithVirtualThreads() throws Exception {
        // Setup: Create network, sensor and observer
        SensorNetwork network = SensorNetwork.of("VirtualThreadTest");
        Sensor<Temperature> temperatureSensor = network.addTemperatureSensor("TempVirtual");
        // Thread-safe queue to collect measurements
        var received = new ConcurrentLinkedQueue<Measurement<Temperature>>();
        // Subscribe observer using method reference
        temperatureSensor.subscribe(received::add);
        // Latch to coordinate completion of all measurements
        var latch = new CountDownLatch(MEASUREMENT_COUNT);

        // Execute: Submit concurrent measurement tasks
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            // Create MEASUREMENT_COUNT virtual threads
            for (int i = 0; i < MEASUREMENT_COUNT; i++) {
                final int step = i;
                futures.add(executor.submit(() -> {
                    try {
                        // Add measurement with increasing time and temperature
                        temperatureSensor.addMeasurement(
                            Instant.now().plusSeconds(60 * step),
                            Quantities.getQuantity(BASE_TEMPERATURE + step, Units.CELSIUS)
                        );
                    } finally {
                        latch.countDown(); // Signal task completion
                    }
                }));
            }
            
            // Wait for all measurements to complete
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                fail("Timeout waiting for measurements");
            }
            
            // Ensure all futures completed without exceptions
            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS);
            }
        }

        // Verify: Multiple assertions to check thread safety
        assertAll(
            // Check if all measurements were received by observer
            () -> assertEquals(MEASUREMENT_COUNT, received.size(), "Observer notification count"),
            // Verify all measurements were stored
            () -> assertEquals(MEASUREMENT_COUNT, temperatureSensor.getMeasurementCount(), "Stored measurement count"),
            // Confirm maximum temperature was recorded
            () -> assertTrue(temperatureSensor.measurementStream()
                .mapToDouble(m -> m.getValue().getValue().doubleValue())
                .max()
                .orElse(0.0) >= BASE_TEMPERATURE + MEASUREMENT_COUNT - 1, "Max temperature check"),
            // Verify all timestamps are unique
            () -> assertEquals(MEASUREMENT_COUNT, temperatureSensor.measurementStream()
                .map(Measurement::getTimestamp)
                .distinct()
                .count(), "Distinct timestamp count")
        );
    }
}