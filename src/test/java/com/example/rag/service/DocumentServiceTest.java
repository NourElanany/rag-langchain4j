package com.example.rag.service;

import com.example.rag.model.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DocumentService.
 * Note: These tests require Milvus to be running.
 */
@Disabled("Integration tests - require Milvus to be running")
public class DocumentServiceTest {

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService();
        documentService.initialize();
    }

    @Test
    void testAddDocument() {
        // Given
        String id = "test_doc_1";
        String content = "This is a test document for unit testing.";

        // When
        assertDoesNotThrow(() -> documentService.addDocument(id, content));

        // Then
        assertTrue(documentService.getDocumentCount() > 0);
    }

    @Test
    void testSearchSimilarDocuments() {
        // Given
        String id = "test_doc_2";
        String content = "Java is a programming language used for building applications.";
        documentService.addDocument(id, content);

        // When
        List<Document> results = documentService.searchSimilarDocuments("programming language", 5);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getScore() > 0);
    }

    @Test
    void testGetDocumentCount() {
        // When
        long count = documentService.getDocumentCount();

        // Then
        assertTrue(count >= 0);
    }
}
