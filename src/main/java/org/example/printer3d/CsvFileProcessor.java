package org.example.printer3d;

import org.example.printer3d.model.DentalInfo;
import org.example.printer3d.model.Detection3DResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvFileProcessor {

    private static final String INPUT_ENCODING = "EUC-KR";  // 🔥 EUC-KR 고정

    /**
     * CSV 파일에서 치과 정보를 로드합니다.
     */
    public List<DentalInfo> loadDentalInfoFromCsv(String csvPath) throws Exception {
        List<DentalInfo> dentals = new ArrayList<>();

        System.out.println("✅ 사용 인코딩: " + INPUT_ENCODING);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvPath), INPUT_ENCODING))) {

            String line = br.readLine(); // 헤더 읽기
            if (line != null && line.startsWith("\ufeff")) {
                line = line.substring(1); // BOM 제거
            }

            System.out.println("📋 헤더: " + line);

            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length >= 3) {
                    String company = parts[0].trim();
                    String website = parts[1].trim();
                    String email = parts[2].trim();

                    // 이메일이 있거나 웹사이트가 있는 치과만 포함
                    if ((!email.equals("X") && !email.isEmpty()) || (!website.isEmpty())) {
                        dentals.add(new DentalInfo(company, website, email));
                        System.out.printf("✅ 로드: %s\n", company);
                    }
                }
            }
        }

        System.out.printf("📊 총 %d개 치과 정보 로드 완료\n", dentals.size());
        return dentals;
    }

    /**
     * 3D 스캐너 검출 결과를 CSV 파일로 저장합니다.
     */
    public void save3DResultsToCsv(List<Detection3DResult> results, String outputPath) throws Exception {
        try (FileWriter writer = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write('\ufeff'); // UTF-8 BOM 추가 (Excel 호환)
            // 🔥 오류메시지 컬럼 추가
            writer.write("치과명,웹사이트,이메일,3D스캐너보유,신뢰도,점수,증거,오류메시지\n");

            for (Detection3DResult result : results) {
                writer.write(String.format("%s,%s,%s,%s,%s,%d,\"%s\",\"%s\"\n",
                        escapeCsv(result.getDentalName()),
                        escapeCsv(result.getWebsite()),
                        escapeCsv(result.getEmail()),
                        result.isHas3DPrinter() ? "예" : "아니오",
                        result.getConfidenceLevel(),
                        result.getScore(),
                        result.getEvidence().replace("\"", "\"\""),
                        result.getErrorMessage().replace("\"", "\"\""))); // 🔥 오류메시지 추가
            }
        }

        System.out.println("💾 결과 파일 저장 완료 (UTF-8 인코딩): " + outputPath);
    }

    /**
     * CSV 라인을 파싱합니다.
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        result.add(currentField.toString().trim());
        return result.toArray(new String[0]);
    }

    /**
     * CSV 필드를 이스케이프 처리합니다.
     */
    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}

