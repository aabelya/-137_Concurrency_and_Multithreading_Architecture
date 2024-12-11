package com.epam.task1.work;

import com.epam.task1.model.PerfMetric;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public class ThreadWork implements Runnable, Serializable {

    private final List<PerfMetric> perfMetrics;
    private final String name;
    private final Runnable runnable;
    private boolean successful = false;

    public ThreadWork(String name, Runnable runnable) {
        this.name = name;
        this.runnable = runnable;
        perfMetrics = new ArrayList<PerfMetric>();
    }

    @Override
    public void run() {
        long t1 = System.nanoTime();
        try {
            if (runnable != null) runnable.run();
        } finally {
            long execTime = System.nanoTime() - t1;
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long cpuTime = threadMXBean.getCurrentThreadCpuTime();
            perfMetrics.add(PerfMetric.cpuTime(cpuTime));
            perfMetrics.add(PerfMetric.execTime(execTime));
        }
    }

    public String getName() {
        return name;
    }

    public List<PerfMetric> getPerfMetrics() {
        return perfMetrics;
    }

    public void setSuccessful() {
        this.successful = true;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
