FROM openjdk:17-jdk-slim

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean compile

# Expose port (if needed for future web interface)
EXPOSE 8080

# Run the application
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.example.rag.RagApplication"]
