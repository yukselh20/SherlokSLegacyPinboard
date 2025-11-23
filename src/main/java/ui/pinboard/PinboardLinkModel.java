package ui.pinboard;

import java.io.Serializable;

public class PinboardLinkModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String startItemId;
    private String endItemId;

    public PinboardLinkModel() {
    }

    public PinboardLinkModel(String startItemId, String endItemId) {
        this.startItemId = startItemId;
        this.endItemId = endItemId;
    }

    public String getStartItemId() {
        return startItemId;
    }

    public void setStartItemId(String startItemId) {
        this.startItemId = startItemId;
    }

    public String getEndItemId() {
        return endItemId;
    }

    public void setEndItemId(String endItemId) {
        this.endItemId = endItemId;
    }
}
