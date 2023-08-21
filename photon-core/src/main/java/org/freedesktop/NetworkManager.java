package org.freedesktop;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

// From https://github.com/hypfvieh/sandbox/
@DBusInterfaceName("org.freedesktop.NetworkManager")
public interface NetworkManager extends DBusInterface, Properties {
    public static class CheckPermissions extends DBusSignal {
        public CheckPermissions(String path) throws DBusException {
            super(path);
        }
    }

    public static class StateChanged extends DBusSignal {
        public final UInt32 state;

        public StateChanged(String path, UInt32 state) throws DBusException {
            super(path, state);
            this.state = state;
        }
    }

    public static class DeviceAdded extends DBusSignal {
        public final DBusInterface device_path;

        public DeviceAdded(String path, DBusInterface device_path) throws DBusException {
            super(path, device_path);
            this.device_path = device_path;
        }
    }

    public static class DeviceRemoved extends DBusSignal {
        public final DBusInterface device_path;

        public DeviceRemoved(String path, DBusInterface device_path) throws DBusException {
            super(path, device_path);
            this.device_path = device_path;
        }
    }

    public void Reload(UInt32 flags);

    public List<DBusPath> GetDevices();

    public List<DBusPath> GetAllDevices();

    public DBusInterface GetDeviceByIpIface(CharSequence iface);

    public DBusInterface ActivateConnection(
            DBusInterface connection, DBusInterface device, DBusInterface specific_object);

    public Pair<DBusInterface, DBusInterface> AddAndActivateConnection(
            Map<CharSequence, Map<CharSequence, Variant<?>>> connection,
            DBusInterface device,
            DBusInterface specific_object);

    public void DeactivateConnection(DBusInterface active_connection);

    public void Sleep(boolean sleep);

    public void Enable(boolean enable);

    public Map<CharSequence, CharSequence> GetPermissions();

    public void SetLogging(CharSequence level, CharSequence domains);

    public Pair<CharSequence, CharSequence> GetLogging();

    public UInt32 CheckConnectivity();

    public UInt32 state();

    public DBusInterface CheckpointCreate(
            List<DBusInterface> devices, UInt32 rollback_timeout, UInt32 flags);

    public void CheckpointDestroy(DBusInterface checkpoint);

    public Map<CharSequence, UInt32> CheckpointRollback(DBusInterface checkpoint);

    public void CheckpointAdjustRollbackTimeout(DBusInterface checkpoint, UInt32 add_timeout);

    // ==============================

    public static void main(String[] args) throws DBusException {

        try (DBusConnection dbusConn = DBusConnection.getConnection(DBusBusType.SYSTEM)) {

            NetworkManager nm =
                    dbusConn.getRemoteObject(
                            "org.freedesktop.NetworkManager",
                            "/org/freedesktop/NetworkManager",
                            NetworkManager.class);

            System.out.println("");
            System.out.println("Permissions");
            System.out.println("------------------");
            System.out.println("");

            Map<CharSequence, CharSequence> getPermissions = nm.GetPermissions();
            for (var entry : getPermissions.entrySet()) {
                System.out.println("Permission: " + entry.getKey() + " = " + entry.getValue());
            }

            System.out.println("");
            System.out.println("Device Information");
            System.out.println("------------------");
            System.out.println("");

            for (DBusPath path : nm.GetDevices()) {
                System.out.println("DevicePath: " + path);
                Properties deviceProps =
                        dbusConn.getRemoteObject(
                                "org.freedesktop.NetworkManager", path.toString(), Properties.class);

                DBusPath ipv4Path = deviceProps.Get("org.freedesktop.NetworkManager.Device", "Ip4Config");
                DBusPath ipv6Path = deviceProps.Get("org.freedesktop.NetworkManager.Device", "Ip6Config");
                System.out.println("ConfigPath: " + ipv4Path);

                System.out.println(
                        "  Interface: "
                                + deviceProps.Get("org.freedesktop.NetworkManager.Device", "Interface"));

                if (ipv4Path.toString().equals("/")) {
                    System.out.println("        IPv4: No IPv4 Config");
                } else {
                    Properties ipv4Config =
                            dbusConn.getRemoteObject(
                                    "org.freedesktop.NetworkManager", ipv4Path.toString(), Properties.class);

                    Map<String, Variant<?>> allIpV4 =
                            ipv4Config.GetAll("org.freedesktop.NetworkManager.IP4Config");
                    for (String key : allIpV4.keySet()) {
                        System.out.println("key: " + key);
                    }

                    List<Map<String, Variant<?>>> addressArrV4 =
                            ipv4Config.Get("org.freedesktop.NetworkManager.IP4Config", "AddressData");

                    // addressArr contains all IPs of the interface and additional properties (if any)
                    printAddressInfo(addressArrV4, "IPv4");
                }

                if (ipv6Path.toString().equals("/")) {
                    System.out.println("        IPv6: No IPv6 Config");
                } else {

                    Properties ipv6Config =
                            dbusConn.getRemoteObject(
                                    "org.freedesktop.NetworkManager", ipv6Path.toString(), Properties.class);
                    List<Map<String, Variant<?>>> addressArrV6 =
                            ipv6Config.Get("org.freedesktop.NetworkManager.IP6Config", "AddressData");
                    printAddressInfo(addressArrV6, "IPv6");
                }
                System.out.println("");
            }

        } catch (IOException _ex) {
            _ex.printStackTrace();
        }
    }

    private static void printAddressInfo(List<Map<String, Variant<?>>> addressArr, String _addrType) {
        for (int i = 0; i < addressArr.size(); i++) {
            System.out.println("  " + _addrType + " Address " + (i + 1));
            if (addressArr.get(i).containsKey("address")) {
                System.out.print(
                        "     "
                                + _addrType
                                + ": "
                                + String.valueOf(addressArr.get(i).get("address")).replaceAll("\\[|\\]", ""));
                if (addressArr.get(i).containsKey("prefix")) {
                    System.out.println(
                            " / " + String.valueOf(addressArr.get(i).get("prefix")).replaceAll("\\[|\\]", ""));
                }
            } else {
                System.out.println("");
            }
            for (var entry : addressArr.get(i).entrySet()) {
                if (entry.getKey().equals("address") || entry.getKey().equals("prefix")) {
                    continue;
                }
                System.out.println("     " + entry.getKey() + " = " + entry.getValue());
            }
        }
    }
}
