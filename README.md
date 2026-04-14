<div align="center">
<img src="https://bval.apache.org/images/bval-logo.png" width="300" />
</div>
<br>

![Maven Central](https://img.shields.io/maven-central/v/org.apache.bval/bval-jsr)
[![ASF Build Status](https://github.com/apache/bval/workflows/BVal%20CI/badge.svg)](https://github.com/apache/bval/actions/workflows/bval-ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Apache's implementation of the Jakarta Validation specification for Jakarta EE and Java SE

## Branches

### master / 3.1.x
Jakarta Validation 3.1 implementation (Jakarta EE 11)  
Requires Java 17+

### 3.0.x
![3.0.x](https://img.shields.io/maven-central/v/org.apache.bval/bval-jsr?versionPrefix=3.0&color=cyan)  
Jakarta Bean Validation 3.0 implementation (Jakarta EE 10)  
Requires Java 11+

### 2.x
![2.x](https://img.shields.io/maven-central/v/org.apache.bval/bval-jsr?versionPrefix=2.&color=cyan)  
Bean Validation 2.0 (JSR-380) implementation (Java EE 8)  
Requires Java 8+

### 1.1.x
![1.1.x](https://img.shields.io/maven-central/v/org.apache.bval/bval-jsr?versionPrefix=1.1&color=cyan)  
Bean Validation 1.1 (JSR-349) implementation (Java EE 7)  
Requires Java 6+

## Minimum Requirements (master)

- Java 17+
- Jakarta Validation 3.1 (Jakarta EE 11)
- TCK compliant

## Installation

```shell
mvn clean install
```

## Usage

### Dependency

```xml
<dependency>
    <groupId>org.apache.bval</groupId>
    <artifactId>bval-jsr</artifactId>
    <version>${bval.version}</version>
</dependency>
```

### Optional: extra constraints

```xml
<dependency>
    <groupId>org.apache.bval</groupId>
    <artifactId>bval-extras</artifactId>
    <version>${bval.version}</version>
</dependency>
```

### Optional: OSGi bundle

```xml
<dependency>
    <groupId>org.apache.bval</groupId>
    <artifactId>org.apache.bval.bundle</artifactId>
    <version>${bval.version}</version>
</dependency>
```

### Basic usage

```java
ValidatorFactory validatorFactory = Validation
    .byProvider(ApacheValidationProvider.class)
    .configure()
    .buildValidatorFactory();

Validator validator = validatorFactory.getValidator();
Set<ConstraintViolation<MyBean>> violations = validator.validate(myBean);
```

## More Information

Please visit https://bval.apache.org for full documentation and release notes.

Issue tracker: https://issues.apache.org/jira/browse/BVAL









# Apache BVal
[![Build Status](https://github.com/apache/bval/workflows/BVal%20CI/badge.svg)](https://github.com/apache/bval/actions/workflows/bval-ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This is an implementation of the Jakarta Validation (JSRs 303, 349, 380) specification for Jakarta EE and Java SE.
The technical objective is to provide a class level constraint declaration and validation facility for the Java application developer, as well as a constraint
metadata repository and query API.
See: https://beanvalidation.org/

## Branches

### Master / 3.1.x

Jakarta Validation 3.1 implementation

### 3.0.x

Bean Validation 3.0 implementation

### 2.x

Bean Validation 2.x implementation

### 1.1.x

Bean Validation 1.x implementation

## Installation

```shell
mvn clean install
```
