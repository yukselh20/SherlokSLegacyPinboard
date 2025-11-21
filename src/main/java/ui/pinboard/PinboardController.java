package ui.pinboard;

import javafx.collections.ListChangeListener;
import common.dto.JournalEntryDTO;
import java.util.List;

public class PinboardController {
    private final PinboardModel model;
    private final PinboardWindow window;

    public PinboardController() {
        this.model = new PinboardModel();
        this.window = new PinboardWindow(model);
    }

    public void show() {
        window.show();
        window.toFront();
    }

    public void addJournalEntry(JournalEntryDTO entry) {
        // Check if already exists (by matching wrapping logic, somewhat inefficient but safe)
        boolean exists = model.getItems().stream()
            .filter(item -> item instanceof PinboardClue)
            .map(item -> ((PinboardClue) item).getJournalEntry())
            .anyMatch(existingEntry -> existingEntry.equals(entry));

        if (!exists) {
            // Stagger position slightly so they don't stack perfectly on top
            int count = model.getItems().size();
            double x = 20 + (count % 5) * 30;
            double y = 20 + (count / 5) * 30;

            model.addItem(new PinboardClue(x, y, entry));
        }
    }

    public void clear() {
        model.clear();
    }

    /**
     * Called when a new game starts to reset the board.
     */
    public void reset() {
        model.clear();
    }
}
