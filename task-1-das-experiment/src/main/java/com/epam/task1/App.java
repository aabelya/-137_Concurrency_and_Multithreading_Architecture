package com.epam.task1;

import com.epam.task1.dto.PerfMetricDto;
import com.epam.task1.impl.CustomSyncThreadSafeMap;
import com.epam.task1.model.PerfMetric;
import com.epam.task1.print.PerfStatsPrinter;
import com.epam.task1.work.IntegerAdder;
import com.epam.task1.work.SumCalculator;
import com.epam.task1.work.ThreadWork;
import com.epam.task1.work.WorkflowRunner;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class App {

    public static void main(String[] args) throws Exception {
        LinkedHashMap<String, List<ThreadWork>> perfStats = new LinkedHashMap<String, List<ThreadWork>>();

        System.out.println("============= Hash Map ================");
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        try {
            perfStats.put("Hash Map", WorkflowRunner.runWorkflow(hashMap));
        } finally {
            hashMap.clear();
        }
        System.out.println("=======================================");

        System.out.println("======== Concurrent Hash Map ==========");
        ConcurrentHashMap<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<Integer, Integer>();
        try {
            perfStats.put("Concurrent Hash Map", WorkflowRunner.runWorkflow(concurrentHashMap));
        } catch (Exception e) {
            hashMap.clear();
        }
        System.out.println("=======================================");

        System.out.println("========= Synchronized Map ============");
        Map<Integer, Integer> synchronizedMap = Collections.synchronizedMap(new HashMap<Integer, Integer>());
        try {
            perfStats.put("Synchronized Map", WorkflowRunner.runWorkflow(synchronizedMap));
        } catch (Exception e) {
            synchronizedMap.clear();
        }
        System.out.println("=======================================");

        System.out.println("==== Synchronized Map With A Fix ======");
        Map<Integer, Integer> synchronizedMap2 = Collections.synchronizedMap(new HashMap<Integer, Integer>());
        try {
            IntegerAdder adder = new IntegerAdder(synchronizedMap2);
            perfStats.put("Synchronized Map With A Fix",
                    WorkflowRunner.runWorkflow(adder, new SumCalculator(synchronizedMap2, adder.getValuesToAdd()) {
                        @Override
                        protected long sum(Collection<Integer> values) {
                            synchronized (this.targetMap) {
                                return super.sum(values);
                            }
                        }
                    }));
        } catch (Exception e) {
            synchronizedMap2.clear();
        }
        System.out.println("=======================================");

        System.out.println("===== Custom Sync Thread Safe Map =====");
        CustomSyncThreadSafeMap<Integer, Integer> customSyncThreadSafeMap = new CustomSyncThreadSafeMap<Integer, Integer>();
        try {
            perfStats.put("Custom Sync Thread Safe Map", WorkflowRunner.runWorkflow(customSyncThreadSafeMap));
        } catch (Exception e) {
            customSyncThreadSafeMap.clear();
        }

        System.out.println("=======================================\n\n");

        String javaVer = System.getProperty("java.version");
        LinkedHashSet<String> works = new LinkedHashSet<String>();
        List<PerfMetricDto> perfMetricDtos = new ArrayList<PerfMetricDto>();
        for (Map.Entry<String, List<ThreadWork>> stringListEntry : perfStats.entrySet()) {
            String testName = stringListEntry.getKey();
            List<ThreadWork> value = stringListEntry.getValue();
            for (ThreadWork threadWork : value) {
                String workName = threadWork.getName();
                works.add(workName);
                List<PerfMetric> perfMetrics = threadWork.getPerfMetrics();
                for (PerfMetric perfMetric : perfMetrics) {
                    perfMetricDtos.add(new PerfMetricDto(javaVer, workName, testName, perfMetric.name, perfMetric.value, threadWork.isSuccessful()));
                }
            }
        }
        final String fname = javaVer + ".csv";
        dumpToCsv(fname, perfMetricDtos);
        File[] files = new File(".").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return !fname.equals(name) && name.endsWith(".csv");
            }
        });
        for (File file : files) {
            perfMetricDtos.addAll(readFromCsv(file));
        }

        PerfStatsPrinter.printPerfStats(perfStats.keySet(), works, perfMetricDtos);
    }

    private static void dumpToCsv(String fname, List<PerfMetricDto> perfMetricDtos) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(fname, false));
            for (PerfMetricDto dto : perfMetricDtos) {
                pw.println(StringUtils.join(dto.toTokens(), ","));
            }
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private static List<PerfMetricDto> readFromCsv(File csvFile) throws IOException {
        ArrayList<PerfMetricDto> res = new ArrayList<PerfMetricDto>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(csvFile);
            while (scanner.hasNextLine()) {
                String[] tokens = scanner.nextLine().split(",");
                res.add(PerfMetricDto.fromTokens(tokens));
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return res;
    }


}
