package org.freedesktop.networkmanager.device;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/** Auto-generated class. */
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.Wireless")
public interface Wireless extends DBusInterface {

    public List<DBusPath> GetAccessPoints();

    public List<DBusPath> GetAllAccessPoints();

    public void RequestScan(Map<String, Variant<?>> options);

    public static class PropertiesChanged extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        PropertiesChanged(String _path, String _interfaceName, Map<String, Variant<?>> _properties)
                throws DBusException {
            super(_path, _interfaceName);
            this.properties = _properties;
        }

        public Map<String, Variant<?>> getProperties() {
            return properties;
        }
    }

    public static class AccessPointAdded extends DBusSignal {

        private final DBusPath accessPoint;

        AccessPointAdded(String _path, String _interfaceName, DBusPath _accessPoint)
                throws DBusException {
            super(_path, _interfaceName);
            this.accessPoint = _accessPoint;
        }

        public DBusPath getAccessPoint() {
            return accessPoint;
        }
    }

    public static class AccessPointRemoved extends DBusSignal {

        private final DBusPath accessPoint;

        AccessPointRemoved(String _path, String _interfaceName, DBusPath _accessPoint)
                throws DBusException {
            super(_path, _interfaceName);
            this.accessPoint = _accessPoint;
        }

        public DBusPath getAccessPoint() {
            return accessPoint;
        }
    }
}
