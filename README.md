# Quick Reference

### Build & Run

```bash
mvn clean package
java -jar target/censored-survivors-1.0-SNAPSHOT.jar
```

### Quick Run (Development)

```bash
mvn compile exec:java -Dexec.mainClass="com.censoredsurvivors.App"
```

### Test

```bash
mvn test
```

### Dependencies

```bash
mvn dependency:tree    # Show dependency tree
mvn install           # Install dependencies
```

### Package

```bash
mvn package           # Create JAR
```