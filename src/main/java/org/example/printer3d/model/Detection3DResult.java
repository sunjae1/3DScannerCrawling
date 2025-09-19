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
    private String errorMessage; // 🔥 오류 메시지 필드 추가

    public Detection3DResult(String dentalName, String website, String email) {
        this.dentalName = dentalName;
        this.website = website;
        this.email = email;
        this.has3DPrinter = false;
        this.confidenceLevel = "NONE";
        this.score = 0;
        this.evidence = "";
        this.reason = "";
        this.errorMessage = ""; // 기본값 빈 문자열
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
    public String getErrorMessage() { return errorMessage; } // 🔥 getter 추가

    // Setters
    public void setDentalName(String dentalName) { this.dentalName = dentalName; }
    public void setWebsite(String website) { this.website = website; }
    public void setEmail(String email) { this.email = email; }
    public void setHas3DPrinter(boolean has3DPrinter) { this.has3DPrinter = has3DPrinter; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public void setScore(int score) { this.score = score; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public void setReason(String reason) { this.reason = reason; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; } // 🔥 setter 추가

    @Override
    public String toString() {
        return String.format("Detection3DResult{dentalName='%s', has3DPrinter=%s, confidenceLevel='%s', score=%d, errorMessage='%s'}",
                dentalName, has3DPrinter, confidenceLevel, score, errorMessage);
    }
}
