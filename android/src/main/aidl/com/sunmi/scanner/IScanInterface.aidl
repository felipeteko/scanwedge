// IScanInterface.aidl
package com.sunmi.scanner;

import android.view.KeyEvent;

interface IScanInterface {
    /**
     * Trigger start/stop scanning
     * key.getAction()==KeyEvent.ACTION_UP -> trigger start scan
     * key.getAction()==KeyEvent.ACTION_DOWN -> trigger stop scan
     */
    void sendKeyEvent(in KeyEvent key);
    /**
     * Trigger start scan
     */
    void scan();
    /**
     * Trigger stop scan
     */
    void stop();
    /**
     * Get scanner model type
     * 100 --> NONE
     * 101 --> P2Lite
     * 102 --> l2-newland
     * 103 --> l2-zebra
     */
    int getScannerModel();
}
