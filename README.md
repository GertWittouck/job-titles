# Job Title Normaliser

Maps free-text job titles to the closest title in a list of normalised job titles.

| Input              | Normalised          |
|--------------------|---------------------|
| `Lead Architect`   | `Architect`         | 
| `Java engineer`    | `Software engineer` |
| `C# engineer`      | `Software engineer` |
| `Accountant`       | `Accountant`        |
| `Chief Accountant` | `Accountant`        |

## Requirements

- Java 25
- Maven 3.9+

## Build, test and run

```bash
mvn clean test                              # run all unit tests
mvn clean package                           # compile, test and package
```

To run the assignment examples, run `Main` from your IDE, or from the command line (this includes Log4j on the classpath):

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" com.feefo.Main
```

You can also import the project into IntelliJ IDEA or Eclipse as a Maven project and run `Main` or the tests from there.

## Usage

```java
NormaliserConfig config = NormaliserConfig.load();   // reads normaliser.properties
Normaliser n = new Normaliser(config, new TokenSimilarityScorer());
JobTitleMatch match = n.normalise("Chief Accountant");

match.normalised(); // "Accountant"
match.quality();    // 0.78
match.status();     // PARTIAL
```

**Deviations from the assignment's sample code:**

- The sample code creates the normaliser with `new Normaliser()`. Here the configuration and scorer are passed in, because loading configuration is not the normaliser's responsibility. `Main` loads the configuration once at startup and wires the application together.
- The sample code expects `normalise()` to return a `String`. Here it returns a `JobTitleMatch`, so the caller gets the normalised title, the quality and the match status from a single call. To get just the title, as in the sample code:

```java
String normalisedTitle = n.normalise(jt).normalised(); // null when there is no match
```

## Configuration

The assignment says the process is *provided with* a list of normalised job titles, with the four titles as a concrete example. So the list is treated as input to the normaliser rather than hardcoded. `NormaliserConfig.load()` reads the example list from the properties file, and code can pass in any other list with `new Normaliser(new NormaliserConfig(titles, minimumQuality), scorer)`.

Settings are in `src/main/resources/normaliser.properties`:

| Property                     | Description                                                        | Default                                                    |
|------------------------------|--------------------------------------------------------------------|------------------------------------------------------------|
| `normaliser.titles`          | Comma-separated list of normalised job titles                      | `Architect,Software engineer,Quantity surveyor,Accountant` |
| `normaliser.minimum-quality` | Quality (0.0 to 1.0) the best match must reach to count as a match | `0.4`                                                      |

## Logging

The project logs through [Log4j 2](https://logging.apache.org/log4j/2.x/). The configuration is in `src/main/resources/log4j2.xml` and writes to the console.

| Level   | What is logged                                          |
|---------|---------------------------------------------------------|
| `INFO`  | The configuration that was loaded, and `Main`'s results |
| `DEBUG` | Every match: input, best title, quality and status      |
| `TRACE` | The score of every candidate title for each input       |

To see more detail, change the level of the `com.feefo` logger in `log4j2.xml` to `DEBUG` or `TRACE`. During the tests, `src/test/resources/log4j2-test.xml` is used instead, and it only shows warnings and errors.

## Design

```
com.feefo
├── Main                       runs the assignment examples
└── normalise
    ├── Normaliser             finds the best matching normalised title
    ├── JobTitleMatch          result: original, normalised, quality, status
    ├── MatchStatus            EXACT | PARTIAL | NO_MATCH
    ├── SimilarityScorer       interface: scores input vs. candidate (0.0 to 1.0)
    ├── TokenSimilarityScorer  word-based implementation of SimilarityScorer
    ├── NormaliserConfig       validated configuration, loaded from the properties file
    └── ConfigurationException missing or invalid configuration
```

- **Single responsibility**: matching, scoring and configuration each live in their own class.
- **Strategy pattern**: `Normaliser` depends on the `SimilarityScorer` interface, so the scoring algorithm can be replaced (for example, by a semantic or synonym-based scorer) without changing the normaliser. The tests use this to check the threshold logic with a stub scorer.
- **Immutable value objects**: `JobTitleMatch` and `NormaliserConfig` are records that validate their state when they are created, so an invalid instance cannot exist.
- **Dependency injection**: every class takes its collaborators through the constructor. `Main` is the single place where the configuration is loaded and the objects are created.

### How the quality score works

`TokenSimilarityScorer` compares titles word by word:

1. Both titles are lower-cased and split into words on anything that is not a letter or digit (`"C# engineer"` → `c`, `engineer`).
2. Two words are compared using the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) from Apache Commons Text: `similarity = 1 - distance / length of the longer word`. This also tolerates typos and word forms (`account` vs `accountant` = 0.7).
3. Each word is paired with its most similar word in the other title, and those best similarities are averaged.
4. Step 3 is done in both directions and the two averages are averaged again. This keeps the score symmetric, and an extra word like "Chief" lowers the score without ruling the match out.

The normaliser scores every normalised title and keeps the highest:

| Quality                       | Status     | `normalised()`  |
|-------------------------------|------------|-----------------|
| 1.0                           | `EXACT`    | the title       |
| ≥ minimum quality, < 1.0      | `PARTIAL`  | the title       |
| < minimum quality             | `NO_MATCH` | `null`          |

**Limitation:** the matching is lexical, not semantic. It recognises shared or similar *words*, but not meaning, so `Bookkeeper` won't map to `Accountant`. If two titles score equally, the first one in the configuration wins.

## Error handling

| Situation                                             | Behaviour                                                           |
|-------------------------------------------------------|---------------------------------------------------------------------|
| Input is `null` or blank                              | `IllegalArgumentException`                                          |
| No title reaches the minimum quality                  | `JobTitleMatch` with status `NO_MATCH` and no normalised title      |
| Properties file missing, key missing or invalid value | `ConfigurationException` naming the file and the property           |
| `null` collaborators or invalid record values         | `NullPointerException` / `IllegalArgumentException` at construction |

The library classes throw, and `Main` decides how to report it to the user. A configuration error is logged as a single clear message, and the program exits with code 1. An invalid job title is logged as a warning and skipped. Any other exception is logged with its stack trace, and the program exits with code 1.

## Development approach

The project was built test-first: for each class the tests were written first, failed, and then the implementation was added to make them pass. The tests are in `src/test/java` and follow a Given/When/Then and Arrange/Act/Assert structure. The invalid configuration files used by the tests are in `src/test/resources/config`.
