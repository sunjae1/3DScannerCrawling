package org.example.printer3d.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
//
    private String foundWebsite;


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



    @Override
    public String toString() {
        return String.format("Detection3DResult{dentalName='%s', has3DPrinter=%s, confidenceLevel='%s', score=%d, errorMessage='%s'}",
                dentalName, has3DPrinter, confidenceLevel, score, errorMessage);
    }
}
