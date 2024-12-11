package com.epam.task1.dto;

public class PerfMetricDto {

    public String javaVersion;
    public String work;
    public String test;
    public String metric;
    public Number value;
    public boolean successful;

    public PerfMetricDto(String javaVersion, String work, String test, String metric, Number value, boolean successful) {
        this.javaVersion = javaVersion;
        this.work = work;
        this.test = test;
        this.metric = metric;
        this.value = value;
        this.successful = successful;
    }

    public Object[] toTokens() {
        return new Object[]{javaVersion, work, test, metric, value, successful};
    }

    public static PerfMetricDto fromTokens(Object[] tokens) {
        String value = (String) tokens[4];
        final Number v;
        if (value.contains(".")) {
            v = Double.valueOf(value);
        } else {
            v = Long.valueOf(value);
        }
        return new PerfMetricDto(
                (String) tokens[0],
                (String) tokens[1],
                (String) tokens[2],
                (String) tokens[3],
                v,
                Boolean.parseBoolean((String) tokens[5])
        );
    }


}
