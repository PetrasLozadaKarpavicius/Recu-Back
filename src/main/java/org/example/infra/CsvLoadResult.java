package org.example.infra;

import java.util.ArrayList;
import java.util.List;

public class CsvLoadResult {
    private int totalLines;
    private int validLines;
    private int invalidLines;
    private List<String> invalidLineDetails;

    public CsvLoadResult() {
        this.invalidLineDetails = new ArrayList<>();
    }

    public void addInvalidLine(int lineNumber, String line, String reason) {
        this.invalidLines++;
        this.invalidLineDetails.add(String.format("Línea %d: %s (Razón: %s)", lineNumber, line, reason));
    }

    public void incrementValidLines() {
        this.validLines++;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    // Getters
    public int getTotalLines() { return totalLines; }
    public int getValidLines() { return validLines; }
    public int getInvalidLines() { return invalidLines; }
    public List<String> getInvalidLineDetails() { return invalidLineDetails; }

    @Override
    public String toString() {
        return String.format("Total líneas: %d, Válidas: %d, Inválidas: %d",
                totalLines, validLines, invalidLines);
    }
}