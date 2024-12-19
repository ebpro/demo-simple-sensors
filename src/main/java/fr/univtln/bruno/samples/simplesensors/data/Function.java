package fr.univtln.bruno.samples.simplesensors.data;

/**
 * This is an interface representing a mathematical function.
 * It is used by the FunctionBasedMeasureGenerator class to generate measures based on a mathematical function.
 * The function is expected to be a mathematical function that takes time as input and returns a double as output.
 *
 * Example physics functions:
 * <ul>
 * <li> Linear temperature increase: f(t) = T₀ + αt
 *    where T₀ is initial temperature and α is heating rate in °C/s
 *    Implementation: t -> 20 + 0.1 * t
 * </li>
 * <li> Exponential cooling (Newton's law): f(t) = T_ambient + (T₀ - T_ambient)e^(-kt)
 *    where k is cooling constant, T₀ initial temp, T_ambient is ambient temp
 *    Implementation: t -> 20 + (100 - 20) * Math.exp(-0.1 * t)
 * </li>
 * <li> Harmonic pressure oscillation: f(t) = P₀ + A*sin(ωt)
 *    where P₀ is mean pressure, A is amplitude, ω is angular frequency
 *    Implementation: t -> 1013.25 + 10 * Math.sin(0.1 * t)
 * </li>
 * <li> Radioactive decay: f(t) = N₀e^(-λt)
 *    where N₀ is initial quantity, λ is decay constant
 *    Implementation: t -> 1000 * Math.exp(-0.693 * t / 3600)
 * </li>
 * </ul>
* Example implementations:
 *
 *
 * Using anonymous class:
 * <pre>
 * {@code
 * Function f2 = new Function() {
 *     // Constants for water cooling
 *     private final double AMBIENT_TEMP = 20.0;  // °C
 *     private final double INITIAL_TEMP = 100.0; // °C
 *     private final double COOLING_RATE = 0.1;   // per second
 *
 *     @Override
 *     public double apply(long t) {
 *         return AMBIENT_TEMP +
 *                (INITIAL_TEMP - AMBIENT_TEMP) *
 *                Math.exp(-COOLING_RATE * t);
 *     }
 * };
 * }
 * </pre>
 * Designed to be used with the FunctionBasedMeasureGenerator class.
 * @see FunctionBasedMeasureGenerator
 *
 */
public interface Function {
    /**
     * Applies this function to the given duration in seconds.
     *
     * @param x the duration in seconds
     * @return the function result
     */
    double apply(long x);
}
