# RAG LangChain4J Project

A Retrieval-Augmented Generation (RAG) system built with LangChain4J and Milvus vector database. This project demonstrates how to build an intelligent question-answering system that can retrieve relevant information from a document corpus and generate contextual answers.

## Features

- **Document Storage**: Store and index documents in Milvus vector database
- **Semantic Search**: Find relevant documents using vector similarity search
- **Question Answering**: Generate answers using retrieved context with LLMs
- **Interactive CLI**: Command-line interface for real-time Q&A
- **Docker Support**: Easy deployment with Docker Compose
- **Extensible Architecture**: Modular design for easy customization

## Technologies Used

- **LangChain4J**: Java framework for LLM applications
- **Milvus**: Open-source vector database for similarity search
- **Docker**: Containerization platform
- **Maven**: Build and dependency management
- **OpenAI GPT**: Large language model for text generation
- **AllMiniLM-L6-v2**: Sentence transformer for embeddings

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker and Docker Compose
- OpenAI API key (optional, for better answers)

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd rag-langchain4j
```

### 2. Start Milvus with Docker

```bash
docker-compose up -d
```

This will start:
- Milvus vector database (port 19530)
- Etcd (for metadata storage)
- MinIO (for object storage)
- Attu (Milvus admin UI on port 3000)

### 3. Set OpenAI API Key (Optional)

```bash
# Windows
set OPENAI_API_KEY=your-api-key-here

# Linux/Mac
export OPENAI_API_KEY=your-api-key-here
```

### 4. Build and Run the Application

```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.rag.RagApplication"
```

## Project Structure

```
rag-langchain4j/
├── src/main/java/com/example/rag/
│   ├── RagApplication.java          # Main application class
│   ├── model/
│   │   └── Document.java            # Document model
│   └── service/
│       ├── DocumentService.java     # Milvus integration
│       └── RagService.java          # RAG implementation
├── src/main/resources/
│   └── logback.xml                  # Logging configuration
├── docker-compose.yml               # Docker services
├── pom.xml                          # Maven configuration
└── README.md                        # This file
```

## How It Works

1. **Document Ingestion**: Documents are converted to embeddings using AllMiniLM-L6-v2 model
2. **Vector Storage**: Embeddings are stored in Milvus with metadata
3. **Query Processing**: User questions are converted to embeddings
4. **Similarity Search**: Milvus finds the most relevant documents
5. **Answer Generation**: Retrieved context is used to generate answers with GPT

## Configuration

### Milvus Connection

The application connects to Milvus on `localhost:19530` by default. You can modify the connection settings in `DocumentService.java`.

### Embedding Model

The project uses AllMiniLM-L6-v2 for generating embeddings. This model runs locally and doesn't require an API key.

### LLM Configuration

By default, the application uses OpenAI's GPT-3.5-turbo. If no API key is provided, it falls back to mock responses showing the retrieved documents.

## Usage Examples

### Interactive Mode

Run the application and ask questions:

```
=== RAG Question-Answering System ===
Ask questions about the loaded documents. Type 'exit' to quit.

Question: What is Java?
Answer: Java is a high-level, class-based, object-oriented programming language...

Question: How does Milvus work?
Answer: Milvus is an open-source vector database that can store, index, and manage...
```

### Adding Custom Documents

Modify the `loadSampleDocuments` method in `RagApplication.java` to add your own documents.

## Docker Services

- **Milvus**: Vector database (localhost:19530)
- **Attu**: Web UI for Milvus (localhost:3000)
- **Etcd**: Metadata storage
- **MinIO**: Object storage (localhost:9000)

## Development

### Building the Project

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Packaging

```bash
mvn clean package
```

## Troubleshooting

### Milvus Connection Issues

1. Ensure Docker containers are running: `docker-compose ps`
2. Check Milvus health: `curl http://localhost:9091/healthz`
3. View logs: `docker-compose logs milvus`

### Memory Issues

- Increase JVM heap size: `-Xmx4g`
- Adjust Docker memory limits in docker-compose.yml

### API Key Issues

- Verify OpenAI API key is set correctly
- Check API quota and billing status
- The application works without API key (mock mode)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Resources

- [LangChain4J Documentation](https://docs.langchain4j.dev/)
- [Milvus Documentation](https://milvus.io/docs)
- [OpenAI API Documentation](https://platform.openai.com/docs)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
