package com.epam.task1.model;

public class PerfMetric {

    public static final String CPU_TIME = "CPU time";
    public static final String EXEC_TIME = "Execution time";

    public final String name;
    public final Number value;

    public PerfMetric(String name, Number value) {
        this.name = name;
        this.value = value;
    }

    public static PerfMetric cpuTime(long value) {
        return new PerfMetric(CPU_TIME, value);
    }

    public static PerfMetric execTime(long value) {
        return new PerfMetric(EXEC_TIME, value);
    }
}
