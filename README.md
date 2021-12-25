Raspberry Pi alpine linux img maven build plugin   
树莓派Alpine Linux固件 Maven 构建插件，直接打包jar为系统镜像，依赖docker+Linux

Base on: https://github.com/raspi-alpine/builder    
Base on: https://github.com/kisChang/raspi-os-frame   

### 使用方式
```
1. add plugin (添加 Plugin)
<plugin>
    <groupId>io.kischang.kisiot.maven</groupId>
    <artifactId>maven-kisiot-maker</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <!--国内使用git镜像加快构建-->
        <mirrorRpiGit>https://github.com.cnpmjs.org/raspberrypi/firmware</mirrorRpiGit>
        <!--根目录分区大小，java需要大一些的空间，默认仅100MB-->
        <imgRootFsSize>250</imgRootFsSize>
    </configuration>
</plugin>

2. run maven plugin(执行构建命令)
mvn clean package kisiot-maker:maker -Dmaven.test.skip=true

3. img files on target/kisiot (编译输出)
${build.finalName}.img.gz
${build.finalName}.img.gz.sha256
${build.finalName}_update.img.gz
${build.finalName}_update.img.gz.sha256
```