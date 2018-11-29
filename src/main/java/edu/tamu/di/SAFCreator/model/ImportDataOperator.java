package edu.tamu.di.SAFCreator.model;

import javax.swing.JTextArea;

public interface ImportDataOperator {
    public class Updates {
        private int processed;
        private int total;

        public Updates() {
            processed = 0;
            total = 0;
        }

        public Updates(int processed, int total) {
            this.processed = processed;
            this.total = total;
        }

        public int getProcessed() {
            return processed;
        }

        public int getTotal() {
            return total;
        }

        public void setProcessed(int processed) {
            this.processed = processed;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    public Batch getBatch();

    public JTextArea getConsole();

    public FlagPanel getFlags();

    public void setBatch(Batch batch);

    public void setConsole(JTextArea console);

    public void setFlags(FlagPanel flags);
}
