package com.nikkil;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseEditor editor = new DatabaseEditor();
            editor.start();
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}