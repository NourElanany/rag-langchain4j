@echo off
echo Starting RAG LangChain4J Application...
echo.

echo Step 1: Starting Milvus with Docker Compose...
docker-compose up -d

echo.
echo Step 2: Waiting for Milvus to be ready...
timeout /t 30 /nobreak > nul

echo.
echo Step 3: Building and running the application...
mvn clean compile exec:java -Dexec.mainClass="com.example.rag.RagApplication"

pause
