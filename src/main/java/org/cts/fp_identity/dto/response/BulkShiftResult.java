package org.cts.fp_identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkShiftResult {
    private int totalRows;
    private int created;
    private int skipped;
    private int failed;
    private List<ShiftResponse> createdShifts;
    private List<SkippedRow>    skippedRows;
    private List<FailedRow>     failedRows;

    @Data
    @Builder
    public static class SkippedRow {
        private int    row;
        private String name;
        private String reason;

        public SkippedRow(int row, String name, String reason) {
            this.row    = row;
            this.name   = name;
            this.reason = reason;
        }
    }

    @Data
    @Builder
    public static class FailedRow {
        private int    row;
        private String name;
        private String reason;

        public FailedRow(int row, String name, String reason) {
            this.row    = row;
            this.name   = name;
            this.reason = reason;
        }
    }
}
