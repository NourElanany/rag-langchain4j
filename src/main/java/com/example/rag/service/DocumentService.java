package com.example.rag.service;

import com.example.rag.model.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service class for managing documents in Milvus vector database.
 * Handles document storage, embedding generation, and similarity search.
 */
public class DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private static final String COLLECTION_NAME = "documents";
    private static final String ID_FIELD = "id";
    private static final String CONTENT_FIELD = "content";
    private static final String VECTOR_FIELD = "vector";
    private static final int VECTOR_DIM = 384; // AllMiniLmL6V2 embedding dimension
    
    private MilvusServiceClient milvusClient;
    private EmbeddingModel embeddingModel;

    public DocumentService() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Initialize the connection to Milvus and create collection if needed.
     */
    public void initialize() {
        try {
            // Connect to Milvus
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost("localhost")
                    .withPort(19530)
                    .build();
            
            milvusClient = new MilvusServiceClient(connectParam);
            logger.info("Connected to Milvus successfully");

            // Create collection if it doesn't exist
            createCollectionIfNotExists();
            
        } catch (Exception e) {
            logger.error("Failed to initialize DocumentService", e);
            throw new RuntimeException("Failed to initialize DocumentService", e);
        }
    }

    private void createCollectionIfNotExists() {
        // Check if collection exists
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        
        R<Boolean> hasCollectionResponse = milvusClient.hasCollection(hasCollectionParam);
        if (hasCollectionResponse.getData()) {
            logger.info("Collection '{}' already exists", COLLECTION_NAME);
            return;
        }

        // Create collection schema
        FieldType idField = FieldType.newBuilder()
                .withName(ID_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(100)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName(CONTENT_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName(VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(VECTOR_DIM)
                .build();

        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDescription("Document collection for RAG system")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(contentField)
                .addFieldType(vectorField)
                .build();

        R<RpcStatus> createCollectionResponse = milvusClient.createCollection(createCollectionParam);
        if (createCollectionResponse.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create collection: " + createCollectionResponse.getMessage());
        }

        logger.info("Created collection '{}' successfully", COLLECTION_NAME);

        // Create index on vector field
        createVectorIndex();

        // Load collection
        LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        
        milvusClient.loadCollection(loadCollectionParam);
        logger.info("Loaded collection '{}' successfully", COLLECTION_NAME);
    }

    private void createVectorIndex() {
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFieldName(VECTOR_FIELD)
                .withIndexType(io.milvus.param.IndexType.IVF_FLAT)
                .withMetricType(io.milvus.param.MetricType.COSINE)
                .withExtraParam("{\"nlist\":128}")
                .build();

        R<RpcStatus> createIndexResponse = milvusClient.createIndex(createIndexParam);
        if (createIndexResponse.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create index: " + createIndexResponse.getMessage());
        }

        logger.info("Created vector index successfully");
    }

    /**
     * Add a document to the vector database.
     */
    public void addDocument(String id, String content) {
        try {
            // Generate embedding for the document content
            Embedding embedding = embeddingModel.embed(content).content();
            List<Float> vector = embedding.vectorAsList();

            // Prepare data for insertion
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field(ID_FIELD, Collections.singletonList(id)),
                    new InsertParam.Field(CONTENT_FIELD, Collections.singletonList(content)),
                    new InsertParam.Field(VECTOR_FIELD, Collections.singletonList(vector))
            );

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            R<MutationResult> insertResponse = milvusClient.insert(insertParam);
            if (insertResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to insert document: " + insertResponse.getMessage());
            }

            logger.debug("Added document with ID: {}", id);
            
        } catch (Exception e) {
            logger.error("Error adding document with ID: " + id, e);
            throw new RuntimeException("Error adding document", e);
        }
    }

    /**
     * Search for similar documents based on a query.
     */
    public List<Document> searchSimilarDocuments(String query, int topK) {
        try {
            // Generate embedding for the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            List<Float> queryVector = queryEmbedding.vectorAsList();

            // Perform similarity search
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                    .withOutFields(Arrays.asList(ID_FIELD, CONTENT_FIELD))
                    .withTopK(topK)
                    .withVectors(Collections.singletonList(queryVector))
                    .withVectorFieldName(VECTOR_FIELD)
                    .withParams("{\"nprobe\":10}")
                    .build();

            R<SearchResults> searchResponse = milvusClient.search(searchParam);
            if (searchResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Search failed: " + searchResponse.getMessage());
            }

            // Parse search results
            List<Document> documents = new ArrayList<>();
            SearchResults results = searchResponse.getData();
            
            if (results.getResults().getTopK() > 0) {
                List<SearchResults.QueryResult> queryResults = results.getResults().getQueryResults();
                for (SearchResults.QueryResult queryResult : queryResults) {
                    for (int i = 0; i < queryResult.getVectorIds().size(); i++) {
                        String id = (String) queryResult.getVectorIds().get(i);
                        String content = (String) queryResult.getRow(i).get(CONTENT_FIELD);
                        float score = queryResult.getScores().get(i);
                        
                        documents.add(new Document(id, content, score));
                    }
                }
            }

            logger.debug("Found {} similar documents for query: {}", documents.size(), query);
            return documents;
            
        } catch (Exception e) {
            logger.error("Error searching for similar documents", e);
            throw new RuntimeException("Error searching for similar documents", e);
        }
    }

    /**
     * Get the total number of documents in the collection.
     */
    public long getDocumentCount() {
        try {
            GetCollectionStatisticsParam param = GetCollectionStatisticsParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .build();
            
            R<GetCollectionStatisticsResponse> response = milvusClient.getCollectionStatistics(param);
            if (response.getStatus() == R.Status.Success.getCode()) {
                return response.getData().getStats().getOrDefault("row_count", "0").equals("0") ? 0 : 
                       Long.parseLong(response.getData().getStats().get("row_count"));
            }
            return 0;
        } catch (Exception e) {
            logger.warn("Could not get document count", e);
            return 0;
        }
    }

    /**
     * Close the Milvus connection.
     */
    public void close() {
        if (milvusClient != null) {
            milvusClient.close();
            logger.info("Closed Milvus connection");
        }
    }
}
