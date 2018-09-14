package edu.tamu.di.SAFCreator.model;

public class Problem {
    private Integer rownumber;
    private String columnLabel;
    private boolean error;
    private String note;
    private Flag flag;

    public Problem(boolean error, String note) {
        this.error = error;
        this.note = note;
    }

    public Problem(int rownumber, String columnLabel, boolean error, String note) {
        this.rownumber = rownumber;
        this.columnLabel = columnLabel;
        this.error = error;
        this.note = note;
    }

    public Problem(int rownumber, String columnLabel, boolean error, String note, Flag flag) {
        this.rownumber = rownumber;
        this.columnLabel = columnLabel;
        this.error = error;
        this.note = note;
        this.flag = flag;
    }

    public Flag getFlag() {
        return flag;
    }

    public boolean isError() {
        return error;
    }

    public boolean isFlagged() {
        return flag != null;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    @Override
    public String toString() {
        String flagged = "";

        if (flag != null) {
            flagged = "Flagged ";
        }

        if (rownumber == null) {
            return flagged + (error ? "ERROR" : "WARNING") + ": " + note;
        }

        return flagged + (error ? "ERROR at " : "WARNING at ") + "column " + columnLabel + " row " + rownumber + ":\n\t" + note;
    }
}
