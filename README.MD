# APIstax Java Client

[![Maven Central](https://img.shields.io/maven-central/v/io.apistax/apistax-client.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.apistax/apistax-client)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

APIstax Java client is a complete Java client implementation for the [APIstax](https://apistax.io) platform.

## Usage

Add the following dependency to your build file:

### Maven pom.xml

```xml

<dependency>
    <groupId>io.apistax</groupId>
    <artifactId>apistax-client</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Gradle build.gradle
```groovy
implementation("io.apistax:apistax-client:$latestVersion")
```

Get your APIstax API key [here](https://app.apistax.io/api-keys).

Initialise an `APIstaxClient` and start using it.

```java
public class VatService {

    private APIstaxClient client;
    
    public VatService(String apiKey) {
        client = new APIstaxClient.Builder()
                .apiKey(apiKey)
                .build();
    }

    public boolean isValid(String vatId) {
        VatVerificationResult result = client.verifyVatId(vatId);
        return result.getValid() == true;
    }
}
```

The further information and documentation about the APIs can be found on [APIstax documentation](https://apistax.io/docs) page.