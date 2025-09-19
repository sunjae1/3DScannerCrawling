package org.example.printer3d;

import org.example.printer3d.model.DentalInfo;
import org.example.printer3d.model.Detection3DResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvFileProcessor {

    private static final String INPUT_ENCODING = "EUC-KR";  // 🔥 EUC-KR 고정

    // 원본 CSV 데이터 저장용
    private List<String[]> originalCsvData = new ArrayList<>();
    private String originalHeader = "";

    /**
     * CSV 파일에서 치과 정보를 로드합니다.
     */
    public List<DentalInfo> loadDentalInfoFromCsv(String csvPath) throws Exception {
        List<DentalInfo> dentals = new ArrayList<>();
        originalCsvData.clear();

        System.out.println("✅ 사용 인코딩: " + INPUT_ENCODING);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvPath), INPUT_ENCODING))) {

            String line = br.readLine(); // 헤더 읽기
            if (line != null && line.startsWith("\ufeff")) {
                line = line.substring(1); // BOM 제거
            }

            originalHeader = line;
            System.out.println("📋 원본 헤더: " + line);

            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                originalCsvData.add(parts); // 원본 데이터 저장

                if (parts.length >= 3) {
                    String company = parts[0].trim();
                    String website = parts[1].trim();
                    String email = parts[2].trim();

                    // 이메일이 있거나 웹사이트가 있는 치과만 처리 대상에 포함
                    if ((!email.equals("X") && !email.isEmpty()) || (!website.isEmpty())) {
                        dentals.add(new DentalInfo(company, website, email));
                        System.out.printf("✅ 로드: %s\n", company);
                    }
                }
            }
        }

        System.out.printf("📊 총 %d개 치과 정보 로드 완료 (원본 %d줄 보존)\n", dentals.size(), originalCsvData.size());
        return dentals;
    }

    /**
     * 원본 CSV를 유지하면서 3D 스캐너 검출 결과를 추가 컬럼으로 저장합니다.
     */
    public void save3DResultsToCsv(List<Detection3DResult> results, String outputPath) throws Exception {
        // 결과를 치과명으로 매핑
        Map<String, Detection3DResult> resultMap = new HashMap<>();
        for (Detection3DResult result : results) {
            resultMap.put(result.getDentalName().trim(), result);
        }

        try (FileWriter writer = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write('\ufeff'); // UTF-8 BOM 추가 (Excel 호환)

            // 확장된 헤더 작성 (원본 + 3D 스캐너 검출 컬럼들)
            writer.write(originalHeader + ",3D스캐너보유,신뢰도,점수,증거,처리상태,오류메시지\n");

            // 원본 데이터와 검출 결과를 매칭하여 저장
            for (String[] originalRow : originalCsvData) {
                // 원본 데이터 먼저 작성
                for (int j = 0; j < originalRow.length; j++) {
                    if (j > 0) writer.write(",");
                    writer.write(escapeCsv(originalRow[j]));
                }

                // 3D 스캐너 검출 결과 추가
                if (originalRow.length > 0) {
                    String dentalName = originalRow[0].trim();
                    Detection3DResult result = resultMap.get(dentalName);

                    if (result != null) {
                        // 검출 결과가 있는 경우
                        writer.write(String.format(",%s,%s,%d,\"%s\",%s,\"%s\"",
                                result.isHas3DPrinter() ? "예" : "아니오",
                                result.getConfidenceLevel(),
                                result.getScore(),
                                result.getEvidence().replace("\"", "\"\""),
                                getProcessStatus(result),
                                result.getErrorMessage().replace("\"", "\"\"")));
                    } else {
                        // 검출 결과가 없는 경우 (이메일/웹사이트 없어서 건너뛴 경우)
                        String website = originalRow.length > 1 ? originalRow[1].trim() : "";
                        String email = originalRow.length > 2 ? originalRow[2].trim() : "";
                        String skipReason = getSkipReason(website, email);

                        writer.write(String.format(",미검사,SKIP,0,\"%s\",건너뜀,\"\"", skipReason));
                    }
                }
                writer.write("\n");
            }
        }

        System.out.println("💾 확장된 결과 파일 저장 완료 (UTF-8 인코딩): " + outputPath);
        System.out.printf("📊 원본 %d줄 + 검출결과 %d개 = 총 %d줄 저장\n",
                originalCsvData.size(), results.size(), originalCsvData.size());
    }

    /**
     * 처리 상태를 반환합니다.
     */
    private String getProcessStatus(Detection3DResult result) {
        if ("ERROR".equals(result.getConfidenceLevel())) {
            return "오류";
        } else if (result.isHas3DPrinter()) {
            return "발견";
        } else {
            return "미발견";
        }
    }

    /**
     * 건너뛴 이유를 반환합니다.
     */
    private String getSkipReason(String website, String email) {
        boolean hasWebsite = website != null && !website.isEmpty() && !website.equals("X");
        boolean hasEmail = email != null && !email.isEmpty() && !email.equals("X");

        if (!hasWebsite && !hasEmail) {
            return "웹사이트, 이메일 정보 없음";
        } else if (!hasWebsite) {
            return "웹사이트 정보 없음";
        } else if (!hasEmail) {
            return "이메일 정보 없음 (웹사이트만 있음)";
        } else {
            return "기타 사유로 건너뜀";
        }
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

