package org.example.printer3d;

import org.example.printer3d.model.DentalInfo;
import org.example.printer3d.model.Detection3DResult;
import org.example.printer3d.model.UrlWithDepth;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


//3D 스캐너 찾는 딥 크롤링_멀티 스레드
public class Dental3DScannerDetector_Full_SiteCrawling {
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
            "디지털임플란트", "무인상", "인상없이", "편안한치료",
            "정밀진단", "cad/cam", "캐드캠", "cadcam", "워크플로우"
    };

    // 우선순위 높은 페이지 키워드
    private static final String[] PRIORITY_PAGE_KEYWORDS = {
            "장비", "equipment", "시설", "facility", "진료", "treatment",
            "소개", "about", "clinic", "technology", "tech", "digital",
            "임플란트", "implant", "진단", "diagnosis", "첨단", "advanced"
    };

    // 제외할 페이지 키워드
    private static final String[] EXCLUDE_PAGE_KEYWORDS = {
            "contact", "연락처", "오시는길", "location", "map", "sitemap",
            "privacy", "개인정보", "terms", "약관", "login", "admin",
            "board", "게시판", "notice", "공지", "news", "뉴스"
    };

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT_MS = 10000;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int MAX_PAGES_PER_SITE = 25; // 사이트당 최대 25페이지
    private static final int DELAY_BETWEEN_PAGES_MS = 200; // 페이지간 0.2초 대기
    private static final int MAX_TIMEOUT_RETRIES = 3; // Read timeout 최대 3번까지 허용
    //수정
    private static final int MAX_DEPTH = 5;

    // 진행률 알림 간격 (밀리초)
    private static final long PROGRESS_REPORT_INTERVAL_MS = 5 * 60 * 1000; // 5분마다

    // 스레드 안전한 카운터
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);

    // 진행률 타이머용
    private volatile boolean isRunning = false;
    private long startTime;

    /**
     * 모든 치과의 3D 스캐너 보유 여부를 멀티스레드 딥 크롤링으로 검사합니다.
     */
    public List<Detection3DResult> scanAllDentalsFor3D(List<DentalInfo> dentalList) {
        startTime = System.currentTimeMillis(); // 시작 시간 기록
        totalCount.set(dentalList.size());
        processedCount.set(0);
        isRunning = true;

        System.out.println("🕷️ 3D 스캐너 딥 크롤링 시작 (멀티스레드: " + THREAD_POOL_SIZE + "개)...\n");

        // 진행률 타이머 시작
        ScheduledExecutorService progressTimer = Executors.newSingleThreadScheduledExecutor();
        progressTimer.scheduleAtFixedRate(this::reportProgress,
                PROGRESS_REPORT_INTERVAL_MS / 1000, // 첫 보고는 5분 후
                PROGRESS_REPORT_INTERVAL_MS / 1000, // 이후 5분마다
                TimeUnit.SECONDS);

        // 스레드풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // 결과 저장용 배열
        Detection3DResult[] resultsArray = new Detection3DResult[dentalList.size()];

        // CompletableFuture 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < dentalList.size(); i++) {
            final int index = i;
            final DentalInfo dental = dentalList.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Detection3DResult result = deepScanSite(dental);
                    resultsArray[index] = result;

                    // 진행상황 출력 (스레드 안전)
                    int currentProgress = processedCount.incrementAndGet();
                    printProgress(currentProgress, totalCount.get(), dental.getName(), result);

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
            allTasks.get(20, TimeUnit.MINUTES); // 딥 크롤링이므로 최대 20분 대기
        } catch (TimeoutException e) {
            System.err.println("⚠️ 일부 작업이 타임아웃되었습니다.");
        } catch (Exception e) {
            System.err.println("❌ 멀티스레드 처리 중 오류: " + e.getMessage());
        } finally {
            isRunning = false;
            progressTimer.shutdown(); // 진행률 타이머 종료

            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
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

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        // 최종 요약 출력
        printFinalSummary(results, totalDuration);

        return results;
    }

    /**
     * 주기적 진행률 보고
     */
    private void reportProgress() {
        if (!isRunning) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        int processed = processedCount.get();
        int total = totalCount.get();

        // 경과 시간 계산
        long elapsedHours = elapsedTime / (1000 * 60 * 60);
        long elapsedMinutes = (elapsedTime % (1000 * 60 * 60)) / (1000 * 60);

        // 진행률 계산
        double progressPercent = (double) processed / total * 100;

        // 예상 완료 시간 계산
        if (processed > 0) {
            long avgTimePerItem = elapsedTime / processed;
            long remainingItems = total - processed;
            long estimatedRemainingTime = avgTimePerItem * remainingItems;

            long remainingHours = estimatedRemainingTime / (1000 * 60 * 60);
            long remainingMinutes = (estimatedRemainingTime % (1000 * 60 * 60)) / (1000 * 60);

            System.out.println("\n" + "=".repeat(60));
            System.out.printf("📊 [진행률 보고] %d시간 %d분 경과\n", elapsedHours, elapsedMinutes);
            System.out.printf("✅ 진행: %d/%d (%.1f%%) 완료\n", processed, total, progressPercent);
            System.out.printf("⏱️ 예상 완료까지: %d시간 %d분 남음\n", remainingHours, remainingMinutes);
            System.out.printf("⚡ 현재 처리속도: %.1f개/분\n", (double) processed / (elapsedTime / 60000.0));
            System.out.println("=".repeat(60) + "\n");
        }
    }

    /**
     * 개별 사이트 딥 크롤링
     */
    private Detection3DResult deepScanSite(DentalInfo dental) {
        Detection3DResult result = new Detection3DResult(dental.getName(), dental.getWebsite(), dental.getEmail());

        if (dental.getWebsite() == null || dental.getWebsite().trim().isEmpty()) {
            result.setHas3DPrinter(false);
            result.setConfidenceLevel("NONE");
            result.setReason("웹사이트 정보 없음");
            result.setErrorMessage("");
            return result;
        }

        Set<String> visitedPages = ConcurrentHashMap.newKeySet();
        Queue<UrlWithDepth> pagesToVisit = new LinkedList<>();
        StringBuilder allText = new StringBuilder();
        List<String> foundEvidence = new ArrayList<>();

        try {

            String baseUrl = dental.getWebsite().trim();
            pagesToVisit.offer(new UrlWithDepth(baseUrl, 0));


            int pageCount = 0;
            int timeoutCount = 0; // Read timeout 카운터 추가
            while (!pagesToVisit.isEmpty()) {

//                String currentUrl = pagesToVisit.poll();
                UrlWithDepth urlWithDepth = pagesToVisit.poll();
                String currentUrl = urlWithDepth.getUrl();
                int currentDepth = urlWithDepth.getDepth();

                if (visitedPages.contains(currentUrl)) {
                    continue;
                }

                visitedPages.add(currentUrl);
                pageCount++;

                try {
                    Document doc = Jsoup.connect(currentUrl)
                            .userAgent(USER_AGENT)
                            .timeout(TIMEOUT_MS)
                            .followRedirects(true)
                            .get();

                    // 페이지 텍스트 수집
                    String pageText = doc.text().toLowerCase();
                    allText.append(pageText).append(" ");

                    // 키워드 검사
                    List<String> foundKeywords = findMatchingKeywords(pageText, SCANNER_3D_KEYWORDS);
                    if (!foundKeywords.isEmpty()) {
                        foundEvidence.add(String.format("페이지[%s]: %s",
                                getPageTitle(doc), String.join(", ", foundKeywords)));
                    }

                    // 심층 크롤링.
                        collectInternalLinks(doc, baseUrl, pagesToVisit, visitedPages, currentDepth);

                    // 페이지간 딜레이
                    Thread.sleep(DELAY_BETWEEN_PAGES_MS);

                } catch (Exception e) {

                    // 오류 상세 출력
                    System.err.printf("   [DEBUG] 페이지 오류 [%s]: %s\n", currentUrl, e.getMessage());

                    // Read timeout 체크
                    if (e.getMessage() != null && e.getMessage().contains("Read timed out")) {
                        timeoutCount++;
                        if (timeoutCount >= MAX_TIMEOUT_RETRIES) {
                            System.err.printf("   [ERROR] Read timeout %d회 초과, 해당 치과 처리 중단\n", MAX_TIMEOUT_RETRIES);
                            throw new RuntimeException("연속 Read timeout 초과: " + timeoutCount + "회", e);
                        }
                    }

                    // 첫 번째 페이지(메인 페이지) 오류는 전체 실패로 처리
                    if (pageCount == 1) {
                        throw new RuntimeException("메인 페이지 접근 실패: " + e.getMessage(), e);
                    }

                    continue; // 서브 페이지 오류만 무시
                }
            }

            // 최종 점수 계산
            calculateDeepScanScore(result, allText.toString(), foundEvidence, pageCount);

        } catch (Exception e) {
            result.setHas3DPrinter(false);
            result.setConfidenceLevel("ERROR");
            result.setReason("딥 크롤링 오류: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 내부 링크 수집 (우선순위 기반)
     */
    private void collectInternalLinks(Document doc, String baseUrl, Queue<UrlWithDepth> pagesToVisit, Set<String> visitedPages, int currentDepth) {
        try {

            if (currentDepth >= MAX_DEPTH) return; //깊이제한.

            URL base = new URL(baseUrl);
            String baseDomain = base.getHost();

            Elements links = doc.select("a[href]");
            Map<String, Integer> linkPriorities = new HashMap<>();

            for (Element link : links) {
                String href = link.attr("abs:href");
                String linkText = link.text().toLowerCase();


                //이미지/문서 파일 제외.
                if(linkText.endsWith(".jpg") || linkText.endsWith(".jpeg") ||
                linkText.endsWith(".png") || linkText.endsWith(".gif") ||
                linkText.endsWith(".pdf") || linkText.endsWith(".doc") || linkText.endsWith(".zip"))
                {
                    continue;
                }




                if (href.isEmpty() || visitedPages.contains(href)) {
                    continue;
                }

                // 같은 도메인인지 확인
                try {
                    URL linkUrl = new URL(href);
                    if (!baseDomain.equals(linkUrl.getHost())) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }

                // 우선순위 계산
                int priority = calculateLinkPriority(href, linkText);
                if (priority > 0) {
                    linkPriorities.put(href, priority);
                }
            }

            // 우선순위 순으로 정렬하여 큐에 추가
            linkPriorities.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(MAX_PAGES_PER_SITE - 1) // 메인페이지 제외
                    .forEach(entry -> pagesToVisit.offer(new UrlWithDepth(entry.getKey(), currentDepth +1)));

        } catch (Exception e) {
            // 링크 수집 오류는 무시
        }
    }

    /**
     * 링크 우선순위 계산
     */
    private int calculateLinkPriority(String url, String linkText) {
        int priority = 0;
        String urlLower = url.toLowerCase();
        String textLower = linkText.toLowerCase();

        // 제외할 페이지 체크
        for (String exclude : EXCLUDE_PAGE_KEYWORDS) {
            if (urlLower.contains(exclude) || textLower.contains(exclude)) {
                return 0; // 제외
            }
        }

        // 우선순위 페이지 체크
        for (String keyword : PRIORITY_PAGE_KEYWORDS) {
            if (urlLower.contains(keyword)) priority += 10;
            if (textLower.contains(keyword)) priority += 15;
        }

        // 기본 점수
        if (priority == 0) priority = 1;

        return priority;
    }

    /**
     * 딥 스캔 점수 계산
     */
    private void calculateDeepScanScore(Detection3DResult result, String allText,
                                        List<String> evidenceList, int pageCount) {
        List<String> foundKeywords = findMatchingKeywords(allText, SCANNER_3D_KEYWORDS);
        List<String> foundDigital = findMatchingKeywords(allText, DIGITAL_KEYWORDS);

        int score = 0;
        StringBuilder evidence = new StringBuilder();

        // 3D 스캐너 키워드
        if (!foundKeywords.isEmpty()) {
            score += foundKeywords.size() * 12; // 딥 크롤링에서는 점수 조정
            evidence.append("📱 3D스캐너: ").append(String.join(", ", foundKeywords)).append(" | ");
        }

        // 디지털 치과 키워드
        if (!foundDigital.isEmpty()) {
            score += foundDigital.size() * 4;
            evidence.append("💻 디지털: ").append(String.join(", ", foundDigital)).append(" | ");
        }

        // 페이지 다양성 보너스
        if (evidenceList.size() > 1) {
            score += evidenceList.size() * 3;
        }

        evidence.append("📄 검사 페이지: ").append(pageCount).append("개");

        // 신뢰도 판정 (딥 크롤링은 더 엄격하게)
        if (score >= 20) {
            result.setHas3DPrinter(true);
            if (score >= 50) {
                result.setConfidenceLevel("HIGH");
            } else if (score >= 35) {
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
        result.setReason(evidence.length() > 0 ? evidence.toString() :
                String.format("3D 관련 정보 없음 (%d페이지 검사)", pageCount));
        result.setErrorMessage("");
    }

    /**
     * 키워드 매칭 찾기
     */
    private List<String> findMatchingKeywords(String text, String[] keywords) {
        Set<String> found = new HashSet<>(); // 중복 제거
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                found.add(keyword);
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * 페이지 제목 추출
     */
    private String getPageTitle(Document doc) {
        String title = doc.title();
        return title.length() > 20 ? title.substring(0, 20) + "..." : title;
    }

    /**
     * 오류 결과를 생성합니다.
     */
    private Detection3DResult createErrorResult(DentalInfo dental, String errorMessage) {
        Detection3DResult result = new Detection3DResult(dental.getName(), dental.getWebsite(), dental.getEmail());
        result.setHas3DPrinter(false);
        result.setConfidenceLevel("ERROR");
        result.setReason("딥 크롤링 오류: " + errorMessage);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 진행상황을 출력합니다 (스레드 안전).
     */
    private synchronized void printProgress(int current, int total, String dentalName, Detection3DResult result) {
        System.out.printf("[%d/%d] 🕷️ %s ", current, total, dentalName);

        if (result.isHas3DPrinter()) {
            System.out.printf("✅ 3D스캐너 발견! 신뢰도: %s (점수: %d)\n",
                    result.getConfidenceLevel(), result.getScore());
        } else {
            if ("ERROR".equals(result.getConfidenceLevel())) {
                System.out.printf("❌ 크롤링 오류\n", result.getErrorMessage());
            } else {
                System.out.printf("❌ 3D스캐너 없음\n");
            }
        }
    }

    /**
     * 최종 요약을 출력합니다.
     */
    private void printFinalSummary(List<Detection3DResult> results, long totalDurationMs) {
        long high = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
        long medium = results.stream().filter(r -> "MEDIUM".equals(r.getConfidenceLevel())).count();
        long low = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();
        long error = results.stream().filter(r -> "ERROR".equals(r.getConfidenceLevel())).count();
        long total3D = high + medium + low;

        long hours = totalDurationMs / 3600000;
        long minutes = totalDurationMs % 3600000 / 60000;
        long seconds = totalDurationMs % 60000 / 1000;

        System.out.println("\n" + "═".repeat(60));
        System.out.println("🎉 3D 스캐너 딥 크롤링 완료! (멀티스레드 " + THREAD_POOL_SIZE + "개)");
        System.out.printf("📊 전체 검사: %d개 치과\n", results.size());
        System.out.printf("📱 3D스캐너 보유 추정: %d개 (%.1f%%)\n", total3D, (double)total3D/results.size()*100);
        System.out.printf("   - 높은 신뢰도: %d개\n", high);
        System.out.printf("   - 중간 신뢰도: %d개\n", medium);
        System.out.printf("   - 낮은 신뢰도: %d개\n", low);
        if (error > 0) {
            System.out.printf("   - 처리 오류: %d개\n", error);
        }
        System.out.printf("⏱️ 총 소요시간: %d시간 %d분 %d초\n", hours, minutes, seconds);
        System.out.printf("⚡ 평균 처리속도: %.1f개/분\n", (double)results.size() / (totalDurationMs / 60000.0));
        System.out.println("═".repeat(60));
    }

}
