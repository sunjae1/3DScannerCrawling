package org.example.printer3d;

import org.example.printer3d.model.DentalInfo;
import org.example.printer3d.model.Detection3DResult;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            printWelcomeMessage();

            // CSV 파일 경로 입력
            System.out.print("📁 CSV 파일 경로를 입력하세요: ");
            String csvPath = scanner.nextLine().trim();

            if (csvPath.isEmpty()) {
                System.out.println("❌ 파일 경로가 입력되지 않았습니다.");
                return;
            }

            // CSV 파일 처리기 생성
            CsvFileProcessor_Email csvProcessor = new CsvFileProcessor_Email();

            // 치과 정보 로드
            List<DentalInfo> dentalList = csvProcessor.loadDentalInfoFromCsv(csvPath);

            if (dentalList.isEmpty()) {
                System.out.println("❌ 유효한 치과 정보를 찾을 수 없습니다.");
                return;
            }

            System.out.printf("📊 총 %d개 치과 정보 로드 완료\n", dentalList.size());

            // 3D 스캐너 검출기 생성 및 실행
            Dental3DScannerDetectorDeepCrawling_Timer_Temp detector = new Dental3DScannerDetectorDeepCrawling_Timer_Temp();
            List<Detection3DResult> results = detector.scanAllDentalsFor3D(dentalList);

            // 결과 저장
            String outputPath = generateOutputPath(csvPath);
            csvProcessor.save3DResultsToCsv(results, outputPath);

            System.out.printf("\n💾 결과 저장 완료: %s\n", outputPath);

        } catch (Exception e) {
            System.err.println("❌ 프로그램 실행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void printWelcomeMessage() {
        System.out.println("🖨️ === 치과 3D 스캐너 검출기 ===");
        System.out.println("버전: 1.0.0");
        System.out.println("기능: CSV 파일의 치과 웹사이트에서 3D 스캐너 보유 여부 검사");
        System.out.println();
    }

    private static String generateOutputPath(String originalPath) {
        if (originalPath.toLowerCase().endsWith(".csv")) {
            return originalPath.substring(0, originalPath.length() - 4) + "_3d_results.csv";
        } else {
            return originalPath + "_3d_results.csv";
        }
    }
}

