package org.freedesktop.networkmanager;

import java.util.Map;
import org.freedesktop.Pair;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.NetworkManager.Device")
public interface Device extends DBusInterface {
    public static class StateChanged extends DBusSignal {
        public final UInt32 new_state;
        public final UInt32 old_state;
        public final UInt32 reason;

        public StateChanged(String path, UInt32 new_state, UInt32 old_state, UInt32 reason)
                throws DBusException {
            super(path, new_state, old_state, reason);
            this.new_state = new_state;
            this.old_state = old_state;
            this.reason = reason;
        }
    }

    public void Reapply(
            Map<CharSequence, Map<CharSequence, Variant<?>>> connection, UInt64 version_id, UInt32 flags);

    public Pair<Map<CharSequence, Map<CharSequence, Variant<?>>>, UInt64> GetAppliedConnection(
            UInt32 flags);

    public void Disconnect();

    public void Delete();
}
