package fr.univtln.bruno.samples.simplesensors.statistics;

import java.util.stream.DoubleStream;

import javax.measure.Quantity;
import javax.measure.Unit;

import tech.units.indriya.quantity.Quantities;

/**
 * Utility class for performing statistical calculations on measurement values.
 * Provides methods to calculate average, minimum, maximum, variance, standard
 * deviation, and median of measurements.
 *
 * @param <Q> the type of quantity
 */
public class MeasurementsUtils<Q extends Quantity<Q>> {

    /**
     * Error message when no measurements are found.
     */
    private static final String NO_MEASUREMENTS_FOUND = "No measurements found";

    /**
     * Returns the average measurement value.
     *
     * @param measurements the measurements
     * @param unit         the unit of the measurements
     * @return The average measurement value
     */
    public Quantity<Q> getAverageMeasurementQuantity(DoubleStream measurements, Unit<Q> unit) {
        return Quantities.getQuantity(
                measurements.average().orElseThrow(() -> new IllegalStateException(NO_MEASUREMENTS_FOUND)),
                unit);
    }

    /**
     * Returns the minimum measurement value.
     * @param measurements the measurements
     * @param unit         the unit of the measurements
     *
     * @return The minimum measurement value
     */
    public Quantity<Q> getMinMeasurementValue(DoubleStream measurements, Unit<Q> unit) {
        return Quantities.getQuantity(
                measurements.min().orElseThrow(() -> new IllegalStateException(NO_MEASUREMENTS_FOUND)),
                unit);
    }

    /**
     * Returns the maximum measurement value.
     * @param measurements the measurements
     * @param unit         the unit of the measurements
     *
     * @return The maximum measurement value
     */
    public Quantity<Q> getMaxMeasurementValue(DoubleStream measurements, Unit<Q> unit) {
        return Quantities.getQuantity(
                measurements.max().orElseThrow(() -> new IllegalStateException(NO_MEASUREMENTS_FOUND)),
                unit);
    }

    /**
     * Calculates variance of measurement values
     * The variance is the average of the squared differences from the Mean
     * The unit of the variance is the square of the unit of the measurements
     *
     * @param mean         the mean of the measurements
     * @param measurements the measurements
     * @param unit         the unit of the measurements
     * @return the variance
     *
     */
    public Quantity<?> getVariance(Quantity<Q> mean, DoubleStream measurements, Unit<Q> unit) {
        double variance = measurements
                .map(x -> Math.pow(x - mean.getValue().doubleValue(), 2))
                .average()
                .orElseThrow(() -> new IllegalStateException(NO_MEASUREMENTS_FOUND));
        return Quantities.getQuantity(variance, unit.multiply(unit)); // unit squared
    }

    /**
     * Calculates standard deviation of measurement values
     * @param variance     the variance of the measurements
     * @param unit         the unit of the measurements
     */
    public Quantity<Q> getStandardDeviation(Quantity<?> variance, Unit<Q> unit) {
        return Quantities.getQuantity(Math.sqrt(variance.getValue().doubleValue()), unit);
    }

    /**
     * Calculates median of measurement values
     * @param numberOfMeasurements the number of measurements
     * @param measurements         the measurements
     * @param unit                 the unit of the measurements
     */
    public Quantity<Q> getMedian(long numberOfMeasurements, DoubleStream measurements, Unit<Q> unit) {
        if (numberOfMeasurements == 0) {
            throw new IllegalStateException(NO_MEASUREMENTS_FOUND);
        }

        double median = (numberOfMeasurements % 2 == 0) ?
        // Even: average of two middle values
                measurements.sorted()
                        .skip(numberOfMeasurements / 2 - 1)
                        .limit(2)
                        .average()
                        .orElseThrow(() -> new IllegalStateException(NO_MEASUREMENTS_FOUND))
                :
                // Odd: middle value
                measurements.sorted()
                        .skip(numberOfMeasurements / 2)
                        .limit(1)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(NO_MEASUREMENTS_FOUND));

        return Quantities.getQuantity(median, unit);
    }

}
