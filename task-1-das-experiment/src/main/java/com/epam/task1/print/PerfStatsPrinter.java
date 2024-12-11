package com.epam.task1.print;

import com.epam.task1.model.PerfMetric;
import com.epam.task1.dto.PerfMetricDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PerfStatsPrinter {

    public static void printPerfStats(Collection<String> tests, Collection<String> works, List<PerfMetricDto> perfMetrics) {

        String[] subCols = new String[]{"CPU time", "Exec Time", "Diff"};

        PrettyPrint.Table table = PrettyPrint.table().column("java", 15).column("work", 35);
        for (String test : tests) {
            table = table.column(test, subCols);
        }
        PrettyPrint.Printer printer = table.printer();

        LinkedHashMap<String, List<PerfMetricDto>> byJavaVersion = group(JAVA_VERSION, perfMetrics);
        for (String javaVersion : byJavaVersion.keySet()) {
            LinkedHashMap<String, List<PerfMetricDto>> byWork = group(WORK, byJavaVersion.get(javaVersion));
            for (String work : works) {
                if (!byWork.containsKey(work)) continue;
                String[] tokens = new String[tests.size() * subCols.length + 2];
                String[] ansiColors = new String[tokens.length];
                int i = 0;
                ansiColors[i] = PrettyPrint.DEFAULT;
                tokens[i++] = javaVersion;
                ansiColors[i] = PrettyPrint.DEFAULT;
                tokens[i++] = work;
                LinkedHashMap<String, List<PerfMetricDto>> byTestName = group(TEST_NAME, byWork.get(work));
                for (String test : tests) {
                    if (!byTestName.containsKey(test)) {
                        for (int j = 0; j < subCols.length; j++) {
                            tokens[i++] = "-";
                        }
                    } else {
                        LinkedHashMap<String, PerfMetricDto> byMetrics = map(METRIC, byTestName.get(test));
                        Number cpuTime = addNsTokens(byMetrics, PerfMetric.CPU_TIME, i++, ansiColors, tokens);
                        Number execTime = addNsTokens(byMetrics, PerfMetric.EXEC_TIME, i++, ansiColors, tokens);;
                        if (cpuTime != null && execTime != null) {
                            ansiColors[i] = ansiColors[i - 1];
                            tokens[i++] = String.format("%d ms", TimeUnit.NANOSECONDS.toMillis(execTime.longValue() - cpuTime.longValue()));
                        } else {
                            ansiColors[i] = PrettyPrint.DEFAULT;
                            tokens[i++] = "-";
                        }
                    }
                }
                printer.print(ansiColors, tokens);
            }
            printer.lineSeparator();
        }
        printer.close();
    }

    private static Number addNsTokens(LinkedHashMap<String, PerfMetricDto> byMetrics, String metric, int i, String[] ansiColors, String[] tokens) {
        if (byMetrics.containsKey(metric)) {
            PerfMetricDto dto = byMetrics.get(metric);
            Number time = dto.value;
            ansiColors[i] = dto.successful ? PrettyPrint.SUCCESS : PrettyPrint.FAIL;
            tokens[i] = String.format("%d ms", TimeUnit.NANOSECONDS.toMillis(time.longValue()));
            return time;
        } else {
            ansiColors[i] = PrettyPrint.DEFAULT;
            tokens[i] = "-";
            return null;
        }
    }


    private static <T> LinkedHashMap<T, List<PerfMetricDto>> group(FooFunc<T> func, List<PerfMetricDto> perfMetrics) {
        LinkedHashMap<T, List<PerfMetricDto>> grouped = new LinkedHashMap<T, List<PerfMetricDto>>();
        for (PerfMetricDto perfMetric : perfMetrics) {
            T key = func.apply(perfMetric);
            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<PerfMetricDto>());
            }
            grouped.get(key).add(perfMetric);
        }
        return grouped;
    }

    private static <T> LinkedHashMap<T, PerfMetricDto> map(FooFunc<T> func, List<PerfMetricDto> perfMetrics) {
        LinkedHashMap<T, PerfMetricDto> mapped = new LinkedHashMap<T, PerfMetricDto>();
        for (PerfMetricDto perfMetric : perfMetrics) {
            T key = func.apply(perfMetric);
            if (mapped.containsKey(key)) throw new IllegalArgumentException("multiple values for: " + key);
            mapped.put(key, perfMetric);
        }
        return mapped;
    }

    interface FooFunc<V> {
        V apply(PerfMetricDto o);
    }

    private static FooFunc<String> JAVA_VERSION = new FooFunc<String>() {
        @Override
        public String apply(PerfMetricDto o) {
            return o.javaVersion;
        }
    };

    private static FooFunc<String> WORK = new FooFunc<String>() {
        @Override
        public String apply(PerfMetricDto o) {
            return o.work;
        }
    };

    private static FooFunc<String> TEST_NAME = new FooFunc<String>() {
        @Override
        public String apply(PerfMetricDto o) {
            return o.test;
        }
    };

    private static FooFunc<String> METRIC = new FooFunc<String>() {
        @Override
        public String apply(PerfMetricDto o) {
            return o.metric;
        }
    };


}
