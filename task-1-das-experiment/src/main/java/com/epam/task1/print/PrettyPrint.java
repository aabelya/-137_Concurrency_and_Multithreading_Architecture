package com.epam.task1.print;

import org.apache.commons.lang3.StringUtils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrettyPrint {

    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

    public static final String FAIL = RED;
    public static final String SUCCESS = GREEN;
    public static final String DEFAULT = "\u001B[0m";


    public static Table table() {
        return new Table();
    }

    public static class Printer {

        private final Table table;
        List<Column> columns = null;
        boolean headerPrinted = false;

        private Printer(Table table) {
            this.table = table;
        }

        public Printer print(String... tokens) {
            String[] colors = new String[tokens.length];
            Arrays.fill(colors, DEFAULT);
            return print(colors, tokens);
        }

        public Printer print(String[] ansiColors, String... tokens) {
            ensureHeaderPrinted();
            if (columns.size() != tokens.length) {
                throw new IllegalArgumentException(
                        String.format("Wrong number of tokens: expected %d, got %d", columns.size(), tokens.length));
            }
            if (ansiColors.length != tokens.length) {
                throw new IllegalArgumentException(
                        String.format("Colors and tokens lengths do not match: colors %d, tokens %d", ansiColors.length, tokens.length));
            }


            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                int colWidth = columns.get(i).getColWidth();
                String s;
                if (token.matches("^[0-9+-].*")) {
                    s = StringUtils.leftPad(token, colWidth);
                } else {
                    s = StringUtils.rightPad(token, colWidth);
                }
                System.out.printf("|%s%s%s", ansiColors[i], s, DEFAULT);
            }
            System.out.println("|");
            return this;
        }

        public Printer lineSeparator() {
            ensureHeaderPrinted();
            printLineSep("-", this.columns, false);
            return this;
        }

        public void close() {
            lineSeparator();
            this.columns = null;
            this.headerPrinted = false;
        }

        private void ensureHeaderPrinted() {
            if (!headerPrinted) {
                printHeader();
                headerPrinted = true;
            }
        }

        private void printHeader() {
            List<Column> columns = table.columns;
            printHeaderLines("=", columns);
        }

        private void printHeaderLines(String topSep, List<Column> columns) {
            printLineSep(topSep, columns, true);
            for (Column c : columns) {
                int w = c.getColWidth();
                System.out.print("|" + StringUtils.center(c.s, w));
            }
            System.out.println("|");
            List<Column> nextLevelColumns = getNextLevelColumns(columns);
            if (nextLevelColumns.isEmpty()) {
                this.columns = columns;
                printLineSep("=", columns, false);
            } else {
                printHeaderLines("-", nextLevelColumns);
            }
        }

        private void printLineSep(String sep, List<Column> columns, boolean checkIfEmpty) {
            String csep = "+";
            for (Column c : columns) {
                final String lsep;
                if (checkIfEmpty && c.s.isEmpty()) {
                    lsep = " ";
                    csep = "|";
                } else {
                    lsep = sep;
                    csep = "+";
                }
                String s = new String(new char[c.getColWidth()]).replace("\0", lsep);
                System.out.print(csep + s);
            }
            System.out.println(csep);
        }

        private List<Column> getNextLevelColumns(List<Column> columns) {
            List<Column> subColumns = new ArrayList<Column>();
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
                if (!c.subColumns.isEmpty()) {
                    if (subColumns.isEmpty()) {
                        for (int j = 0; j < i; j++) {
                            subColumns.add(columns.get(j).emptyWidth());
                        }
                    }
                    subColumns.addAll(c.subColumns);
                } else if (!subColumns.isEmpty()) {
                    subColumns.add(c.emptyWidth());
                }
            }
            return subColumns;
        }
    }


    public static class Table {
        private List<Column> columns = new ArrayList<Column>();
        int columnWidth = 30;

        private Table() {
        }

        public Printer printer() {
            return new Printer(this);
        }

        public Table column(String s, int width) {
            Column column = new Column(s);
            column.setColWidth(width);
            columns.add(column);
            return this;
        }

        public Table columns(String... ss) {
            for (String s : ss) {
                columns.add(new Column(s));
            }
            return this;
        }

        public Table column(String s, String[] subCols) {
            Column[] cols = new Column[subCols.length];
            for (int i = 0; i < subCols.length; i++) {
                cols[i] = new Column(subCols[i]);
            }
            return column(s, cols);
        }

        public Table column(String s, Column[] subCols) {
            Column col = new Column(s);
            col.subColumns.addAll(Arrays.asList(subCols));
            columns.add(col);
            return this;
        }

        public Table columnWidth(int width) {
            this.columnWidth = width;
            return this;
        }

    }

    static class Column {

        private final String s;
        private final List<Column> subColumns = new ArrayList<Column>();
        private int colWidth = 10;

        private Column(String s) {
            this.s = s;
        }

        private Column emptyWidth() {
            Column c = new Column("");
            c.setColWidth(colWidth);
            return c;
        }

        private int getSize() {
            if (subColumns.isEmpty()) {
                return 1;
            }
            int size = 0;
            for (Column c : subColumns) {
                size += c.getSize();
            }
            return size;
        }

        private int getColWidth() {
            if (subColumns.isEmpty()) {
                return colWidth;
            }
            int width = subColumns.size() - 1;
            for (Column c : subColumns) {
                width += c.getColWidth();
            }
            return width;
        }

        public void setColWidth(int colWidth) {
            this.colWidth = colWidth;
        }
    }

}
