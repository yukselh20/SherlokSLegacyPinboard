package ui.pinboard;

import common.dto.JournalEntryDTO;

public class PinboardClue extends PinboardItem {
    private final JournalEntryDTO journalEntry;

    public PinboardClue(double x, double y, JournalEntryDTO journalEntry) {
        super(x, y);
        this.journalEntry = journalEntry;
    }

    public JournalEntryDTO getJournalEntry() {
        return journalEntry;
    }

    // Helpers for the UI representation
    public String getTitle() {
        // Heuristic: First 20 chars or up to first newline
        String text = journalEntry.getText();
        int newlineIdx = text.indexOf('\n');
        if (newlineIdx > 0 && newlineIdx < 30) {
            return text.substring(0, newlineIdx);
        }
        if (text.length() > 30) {
            return text.substring(0, 27) + "...";
        }
        return text;
    }

    public String getSummary() {
        return journalEntry.getText();
    }
}
