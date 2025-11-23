package ui.pinboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PinboardModel {
    private final ObservableList<PinboardItem> items;
    private final ObservableList<PinboardConnection> connections;

    public PinboardModel() {
        this.items = FXCollections.observableArrayList();
        this.connections = FXCollections.observableArrayList();
    }

    public ObservableList<PinboardItem> getItems() {
        return items;
    }

    public ObservableList<PinboardConnection> getConnections() {
        return connections;
    }

    public void addItem(PinboardItem item) {
        if (!items.contains(item)) {
            items.add(item);
        }
    }

    public void removeItem(PinboardItem item) {
        items.remove(item);
        // Remove connections associated with this item
        connections.removeIf(c -> c.getStartItem() == item || c.getEndItem() == item);
    }

    public void addConnection(PinboardItem start, PinboardItem end) {
        if (start != end) {
            connections.add(new PinboardConnection(start, end));
        }
    }

    public void clear() {
        items.clear();
        connections.clear();
    }
}
