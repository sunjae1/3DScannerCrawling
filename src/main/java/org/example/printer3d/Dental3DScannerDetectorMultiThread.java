package org.example.printer3d;

import org.example.printer3d.model.DentalInfo;
import org.example.printer3d.model.Detection3DResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Dental3DScannerDetectorMultiThread {
    // 3D 스캐너 관련 키워드들
    private static final String[] SCANNER_3D_KEYWORDS = {
            "3d스캐너", "3d 스캐너", "3d scanning", "3d스캐닝", "3d 스캐닝",
            "쓰리디스캐너", "쓰리디 스캐너", "삼차원 스캐너",
            "구강스캐너", "intraoral scanner", "인트라오럴 스캐너",
            "광학스캐너", "optical scanner", "디지털인상", "digital impression",
            // 브랜드명
            "itero", "trios", "cerec", "carestream", "cs3600", "cs3700",
            "medit", "i500", "i700", "primescan", "sirona",
            "planmeca", "emerald", "3shape", "shining3d", "aoralscan",
            "dentapix", "launca", "virtuo vivo",
            // 기술 방식
            "confocal", "컨포컬", "structured light", "구조광",
            "triangulation", "삼각측량", "stereo camera", "스테레오카메라",
            // 치과 전용
            "dental scanner", "덴탈 스캐너", "치과용 스캐너", "치과 3d스캐너",
            "구강내 스캐너", "인상채득", "impression", "석고모형", "plaster model"
    };

    // 디지털 치과 키워드
    private static final String[] DIGITAL_KEYWORDS = {
            "디지털치과", "디지털 치과", "digital dentistry",
            "스마트치과", "첨단장비", "최신장비", "하이테크",
            "디지털임플란트", "원데이", "당일", "즉시", "빠른진료",
            "무인상", "인상없이", "편안한치료", "정밀진단",
            "cad/cam", "캐드캠", "cadcam", "워크플로우"
    };

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT_MS = 15000;
    private static final int THREAD_POOL_SIZE = 5;
    private static final int DELAY_BETWEEN_REQUESTS_MS = 300; // 스레드당 요청 간격

    // 스레드 안전한 카운터
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);

    /**
     * 모든 치과의 3D 스캐너 보유 여부를 멀티스레드로 검사합니다.
     */
    public List<Detection3DResult> scanAllDentalsFor3D(List<DentalInfo> dentalList) {
        totalCount.set(dentalList.size());
        processedCount.set(0);

        System.out.println("🔍 3D 스캐너 검사 시작 (멀티스레드: " + THREAD_POOL_SIZE + "개)...\n");

        // 스레드풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // 결과 저장용 ConcurrentHashMap 대신 인덱스 기반 접근을 위해 배열 사용
        Detection3DResult[] resultsArray = new Detection3DResult[dentalList.size()];

        // CompletableFuture 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < dentalList.size(); i++) {
            final int index = i;
            final DentalInfo dental = dentalList.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 스레드별 요청 간격 조절
                    Thread.sleep(DELAY_BETWEEN_REQUESTS_MS * (index % THREAD_POOL_SIZE));

                    Detection3DResult result = detectSingle3DScanner(dental);
                    resultsArray[index] = result;

                    // 진행상황 출력 (스레드 안전)
                    int currentProgress = processedCount.incrementAndGet();
                    printProgress(currentProgress, totalCount.get(), dental.getName(), result);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("❌ 스레드 중단: " + dental.getName());
                } catch (Exception e) {
                    System.err.println("❌ 처리 오류 [" + dental.getName() + "]: " + e.getMessage());
                    resultsArray[index] = createErrorResult(dental, e.getMessage());
                    processedCount.incrementAndGet();
                }
            }, executor);

            futures.add(future);
        }

        // 모든 작업 완료 대기
        try {
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            allTasks.get(5, TimeUnit.MINUTES); // 최대 5분 대기
        } catch (TimeoutException e) {
            System.err.println("⚠️ 일부 작업이 타임아웃되었습니다.");
        } catch (Exception e) {
            System.err.println("❌ 멀티스레드 처리 중 오류: " + e.getMessage());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 배열을 리스트로 변환
        List<Detection3DResult> results = new ArrayList<>();
        for (Detection3DResult result : resultsArray) {
            if (result != null) {
                results.add(result);
            }
        }

        // 최종 요약 출력
        printFinalSummary(results);

        return results;
    }

    /**
     * 개별 치과의 3D 스캐너 보유 여부를 검사합니다.
     */
    public Detection3DResult detectSingle3DScanner(DentalInfo dental) {
        Detection3DResult result = new Detection3DResult(dental.getName(), dental.getWebsite(), dental.getEmail());

        // 웹사이트 정보가 없는 경우
        if (dental.getWebsite() == null || dental.getWebsite().trim().isEmpty()) {
            result.setHas3DPrinter(false); // 필드명은 그대로 유지
            result.setConfidenceLevel("NONE");
            result.setReason("웹사이트 정보 없음");
            return result;
        }

        try {
            // 웹사이트 크롤링
            Document doc = Jsoup.connect(dental.getWebsite())
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            String fullText = doc.text().toLowerCase();

            // 키워드 검색 및 점수 계산
            calculateDetectionScore(result, fullText);

        } catch (Exception e) {
            result.setHas3DPrinter(false);
            result.setConfidenceLevel("ERROR");
            result.setReason("크롤링 오류: " + e.getMessage());
        }

        return result;
    }

    /**
     * 키워드 기반으로 3D 스캐너 보유 점수를 계산합니다.
     */
    private void calculateDetectionScore(Detection3DResult result, String pageText) {
        List<String> found3D = findMatchingKeywords(pageText, SCANNER_3D_KEYWORDS);
        List<String> foundDigital = findMatchingKeywords(pageText, DIGITAL_KEYWORDS);

        int score = 0;
        StringBuilder evidence = new StringBuilder();

        // 3D 스캐너 직접 언급 (가장 중요)
        if (!found3D.isEmpty()) {
            score += found3D.size() * 15;
            evidence.append("📱 3D스캐너: ").append(String.join(", ", found3D)).append(" | ");
        }

        // 디지털 치과 키워드 (보조 지표)
        if (!foundDigital.isEmpty()) {
            score += foundDigital.size() * 5;
            evidence.append("💻 디지털: ").append(String.join(", ", foundDigital)).append(" | ");
        }

        // 최종 판정
        if (score >= 15) {
            result.setHas3DPrinter(true);
            if (score >= 40) {
                result.setConfidenceLevel("HIGH");
            } else if (score >= 25) {
                result.setConfidenceLevel("MEDIUM");
            } else {
                result.setConfidenceLevel("LOW");
            }
        } else {
            result.setHas3DPrinter(false);
            result.setConfidenceLevel("NONE");
        }

        result.setScore(score);
        result.setEvidence(evidence.toString());
        result.setReason(evidence.length() > 0 ? evidence.toString() : "3D 관련 정보 없음");
    }

    /**
     * 텍스트에서 키워드 매칭을 찾습니다.
     */
    private List<String> findMatchingKeywords(String text, String[] keywords) {
        List<String> found = new ArrayList<>();
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                found.add(keyword);
            }
        }
        return found;
    }

    /**
     * 오류 결과를 생성합니다.
     */
    private Detection3DResult createErrorResult(DentalInfo dental, String errorMessage) {
        Detection3DResult result = new Detection3DResult(dental.getName(), dental.getWebsite(), dental.getEmail());
        result.setHas3DPrinter(false);
        result.setConfidenceLevel("ERROR");
        result.setReason("처리 오류: " + errorMessage);
        return result;
    }

    /**
     * 진행상황을 출력합니다 (스레드 안전).
     */
    private synchronized void printProgress(int current, int total, String dentalName, Detection3DResult result) {
        System.out.printf("[%d/%d] 🔍 %s ", current, total, dentalName);

        if (result.isHas3DPrinter()) {
            System.out.printf("✅ 3D스캐너 발견! 신뢰도: %s (점수: %d)\n",
                    result.getConfidenceLevel(), result.getScore());
            if (!result.getEvidence().isEmpty()) {
                System.out.printf("   증거: %s\n", result.getEvidence());
            }
        } else {
            System.out.printf("❌ 3D스캐너 없음 (%s)\n", result.getReason());
        }
    }

    /**
     * 최종 요약을 출력합니다.
     */
    private void printFinalSummary(List<Detection3DResult> results) {
        long high = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
        long medium = results.stream().filter(r -> "MEDIUM".equals(r.getConfidenceLevel())).count();
        long low = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();
        long error = results.stream().filter(r -> "ERROR".equals(r.getConfidenceLevel())).count();
        long total3D = high + medium + low;

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🎉 3D 스캐너 검사 완료! (멀티스레드 " + THREAD_POOL_SIZE + "개)");
        System.out.printf("📊 전체 검사: %d개 치과\n", results.size());
        System.out.printf("📱 3D스캐너 보유 추정: %d개 (%.1f%%)\n", total3D, (double)total3D/results.size()*100);
        System.out.printf("   - 높은 신뢰도: %d개\n", high);
        System.out.printf("   - 중간 신뢰도: %d개\n", medium);
        System.out.printf("   - 낮은 신뢰도: %d개\n", low);
        if (error > 0) {
            System.out.printf("   - 처리 오류: %d개\n", error);
        }
        System.out.println("═".repeat(60));
    }
}
