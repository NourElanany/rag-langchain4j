package com.example.rag.service;

import com.example.rag.model.Document;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class that implements Retrieval-Augmented Generation (RAG).
 * Combines document retrieval from Milvus with text generation using LLMs.
 */
public class RagService {
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    private final DocumentService documentService;
    private final ChatLanguageModel chatModel;
    
    private static final int TOP_K_DOCUMENTS = 3;
    private static final double SIMILARITY_THRESHOLD = 0.7;

    public RagService(DocumentService documentService) {
        this.documentService = documentService;
        
        // Initialize the chat model (OpenAI GPT)
        // Note: You'll need to set the OPENAI_API_KEY environment variable
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("OPENAI_API_KEY not found. Using mock responses.");
            this.chatModel = null;
        } else {
            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-3.5-turbo")
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();
        }
    }

    /**
     * Answer a question using RAG approach:
     * 1. Retrieve relevant documents from vector database
     * 2. Use retrieved context to generate an answer with LLM
     */
    public String answer(String question) {
        try {
            // Step 1: Retrieve relevant documents
            List<Document> relevantDocs = documentService.searchSimilarDocuments(question, TOP_K_DOCUMENTS);
            
            if (relevantDocs.isEmpty()) {
                return "I couldn't find any relevant information to answer your question.";
            }

            // Filter documents by similarity threshold
            List<Document> filteredDocs = relevantDocs.stream()
                    .filter(doc -> doc.getScore() >= SIMILARITY_THRESHOLD)
                    .collect(Collectors.toList());

            if (filteredDocs.isEmpty()) {
                return "I found some documents, but they don't seem closely related to your question. Could you try rephrasing?";
            }

            // Step 2: Generate answer using retrieved context
            if (chatModel != null) {
                return generateAnswerWithLLM(question, filteredDocs);
            } else {
                return generateMockAnswer(question, filteredDocs);
            }
            
        } catch (Exception e) {
            logger.error("Error answering question: " + question, e);
            return "I encountered an error while processing your question. Please try again.";
        }
    }

    private String generateAnswerWithLLM(String question, List<Document> documents) {
        // Create context from retrieved documents
        String context = documents.stream()
                .map(doc -> "Document: " + doc.getContent())
                .collect(Collectors.joining("\n\n"));

        // Create prompt for the LLM
        String prompt = String.format(
                "Based on the following context, please answer the question. " +
                "If the context doesn't contain enough information to answer the question, " +
                "please say so clearly.\n\n" +
                "Context:\n%s\n\n" +
                "Question: %s\n\n" +
                "Answer:",
                context, question
        );

        try {
            String response = chatModel.generate(prompt);
            logger.debug("Generated answer for question: {}", question);
            return response;
        } catch (Exception e) {
            logger.error("Error generating answer with LLM", e);
            return generateMockAnswer(question, documents);
        }
    }

    private String generateMockAnswer(String question, List<Document> documents) {
        // Simple mock answer when LLM is not available
        StringBuilder answer = new StringBuilder();
        answer.append("Based on the retrieved documents, here's what I found:\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            answer.append(String.format("%d. %s (Similarity: %.2f)\n", 
                    i + 1, doc.getContent(), doc.getScore()));
        }
        
        answer.append("\nNote: This is a mock response. Set OPENAI_API_KEY environment variable to use GPT for better answers.");
        
        return answer.toString();
    }

    /**
     * Get information about the RAG system status.
     */
    public String getSystemInfo() {
        long docCount = documentService.getDocumentCount();
        boolean llmAvailable = chatModel != null;
        
        return String.format(
                "RAG System Status:\n" +
                "- Documents in database: %d\n" +
                "- LLM available: %s\n" +
                "- Top-K retrieval: %d\n" +
                "- Similarity threshold: %.2f",
                docCount, llmAvailable ? "Yes" : "No", TOP_K_DOCUMENTS, SIMILARITY_THRESHOLD
        );
    }
}
