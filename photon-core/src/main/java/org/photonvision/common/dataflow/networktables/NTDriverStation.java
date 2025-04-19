package org.photonvision.common.dataflow.networktables;

import java.util.EnumSet;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import edu.wpi.first.hal.ControlWord;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableEvent.Kind;

// Helper to print when the robot transitions modes
public class NTDriverStation {
    private static final Logger logger = new Logger(NTDriverStation.class, LogGroup.NetworkTables);

    // Copy since stuff was private
    public record NtControlWord(
            boolean m_enabled,
            boolean m_autonomous,
            boolean m_test,
            boolean m_emergencyStop,
            boolean m_fmsAttached,
            boolean m_dsAttached) {
        public NtControlWord() {
            this(false, false, false, false, false, false);
        }
    }

    private IntegerSubscriber ntControlWord;

    NtControlWord lastControlWord = new NtControlWord();

    public NTDriverStation(NetworkTableInstance inst) {
        NetworkTable fmsTable = inst.getTable("FMSInfo");
        this.ntControlWord = fmsTable.getIntegerTopic("FMSControlData").subscribe(0);

        fmsTable.addListener("FMSControlData",
                EnumSet.of(Kind.kValueAll),
                (table, key, event) -> {
                    if (event.is(Kind.kValueAll) && event.valueData.value.isInteger()) {
                        // Logger totally isnt thread safe but whatevs
                        var word = NTDriverStation.getControlWord(event.valueData.value.getInteger());

                        printTransition(this.lastControlWord, word);
                        this.lastControlWord = word;
                    }
                });

        // Slight data race here but whatever
        NtControlWord word = NTDriverStation.getControlWord(this.ntControlWord.get());

        printTransition(this.lastControlWord, word);
        this.lastControlWord = word;
    }

    private void printTransition(NtControlWord old, NtControlWord newWord) {
        logger.info("ROBOT TRANSITIONED MODES! From " + old.toString() + " to " + newWord.toString());
    }

    // Copied from
    // https://github.com/wpilibsuite/allwpilib/blob/07192285f65321a2f7363227a2216f09b715252d/hal/src/main/java/edu/wpi/first/hal/DriverStationJNI.java#L123C1-L140C4
    // TODO: upstream!
    /**
     * Gets the current control word of the driver station.
     *
     * <p>
     * The control work contains the robot state.
     *
     * @param controlWord the ControlWord to update
     * @return 
     * @see "HAL_GetControlWord"
     */
    private static NtControlWord getControlWord(long word) {
        return new NtControlWord(
                (word & 1) != 0,
                ((word >> 1) & 1) != 0,
                ((word >> 2) & 1) != 0,
                ((word >> 3) & 1) != 0,
                ((word >> 4) & 1) != 0,
                ((word >> 5) & 1) != 0);
    }
}
