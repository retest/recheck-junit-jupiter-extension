# <a href="https://retest.dev"><img src="https://assets.retest.org/retest/ci/logos/recheck-screen.svg" width="300"/></a>

[![Build Status](https://travis-ci.com/retest/recheck-junit-jupiter-extension.svg?branch=master)](https://travis-ci.com/retest/recheck-junit-jupiter-extension)
[![Latest recheck-junit-jupiter-extension on Maven Central](https://maven-badges.herokuapp.com/maven-central/de.retest/recheck-junit-jupiter-extension/badge.svg?style=flat)](https://mvnrepository.com/artifact/de.retest/recheck-junit-jupiter-extension)
[![Latest recheck-junit-jupiter-extension releases on JitPack](https://jitpack.io/v/de.retest/recheck-junit-jupiter-extension.svg)](https://jitpack.io/#de.retest/recheck-junit-jupiter-extension)
[![license](https://img.shields.io/badge/license-AGPL-brightgreen.svg)](https://github.com/retest/recheck-junit-jupiter-extension/blob/master/LICENSE)
[![PRs welcome](https://img.shields.io/badge/PRs-welcome-ff69b4.svg)](https://github.com/retest/recheck-junit-jupiter-extension/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22)
[![code with hearth by retest](https://img.shields.io/badge/%3C%2F%3E%20with%20%E2%99%A5%20by-retest-C1D82F.svg)](https://retest.de/)

JUnit Extension for [recheck](https://github.com/retest/recheck). Automatic set up and tear down of tests using recheck.

## Features

* Calls `startTest` on all `RecheckLifecycle` objects before each test.
* Calls `capTest` on all `RecheckLifecycle` objects after each test.
* Calls `cap` on all `RecheckLifecycle` objects after all tests.

## Advantages

The extension automatically calls `startTest`, `capTest` and `cap`. So it is no longer required to call those methods manually. This reduces boilerplate code and ensures the lifecycle within a test using recheck.

## Build tools

You can add ***recheck-junit-jupiter-extension*** as an external dependency to your project. It is available via the [release-page](https://github.com/retest/recheck-junit-jupiter-extension/releases) which allows you to include it into your favorite build tool or via [Maven central](https://mvnrepository.com/artifact/de.retest/recheck-junit-jupiter-extension): [![Latest recheck-junit-jupiter-extension on Maven Central](https://maven-badges.herokuapp.com/maven-central/de.retest/recheck-junit-jupiter-extension/badge.svg?style=flat)](https://mvnrepository.com/artifact/de.retest/recheck-junit-jupiter-extension)

### Maven

```xml
<dependency>
	<groupId>de.retest</groupId>
	<artifactId>recheck-junit-jupiter-extension</artifactId>
	<version>${LATEST_VERSION_FROM_ABOVE_LINK}</version>
</dependency>
```

### Gradle

```gradle
compile 'de.retest:recheck-junit-jupiter-extension:${LATEST_VERSION_FROM_ABOVE_LINK}'
```

## Usage

The recheck JUnit extension uses JUnit's extension mechanism. It can be used as a [declarative extension](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-declarative) by adding `@ExtendWith(RecheckExtension.class)` to your test class or [globally/automatically](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic) by adding a `/META-INF/services/org.junit.jupiter.api.extension.Extension` file to your project with `de.retest.recheck.junit.jupiter.RecheckExtension` as its sole contents and setting the `junit.jupiter.extensions.autodetection.enabled=true` parameter e.g. in your `pom.xml` or as a JVM system property.

### Example

A simple example that will visit a page and capture it.

```java
@ExtendWith(RecheckExtension.class)
public class SimpleTest {
	private RecheckDriver driver;

	@BeforeEach
	void setUp() {
		driver = new RecheckDriver(new ChromeDriver());
	}

	@AfterEach
	void tearDown() {
		driver.quit();
	}

	@Test
	void hello_example_dot_com() {
		driver.get( "https://example.com/" );
	}
}
```

### Prerequisites

Requires at least [JUnit Jupiter](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter). For JUnit 4 support look at [recheck extension for JUnit 4](https://github.com/retest/recheck-junit-4-extension).

## License

This project is licensed under the [AGPL license](LICENSE).
