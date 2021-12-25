# add jre
chroot_exec apk add openjdk8-jre

# copy java file
#mkdir ${ROOTFS_PATH}/opt/kisiot/ && cp ${INPUT_PATH}/app.jar ${ROOTFS_PATH}/opt/kisiot/
#mkdir ${ROOTFS_PATH}/opt/kisiot/lib && cp -R ${INPUT_PATH}/lib/* ${ROOTFS_PATH}/opt/oms/lib
%s

# setting java starter
cat >> ${ROOTFS_PATH}/etc/init.d/kisapp <<EOF
#!/sbin/openrc-run
# shellcheck shell=ash
# shellcheck disable=SC2034

command="/usr/bin/java"
pidfile="/var/run/java-app.pid"
command_args="-jar /opt/kisiot/app.jar"
command_background=true

depend() {
	use logger dns
	need net
	after firewall
}
EOF
chroot_exec chmod u+x /etc/init.d/kisapp
chroot_exec rc-update add kisapp default