package ui.pinboard;

public class PinboardConnection {
    private final PinboardItem startItem;
    private final PinboardItem endItem;

    public PinboardConnection(PinboardItem startItem, PinboardItem endItem) {
        this.startItem = startItem;
        this.endItem = endItem;
    }

    public PinboardItem getStartItem() {
        return startItem;
    }

    public PinboardItem getEndItem() {
        return endItem;
    }
}
