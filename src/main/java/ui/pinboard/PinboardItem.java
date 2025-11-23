package ui.pinboard;

import java.util.UUID;

public abstract class PinboardItem {
    private final String id;
    private double x;
    private double y;
    private double width;
    private double height;

    public PinboardItem(double x, double y) {
        this.id = UUID.randomUUID().toString();
        this.x = x;
        this.y = y;
        this.width = 200; // Default width
        this.height = 150; // Default height
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
