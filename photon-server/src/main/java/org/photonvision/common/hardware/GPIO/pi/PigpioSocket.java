package org.photonvision.common.hardware.GPIO.pi;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class PigpioSocket {
    private static final Logger logger  = new Logger(PigpioSocket.class, LogGroup.General);
    private static final int PIGPIOD_MESSAGE_SIZE = 12;

    private PigpioSocketLock commandSocket;

    /**
     * Creates and starts a socket connection to a pigpio daemon on localhost
     */
    public PigpioSocket() {
        this("127.0.0.1", 8888);
    }

    /**
     * Creates and starts a socket connection to a pigpio daemon on a remote host with the specified address and port
     * @param addr Address of remote pigpio daemon
     * @param port Port of remote pigpio daemon
     */
    public PigpioSocket(String addr, int port) {
        try {
            commandSocket = new PigpioSocketLock(addr, port);
        } catch (IOException e) {
            logger.error("Failed to create or connect to Pigpio Daemon socket", e);
        }
    }

    /**
     * Reconnects to the pigpio daemon
     * @throws PigpioException on failure
     */
    public void reconnect() throws PigpioException {
        try {
            commandSocket.reconnect();
        } catch (IOException e) {
            logger.error("Failed to reconnect to Pigpio Daemon socket", e);
            throw new PigpioException("reconnect", e);
        }
    }

    /**
     * Terminates the connection to the pigpio daemon
     * @throws PigpioException on failure
     */
    public void gpioTerminate() throws PigpioException {
        try {
            commandSocket.terminate();
        } catch (IOException e) {
            logger.error("Failed to terminate connection to Pigpio Daemon socket", e);
            throw new PigpioException("gpioTerminate", e);
        }
    }

    /**
     * Read the GPIO level
     * @param pin Pin to read from
     * @return Value of the pin
     * @throws PigpioException on failure
     */
    public boolean gpioRead(int pin) throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_READ.value, pin);
            if (retCode < 0) throw new PigpioException(retCode);
            return retCode != 0;
        } catch (IOException e) {
            logger.error("Failed to read GPIO pin: " + pin, e);
            throw new PigpioException("gpioRead", e);
        }
    }

    /**
     * Write the GPIO level
     * @param pin Pin to write to
     * @param value Value to write
     * @throws PigpioException on failure
     */
    public void gpioWrite(int pin, boolean value) throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WRITE.value, pin, value ? 1 : 0);
            if (retCode < 0) throw new PigpioException(retCode);
        } catch (IOException e) {
            logger.error("Failed to write to GPIO pin: " + pin, e);
            throw new PigpioException("gpioWrite", e);
        }
    }

    /**
     * Clears all waveforms and any data added by calls to {@link #waveAddGeneric(ArrayList)}
     * @throws PigpioException on failure
     */
    public void waveClear() throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVCLR.value);
            if (retCode < 0) throw new PigpioException(retCode);
        } catch (IOException e) {
            logger.error("Failed to clear waveforms", e);
            throw new PigpioException("waveClear", e);
        }
    }

    /**
     * Adds a number of pulses to the current waveform
     * @param pulses ArrayList of pulses to add
     * @return the new total number of pulses in the current waveform
     * @throws PigpioException on failure
     */
    public int waveAddGeneric(ArrayList<PigpioPulse> pulses) throws PigpioException {
        // pigpio wave message format

        // I p1 0
        // I p2 0
        // I p3 pulses * 12
        // ## extension ##
        // III on/off/delay * pulses

        if (pulses == null || pulses.size() == 0) return 0;

        try {
            ByteBuffer bb = ByteBuffer.allocate(pulses.size()*12);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            for (var pulse : pulses) {
                bb.putInt(pulse.gpioOn).putInt(pulse.gpioOff).putInt(pulse.delayMicros);
            }

            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVAG.value,0,0,pulses.size() * PIGPIOD_MESSAGE_SIZE, bb.array());
            if (retCode < 0) throw new PigpioException(retCode);

            return retCode;
        } catch (IOException e) {
            logger.error("Failed to add pulse(s) to waveform", e);
            throw new PigpioException("waveAddGeneric", e);
        }
    }

    /**
     * Stops the transmission of the current waveform
     * @return success
     * @throws PigpioException on failure
     */
    public boolean waveTxStop() throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVHLT.value);
            if (retCode < 0) throw new PigpioException(retCode);
            return retCode == 0;
        } catch (IOException e) {
            logger.error("Failed to stop waveform", e);
            throw new PigpioException("waveTxStop", e);
        }
    }

    /**
     * Creates a waveform from the data provided by the prior calls to {@link #waveAddGeneric(ArrayList)}
     * Upon success a wave ID greater than or equal to 0 is returned
     * @return ID of the created waveform
     * @throws PigpioException on failure
     */
    public int waveCreate() throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVCRE.value);
            if (retCode < 0) throw new PigpioException(retCode);
            return retCode;
        } catch (IOException e) {
            logger.error("Failed to create new waveform", e);
            throw new PigpioException("waveCreate", e);
        }
    }

    /**
     * Deletes the waveform with specified wave ID
     * @param waveId ID of the waveform to delete
     * @throws PigpioException on failure
     */
    public void waveDelete(int waveId) throws PigpioException{
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVDEL.value, waveId);
            if (retCode < 0) throw new PigpioException(retCode);
        } catch (IOException e) {
            logger.error("Failed to delete wave: " + waveId, e);
            throw new PigpioException("waveDelete", e);
        }
    }

    /**
     * Transmits the waveform with specified wave ID. The waveform is sent once
     * @param waveId ID of the waveform to transmit
     * @return The number of DMA control blocks in the waveform
     * @throws PigpioException on failure
     */
    public int waveSendOnce(int waveId) throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVTX.value, waveId);
            if (retCode < 0) throw new PigpioException(retCode);
            return retCode;
        } catch (IOException e) {
            throw new PigpioException("waveSendOnce", e);
        }
    }

    /**
     * Transmits the waveform with specified wave ID. The waveform cycles until cancelled (either by the sending of a new waveform or {@link #waveTxStop()}
     * @param waveId ID of the waveform to transmit
     * @return The number of DMA control blocks in the waveform
     * @throws PigpioException on failure
     */
    public int waveSendRepeat(int waveId) throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_WVTXR.value, waveId);
            if (retCode < 0) throw new PigpioException(retCode);
            return retCode;
        } catch (IOException e) {
            throw new PigpioException("waveSendRepeat", e);
        }
    }

    /**
     * Starts hardware PWM on a GPIO at the specified frequency and dutycycle
     * @param pin GPIO pin to start PWM on
     * @param pwmFrequency Frequency to run at (1Hz-125MHz). Frequencies above 30MHz are unlikely to work
     * @param pwmDuty Duty cycle to run at (0-1,000,000)
     * @throws PigpioException on failure
     */
    public void hardwarePWM(int pin, int pwmFrequency, int pwmDuty) throws PigpioException {
        try {
            int retCode = commandSocket.sendCmd(PigpioCommand.PCMD_HP.value, pin, pwmFrequency, pwmDuty);
            if (retCode < 0) throw new PigpioException(retCode);
        } catch (IOException e) {
            throw new PigpioException("hardwarePWM", e);
        }
    }
}
