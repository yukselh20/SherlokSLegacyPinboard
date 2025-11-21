package ui.pinboard;

public class PinboardNote extends PinboardItem {
    private String text;
    private String colorHex;

    public PinboardNote(double x, double y, String text, String colorHex) {
        super(x, y);
        this.text = text;
        this.colorHex = colorHex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
