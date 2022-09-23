package com.example.androidsdkdemoapp.data;

import java.io.Serializable;

public class PrinterObject implements Serializable {
    private String type;
    private int x;
    private int y;
    private String data;


    public PrinterObject(String type, int x, int y, String data) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

