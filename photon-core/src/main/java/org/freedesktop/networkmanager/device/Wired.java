package org.freedesktop.networkmanager.device;

import java.util.Map;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.NetworkManager.Device.DeviceEthernet")
public interface Wired extends DBusInterface {
    public static class PropertiesChanged extends DBusSignal {
        public final Map<CharSequence, Variant<?>> properties;

        public PropertiesChanged(String path, Map<CharSequence, Variant<?>> properties)
                throws DBusException {
            super(path, properties);
            this.properties = properties;
        }
    }
}
