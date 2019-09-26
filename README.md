JUnit Extension for [recheck](https://github.com/retest/recheck). Automatic set up and tear down of tests using recheck.

## Features
* Calls `startTest` on all `RecheckLifecycle` objects before each test.
* Calls `capTest` on all `RecheckLifecycle` objects after each test.
* Calls `cap` on all `RecheckLifecycle` objects after all tests.

## Advantages
The extension automatically calls `startTest`, `capTest` and `cap`. So it is no longer required to call those methods manually. This reduces boiler plate code and ensures the lifecycle within a test using recheck.

## Usage
Recheck JUnit extension uses JUnit's extension mechanism. It can be used by adding `@ExtendWith(RecheckExtension.class)` to your test class.

### Prerequisites

Requires at least JUnit Jupiter.