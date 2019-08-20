import subprocess
import netifaces


class ChangeIP:
    def __init__(self, connection_type, ip, netmask, gateway, hostname):

        adapter = self.find_adapter()

        if connection_type == "DHCP":
            self.change_to_dhcp(adapter=adapter)
        elif connection_type == "Static":
            self.change_to_static(adapter=adapter, ip=ip, netmask=netmask, gateway=gateway)

        self.change_hostname(hostname=hostname)
        self.restart_adapter(adapter=adapter)

    @staticmethod
    def change_to_dhcp(adapter):
        subprocess.call(['dhclient',"-r", adapter])

    @staticmethod
    def change_to_static(adapter, ip, netmask, gateway):
        subprocess.call(['ifconfig', adapter, ip, 'netmask', netmask])
        subprocess.call(['route add default gw', gateway, adapter])

    @staticmethod
    def restart_adapter(adapter):
        subprocess.call(['ifconfig', adapter, 'down'])
        subprocess.call(['ifconfig', adapter, 'up'])

    @staticmethod
    def find_adapter():
        for i_name in netifaces.interfaces():
            interface = netifaces.ifaddresses(i_name)[netifaces.AF_INET][0]
            address = interface['addr'].split('.')[0]
            if address == "10":
                return str(i_name)

    @staticmethod
    def change_hostname(hostname):
        subprocess.call(['hostnamectl', 'set-hostname', "Chameleon-Vision-" + hostname])
