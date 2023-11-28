package com.ble.demo;

import com.ble.ble.LeScanRecord;
import com.ble.ble.scan.LeScanResult;

public class LeDevice {
    private final String address;
    private String name;
    private String rxData = "No data";
    private LeScanRecord mRecord;
    private boolean oadSupported = false;

    public LeDevice(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public LeDevice(LeScanResult leScanResult) {
        this.name = leScanResult.getDevice().getName();
        this.address = leScanResult.getDevice().getAddress();
        this.mRecord = leScanResult.getLeScanRecord();
    }

    public boolean isOadSupported() {
        return oadSupported;
    }

    public void setOadSupported(boolean oadSupported) {
        this.oadSupported = oadSupported;
    }

    public LeScanRecord getLeScanRecord() {
        return mRecord;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getRxData() {
        return rxData;
    }

    public void setRxData(String rxData) {
        this.rxData = rxData;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LeDevice) {
            return ((LeDevice) o).getAddress().equals(address);
        }
        return false;
    }
}