# SimpleSensors - Java 101 Learning Project

A comprehensive example project demonstrating core Java concepts through a sensor network simulation.

## Prerequisites

- Java 21+
- Maven 3.8+
- macOS/Linux/Windows

## Quick Start

```bash
git clone https://github.com/ebpro/SimpleSensors.git
cd SimpleSensors
./mvnw -P shadedjar package 
java -jar target/SimpleSensors-*-SNAPSHOT-withdependencies.jar 
```

## Basic Object-Oriented Programming

### Classes and Objects

- `Measurement`: Immutable class representing a single sensor reading
  - **Immutability**: All fields are final, ensuring thread safety
  - **equals/hashCode**: (Redefined) Two measurements are equal if from same sensor at same time
  - **Comparable**: (Interface implemented) Orders by sensor ID first, then reverse chronological. Latest measure first, grouped by sensor.
  - **Benefits**:
    - Safe for use in concurrent collections
    - Consistent ordering in `TreeSet`
    - No need for defensive copying
    - Cache-friendly
  - Factory method `of` for creating new instances (using private constructor and generated by lombok library)

- `Sensor`: Basic class representing a physical sensor

  - **Collections Management**:
    - Ordered measurements using `TreeSet` through `SortedSet` interface (easily changeeable implementation).
    - Automatic timestamp-based sorting to efficiently retrieve latest measurements.
  
  - **Iteration Patterns**:
    - Classic iterator access
    - Stream-based processing
    - Filtered views of measurements
  
  - **Thread Safety**:
    - Synchronized collections
    - Concurrent observer notifications
    - Cache invalidation handling
  
  - **Statistical Processing**:
    - Cached computations
    - Lazy evaluation
    - Memory-efficient processing
  
  - **Benefits**:
    - Type-safe measurements
    - Thread-safe operations
    - Memory efficient

### Inheritance and Interfaces

#### `Map` Interface in `SensorMap`

- **Implementation Benefits**:
  - Delegated operations to `ConcurrentHashMap` (changeable thriougth the factory).
  - Thread-safe operations
  - Compatible with Java collections framework
  - Generic type support for sensors

#### Comparable Interface in Measurement

- **Natural Ordering**:
  - Primary: Sensor ID grouping
  - Secondary: Reverse chronological
  - Consistent with `equals()` and `hashCode()`
  - Enables use in sorted collections

#### Interface Hierarchy

- **SensorMap**:
  - Implements Map<UUID, Sensor<?>>
  - Provides type-safe sensor access
  - Supports concurrent operations
  - Maintains sensor identity

- **Measurement**:
  - Implements Comparable<Measurement<Q>>
  - Provides natural ordering
  - Enables TreeSet storage
  - Guarantees consistency

#### Benefits

- **Polymorphism**:
  - Use as standard collections
  - Interoperable with Java APIs
  - Swappable implementations
  - Type-safe operations

## Java Generics

Generics are used throughout the project to ensure type safety and code reuse.
Sensors can be of any type of Quantity, like Temperature, Pressure, etc., and accept only measurements of that type.
Measurements are also generic to ensure type safety and consistency. SensorNetwork is a generic class that can manage any type of sensor.

### Generic Classes

Quantity is a generic type representing a physical quantity. It is used to ensure type safety in measurements.
So a Sensor can be of any type of Quantity, like Temperature, Pressure, etc. and accept only measurements of that type.

```java
public class Sensor<Q extends Quantity<Q>> {
    private final SortedSet<Measurement<Q>> measurements;
}
```

### Bounded Type Parameters

Type parameters can be bounded to ensure they are of a specific type or subtype.

```java
public class MeasurementsUtils<Q extends Quantity<Q>>
```

### Type Erasure and Generic Types

In java Generic type information is removed at runtime.
It is used only for compile-time type checking.

Our project handles this:

```java
// At compile time
Sensor<Temperature> tempSensor;
Sensor<Pressure> pressureSensor;

// At runtime becomes
Sensor sensor1;
Sensor sensor2;

// Type checking using Class<Q>
public class Sensor<Q extends Quantity<Q>> {
    private final Class<Q> quantityClass;
}
```

### Variance Examples in Sensor Project

Variance is the relationship between subtypes of one type and subtypes of another type.
We illustrate this using the Producer-Extends and Consumer-Super principle. PECS is a mnemonic for "Producer Extends, Consumer Super". 

#### Producer (EXTENDS) - Reading Values

```java
// Safe to READ FROM because we know the type must be Measurement<Q> or a subtype
public Stream<? extends Measurement<Q>> measurementStream() {
    return measurements.stream();
}

// Usage example:
Sensor<Temperature> sensor = new Sensor<>();
Stream<? extends Measurement<Temperature>> stream = sensor.measurementStream();
// Safe: We know these are Temperature measurements
stream.forEach(m -> processTemperature(m));
```

#### Consumer (SUPER) - Writing Values

```java
// Safe to WRITE TO because we can add Measurement<Q> to any supertype collection
public void addToCollection(Collection<? super Measurement<Q>> dest) {
    dest.addAll(measurements);
}

// Usage example:
List<Measurement<Quantity<?>>> allMeasurements = new ArrayList<>();
Sensor<Temperature> tempSensor = new Sensor<>();
tempSensor.addToCollection(allMeasurements);  // Safe: Temperature IS-A Quantity
```

#### Invariant - Both Reading and Writing

```java
// Must be EXACT type match because we both read and write
private final SortedSet<Measurement<Q>> measurements = new TreeSet<>();

// We do both:
measurements.add(newMeasurement);      // WRITE
Measurement<Q> first = measurements.first();  // READ
```

## Collections Framework

Collections and Maps are used extensively in the project to manage sensors, measurements, and observers.

### Sorted Collections

Sorted collections are used to maintain order and enable efficient access to sensor measurements.

```java
private final SortedSet<Measurement<Q>> measurements = new TreeSet<>();
```

### Concurrent Collections

Concurrent collections are used to ensure thread safety in sensor operations.
Thread-safety means that multiple threads can access the collection concurrently without data corruption.
But Thread-safety implies a performance cost.

```java
private final Set<MeasurementObserver<Q>> observers = 
    ConcurrentHashMap.newKeySet();
```

## Functional Programming

### Lambda Expressions

Lambda expressions are used to define simple functions inline, making code more concise and readable.
Used for event handlers, data transformers, everywhere a single method interface is needed.

```java
Example: `sensor.subscribe(m -> System.out.println(m))`
```

The mathematical function provided to `FunctionBasedMeasurementGenerator` can be a lambda expression.

### Method References

Shorthand for lambdas referring to existing methods.
Types: Static (`Measurement::of`), Instance (`sensor::process`), Constructor (`Sensor::new`)

### Functional Interfaces

Single-method interfaces enabling lambda usage.

- **Built-in**:
  - `Predicate<T>`: Boolean tests
  - `Function<T,R>`: Transformations
  - `Consumer<T>`: Side effects
  - `Supplier<T>`: Value providers
  - ...

- **Custom**: Possible to define custom functional interfaces

### Streams

Streams are used to process collections of data in a functional style, enabling concise and efficient data processing.

Features Filtering, mapping, reduction, sorting, parallel processing, lazy evaluation, and more.

```java
public Stream<Measurement<Q>> measurementStream() {
    return measurements.stream();
}
```

### Example Usage

Complex data processing using Stream API:

```java
// Statistical operations
measurements.stream()
    .mapToDouble(m -> m.getValue().getValue().doubleValue())
    .average()
    .orElseThrow();
```

```java
// Filtering and mapping
sensorNetwork.sensorStream()
    .filter(s -> s instanceof Sensor<Temperature>)
    .flatMap(Sensor::measurementStream)
    .filter(m -> m.getValue().getValue().doubleValue() > 25.0)
    .map(Measurement::getTimestamp)
    .sorted()
    .collect(Collectors.toList());
```

A functional interface `MeasurementObserver` enables event-based measurement handling.
It is used to define the callback methods to be applied when a new measurement is received.

```java
@FunctionalInterface
public interface MeasurementObserver<Q extends Quantity<Q>> {
    void onMeasurement(Measurement<Q> measurement);
}

// Usage with lambda
sensor.subscribe(measurement -> 
    System.out.println("New value: " + measurement.getValue()));
```

### Optional

Handle nullable values safely.
Prevents NullPointerException through explicit handling using a functional style.

```java
public Optional<Measurement<Q>> getLastMeasurement() {
    return measurements.isEmpty() ? 
           Optional.empty() : 
           Optional.of(measurements.last());
}
```

## Design Patterns

Design patterns are reusable solutions to common software design problems. They provide:

- Proven development paradigms
- Common vocabulary for developers
- Best practices for code organization

### Pattern Categories

1. **Creational Patterns**
   - ✓ **Factory Method** (`Measurement.of()`)
   - ✓ **Builder** (`SensorNetwork.builder()`)
   - Abstract Factory
   - Singleton
   - Prototype

2. **Structural Patterns**
   - Adapter
   - ✓ **Composite** (`SensorNetwork` contains `Sensors`)
   - Decorator
   - Facade
   - Bridge
   - Flyweight
   - Proxy

3. **Behavioral Patterns**
   - ✓ **Observer** (`MeasurementObserver`)
   - ✓ **Strategy** (`FunctionBasedMeasureGenerator`)
   - Chain of Responsibility
   - Command
   - ✓ **Iterator** (`Sensor` measurements)
   - Mediator
   - Memento
   - State
   - Template Method
   - Visitor

### Patterns Used in SimpleSensors (✓)

#### Creational

- **Factory Method**: `Measurement.of()`
  - Creates validated measurements
  - Encapsulates creation logic
  - Ensures immutability

- **Builder**: `SensorNetwork.builder()`
  - Step-by-step network construction
  - Optional components
  - Fluent interface

#### Structural

- **Composite**: `SensorNetwork`
  - Manages sensor hierarchy
  - Uniform sensor access
  - Collection management

#### Behavioral

- **Observer**: `MeasurementObserver`
  - Measurement notifications
  - Loose coupling
  - Event-driven updates

- **Strategy**: `FunctionBasedMeasureGenerator`
  - Pluggable measurement algorithms
  - Runtime behavior selection
  - Clean separation of concerns

## Testing

Different types of tests are used to ensure the correctness and reliability of the project. 
Unit tests are used to test individual components, while integration tests are used to test the interaction between components.
Functional tests are used to test the system as a whole. Performance tests are used to test the performance of the system.

### Running Unit Tests

Test individual components in isolation.

Unit tests are written using JUnit5 and can be run using Maven.

```bash
./mvnw test
```

automatically run before the package phase.

### Integration Testing

Test component interactions.

Integration tests are also written using JUnit5 and can be run using Maven.

```bash
./mvnw verify
```

## Building and Running

### Maven Build System

Maven is a build automation and project management tool that:

- Uses convention over configuration
- Provides standardized project structure
- Manages dependencies automatically
- Follows defined build lifecycle phases
- Generates documentation and reports

### Project Structure

- `src/main/java`: Source code
- `src/test/java`: Test code
- `pom.xml`: Project configuration
- `target/`: Build output
- .gitignore: Files to ignore in git
- `README.md`: Project documentatio`
- `src/main/resources: Resources like configuration file`

### Build Lifecycle

Maven build lifecycle consists of several phases. 
Each phase is responsible for a specific task and depends on the previous phase.

The default lifecycle phases are:

1. **validate**: Check project correctness
2. **compile**: Compile source code
3. **test**: Run unit tests
4. **package**: Create JAR
5. **verify**: Run integration tests
6. **install**: Install to local repo
7. **deploy**: Deploy to remote repo

### Dependency Management

Maven simplifies project dependencies through its centralized repository system. 
All external libraries are declared in the `pom.xml` file and automatically downloaded from Maven Central, 
eliminating manual jar management. 
Dependencies can be scoped (compile, test, runtime, provided) to control their availability in different 
build phases. Maven also handles transitive dependencies - 
if Library A depends on Library B, adding A automatically includes B. 
Version conflicts are resolved through the "nearest definition" rule, though explicit version 
management is possible. The project's dependencies are cached locally (~/.m2/repository) 
for offline access and faster builds.

### Building, testing and running the Project

The project is built using Maven (see `pom.xml` for details).

The maven shaded plugin is used to create a single executable JAR file with all dependencies included.
It is called a "fat" or "uber" JAR. The JAR file is created in the `target` directory.
A manifest file is used to specify the main class to run creating the illusion that the JAR is an executable.

```bash
./mvnw -P shadedjar package 
```

```bash
java -jar target/SimpleSensors-*-SNAPSHOT-withdependencies.jar 
```

### Maven Version Management

- **Version Format**: MAJOR.MINOR.PATCH[-SNAPSHOT]
  - MAJOR: Breaking changes
  - MINOR: New features
  - PATCH: Bug fixes
  - SNAPSHOT: Development version

- **Maven Release Process**:
  1. Update pom.xml versions
  2. Build and test
  3. Tag release
  4. Deploy artifacts
  5. Update to next SNAPSHOT

## Maven Site Generation

The Maven Site Plugin generates comprehensive project documentation including:

### Available Reports

- JavaDoc API documentation
- Unit test results
- Code coverage (JaCoCo)
- Static analysis (SpotBugs)
- Dependency analysis
- Source cross-reference
- ...

```bash
./mvnw site
```

The generated site is available in the `target/site` directory.

## Git and Git Workflow

- **Main Branches**:
  - `main`|`master`: Production code, tags only
  - `develop`: Next release development

- **Supporting Branches**:
  - `feature/*`: New features
  - `release/*`: Release preparation
  - `hotfix/*`: Production fixes
  - `bugfix/*`: Development fixes

### GitFlow Process

Gitflow is a branching model that simplifies project management and release processes. 

```bash
git flow init -d # Initialize git flow
```

1. **Feature Development**:

   ```bash
   git flow feature start new-sensor
   git flow feature finish new-sensor
   ```

2. **Release Creation**:

   ```bash
   git flow release start 1.0.0
   mvn versions:set -DnewVersion=1.0.0
   git flow release finish 1.0.0
   ```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
