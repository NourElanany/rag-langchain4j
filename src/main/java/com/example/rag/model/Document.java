package com.example.rag.model;

/**
 * Represents a document with its content and similarity score.
 */
public class Document {
    private final String id;
    private final String content;
    private final float score;

    public Document(String id, String content, float score) {
        this.id = id;
        this.content = content;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("Document{id='%s', content='%s', score=%.3f}", 
                id, content.length() > 50 ? content.substring(0, 50) + "..." : content, score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return id != null ? id.equals(document.id) : document.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
