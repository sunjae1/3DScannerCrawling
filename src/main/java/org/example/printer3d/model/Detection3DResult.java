package org.example.printer3d.model;

public class Detection3DResult {
    private String dentalName;
    private String website;
    private String email;
    private boolean has3DPrinter;
    private String confidenceLevel; // HIGH, MEDIUM, LOW, NONE, ERROR
    private int score;
    private String evidence;
    private String reason;

    public Detection3DResult(String dentalName, String website, String email) {
        this.dentalName = dentalName;
        this.website = website;
        this.email = email;
        this.has3DPrinter = false;
        this.confidenceLevel = "NONE";
        this.score = 0;
        this.evidence = "";
        this.reason = "";
    }

    // Getters
    public String getDentalName() { return dentalName; }
    public String getWebsite() { return website; }
    public String getEmail() { return email; }
    public boolean isHas3DPrinter() { return has3DPrinter; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public int getScore() { return score; }
    public String getEvidence() { return evidence; }
    public String getReason() { return reason; }

    // Setters
    public void setDentalName(String dentalName) { this.dentalName = dentalName; }
    public void setWebsite(String website) { this.website = website; }
    public void setEmail(String email) { this.email = email; }
    public void setHas3DPrinter(boolean has3DPrinter) { this.has3DPrinter = has3DPrinter; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public void setScore(int score) { this.score = score; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return String.format("Detection3DResult{dentalName='%s', has3DPrinter=%s, confidenceLevel='%s', score=%d}",
                dentalName, has3DPrinter, confidenceLevel, score);
    }
}
