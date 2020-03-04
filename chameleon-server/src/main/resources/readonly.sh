#!/bin/bash

function is_pi() {
  ARCH=$(dpkg --print-architecture)
  if [ "$ARCH" = "armhf" ] ; then
    echo 0
  else
    echo 1
  fi
}

function is_pione() {
   if grep -q "^Revision\s*:\s*00[0-9a-fA-F][0-9a-fA-F]$" /proc/cpuinfo; then
      echo 0
   elif grep -q "^Revision\s*:\s*[ 123][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]0[0-36][0-9a-fA-F]$" /proc/cpuinfo ; then
      echo 0
   else
      echo 1
   fi
}

function is_pitwo() {
   if grep -q "^Revision\s*:\s*[ 123][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]04[0-9a-fA-F]$" /proc/cpuinfo; then
      echo 0
   else
      echo 1
   fi
}

function is_pizero() {
   if grep -q "^Revision\s*:\s*[ 123][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]0[9cC][0-9a-fA-F]$" /proc/cpuinfo; then
      echo 0
   else
      echo 1
   fi
}

function is_pifour() {
   if grep -q "^Revision\s*:\s*[ 123][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]11[0-9a-fA-F]$" /proc/cpuinfo; then
      echo 0
   else
      echo 1
   fi
}

function get_pi_type() {
  if [ $(is_pi) ]; then
    if [ $(is_pione) -eq 0 ]; then
      echo 1
    elif [ $(is_pitwo) -eq 0 ]; then
      echo 2
    elif [ $(is_pizero) -eq 0 ]; then
      echo 0
    elif [ $(is_pifour) -eq 0 ]; then
      echo 4
    else
      echo 3
    fi
  else
    echo -1
  fi
}

pi_type=$(get_pi_type)

if [ $pi_type -ne 3 ] && [ $pi_type -ne 4 ]
then
  echo "This script is only for Raspberry Pi 3 and 4!"
  exit 1
fi

if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root" 
   exit 1
fi

echo -e "GET http://google.com HTTP/1.0\n\n" | nc google.com 80 > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "Internet connection good! Proceding..."
else
    echo "Can't connect to the internet! Internet is needed for this operation. Try again with internet connection!"
    exit 1
fi

#
# From https://medium.com/swlh/make-your-raspberry-pi-file-system-read-only-raspbian-buster-c558694de79
#

apt-get update && apt-get upgrade
apt-get remove --purge triggerhappy logrotate dphys-swapfile
apt-get autoremove --purge

echo ' fastboot noswap ro' >> /boot/cmdline.txt

sudo apt-get install busybox-syslogd
sudo apt-get remove --purge rsyslog

sed -i 's/vfat\s*defaults/vfat    defaults,ro' /etc/fstab
sed -i 's/ext4\s*defaults,noatime/ext4\s*defaults,noatime,ro' /etc/fstab

echo '\ntmpfs        /tmp            tmpfs   nosuid,nodev         0       0\ntmpfs        /var/log        tmpfs   nosuid,nodev\n        0       0\ntmpfs        /var/tmp        tmpfs   nosuid,nodev         0       0' >> /etc/fstab

sudo rm -rf /var/lib/dhcp /var/lib/dhcpcd5 /var/spool /etc/resolv.conf
sudo ln -s /tmp /var/lib/dhcp
sudo ln -s /tmp /var/lib/dhcpcd5
sudo ln -s /tmp /var/spool
sudo touch /tmp/dhcpcd.resolv.conf
sudo ln -s /tmp/dhcpcd.resolv.conf /etc/resolv.conf

sudo rm /var/lib/systemd/random-seed
sudo ln -s /tmp/random-seed /var/lib/systemd/random-seed

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStartPre=/bin/echo "" >/tmp/random-seed

sed -i 's/\[Service\]\nType=oneshot\nRemainAfterExit=yes/\[Service\]\nType=oneshot\nRemainAfterExit=yes\nExecStartPre=/bin/echo "" >/tmp/random-seed' /lib/systemd/system/systemd-random-seed.service

# add ro and rw alianses

echo 'set_bash_prompt() {\n\n    fs_mode=$(mount | sed -n -e "s/^\/dev\/.* on \/ .*(\(r[w|o]\).*/\1/p")    \n\nPS1=\'\[\033[01;32m\]\u@\h${fs_mode:+($fs_mode)}\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ \'
}\nalias ro=\'sudo mount -o remount,ro / ; sudo mount -o remount,ro /boot\'alias rw=\'sudo mount -o remount,rw / ; sudo mount -o remount,rw /boot\'\nPROMPT_COMMAND=set_bash_prompt' >> /etc/bash.bashrc

echo 'mount -o remount,ro /\nmount -o remount,ro /boot' >> /etc/bash.bash_logout

echo "System going down for reboot!"
reboot