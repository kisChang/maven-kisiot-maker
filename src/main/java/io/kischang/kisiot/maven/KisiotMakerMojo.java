package io.kischang.kisiot.maven;

import io.kischang.kisiot.maven.utils.ArchEnum;
import io.kischang.kisiot.maven.utils.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * Kisiot IMG 构建插件
 */
@Mojo(name = "maker"
        // 实例化策略
        , instantiationStrategy = InstantiationStrategy.SINGLETON,
        // 如果用户没有在POM中明确设置此Mojo绑定到的phase，那么绑定一个MojoExecution到那个phase
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        // 提示此Mojo需要被直接调用（而非绑定到生命周期阶段）
        requiresDirectInvocation = true,
        threadSafe = false
)
@Execute(goal = "maker",        // 如果提供goal，则隔离执行此Mojo
        phase = LifecyclePhase.PACKAGE // 在此生命周期阶段自动执行此Mojo
)
public class KisiotMakerMojo extends AbstractMojo {

    private static final Logger logger = LoggerFactory.getLogger(KisiotMakerMojo.class);

    @Parameter(name = "dockerCmd", defaultValue = "docker")
    private String dockerCmd;

    @Parameter(name = "enSerialConsole", defaultValue = "false")
    private boolean enSerialConsole = false;

    @Parameter(name = "enWiFiSupport", defaultValue = "false")
    private boolean enWiFiSupport = false;

    @Parameter(name = "imgRootFsSize", defaultValue = "100")
    private int imgRootFsSize = 100;

    @Parameter(name = "imgHostName", defaultValue = "kisiot")
    private String imgHostName = "kisiot";

    @Parameter(name = "imgRootPw", defaultValue = "pi")
    private String imgRootPw = "pi";

    @Parameter(name = "mirrorAlpine", defaultValue = "https://mirrors.tuna.tsinghua.edu.cn/alpine")
    private String mirrorAlpine = "https://mirrors.tuna.tsinghua.edu.cn/alpine";

    @Parameter(name = "mirrorRpiGit", defaultValue = "https://github.com/raspberrypi/firmware")
    private String mirrorRpiGit = "https://github.com/raspberrypi/firmware";

    @Parameter(name = "mirrorRaspiTag", defaultValue = "latest")
    private String mirrorRaspiTag = "latest";

    @Parameter(name = "rpiArch", defaultValue = "aarch64")
    private ArchEnum rpiArch;
    @Parameter(name = "rpiTimezone", defaultValue = "Asia/Shanghai")
    private String rpiTimezone;
    @Parameter(name = "rpiModules", defaultValue = "ipv6 af_packet rfkill cfg80211 brcmutil brcmfmac rpi-poe-fan")
    private String rpiModules;

    @Parameter(name = "jarFileConf")
    private Properties jarFileConf;


    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File target;
    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File targetOutputDirectory;

    public void execute() {
        File inputPathFile = Paths.get(target.getAbsolutePath(), "kisiot", "input").toFile();
        String inputPath = inputPathFile.getAbsolutePath();
        File outputPath = Paths.get(target.getAbsolutePath(), "kisiot").toFile();
        //0. 创建相关目录
        outputPath.mkdirs();
        inputPathFile.mkdirs();

        //1. 生成input中的custom.sh 脚本
        String imgCustomShPath = inputPath + File.separator + "img-custom.sh";
        StringBuilder copyCpSb = new StringBuilder();
        String finalName = project.getBuild().getFinalName() + ".jar";

        //2. 生成目录和程序文件复制
        copyCpSb.append(
                String.format("mkdir ${ROOTFS_PATH}/opt/kisiot/ && cp ${INPUT_PATH}/%s ${ROOTFS_PATH}/opt/kisiot/app.jar \n", finalName)
        );
        FileUtils.copyFileToFile(
                new File(target.getAbsolutePath(), finalName),
                new File(inputPath, finalName)
        );
        if (jarFileConf != null) {
            for (String fileName : jarFileConf.stringPropertyNames()) {
                String targetFilename = jarFileConf.getProperty(fileName);
                File sourceFilename = new File(targetOutputDirectory.getAbsolutePath(), fileName);
                if (sourceFilename.exists()) {
                    continue;
                }
                boolean fileType = sourceFilename.isDirectory();
                if (fileType) { // 目录
                    FileUtils.copyFolder(sourceFilename, new File(inputPath, fileName),true, null);
                }else { // 文件
                    FileUtils.copyFileToFile(sourceFilename, new File(inputPath, fileName));
                }

                copyCpSb.append(
                        String.format("mkdir ${ROOTFS_PATH}/opt/kisiot/%s && cp %s ${INPUT_PATH}/%s ${ROOTFS_PATH}/opt/kisiot/%s \n"
                                , targetFilename
                                , fileType ? "-R" : ""
                                , fileType ? fileName +"/*" : fileName
                                , targetFilename
                        )
                );
            }
        }
        try (OutputStream out = new FileOutputStream(imgCustomShPath)){
            InputStream shContentIn = KisiotMakerMojo.class.getClassLoader().getResourceAsStream("install-app.sh");
            String appContent = convertStreamToString(Objects.requireNonNull(shContentIn));
            appContent = String.format(appContent, copyCpSb);

            String baseContent = convertStreamToString(Objects.requireNonNull(KisiotMakerMojo.class.getClassLoader().getResourceAsStream("img-custom.sh")));

            strToStream(out, baseContent, appContent);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //3. 执行构建脚本
        String dockerBuildCmd = String.format(
                "%s pull ghcr.io/raspi-alpine/builder:%s && " +
                        "%s run --rm -v %s:/output -v %s:/input " +
                        "-e ARCH=%s " +
                        "-e DEFAULT_TIMEZONE=%s " +
                        "-e DEFAULT_KERNEL_MODULES=\"%s\" " +
                        "-e CUSTOM_IMAGE_SCRIPT=img-custom.sh " +
                        "-e ALPINE_MIRROR=%s " +
                        "-e RPI_FIRMWARE_GIT=%s " +
                        "-e DEFAULT_HOSTNAME=%s " +
                        "-e DEFAULT_ROOT_PASSWORD=%s " +
                        "-e SIZE_ROOT_FS=%sM " +
                        "-e IMG_NAME=%s ghcr.io/raspi-alpine/builder:%s",
                dockerCmd, mirrorRaspiTag, dockerCmd
                , outputPath.getAbsolutePath(), inputPath
                , rpiArch.name() , rpiTimezone , rpiModules
                , mirrorAlpine, mirrorRpiGit
                , imgHostName, imgRootPw, imgRootFsSize
                , finalName
                , mirrorRaspiTag
        );
        logger.info("run command: {}", dockerBuildCmd);
        try {
            runCommand(dockerBuildCmd);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //4. 清理input目录
        FileUtils.deleteDirectory(inputPathFile);
    }

    private void runCommand(String cmd) throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        linkToOut(proc.getErrorStream(), System.err);
        linkToOut(proc.getInputStream(), System.out);
        int exitVal = proc.waitFor();
        logger.info("Process exitValue: {}", exitVal);
    }

    private static void linkToOut(InputStream in, PrintStream out) {
        new Thread(() -> {
            String line = null;
            InputStreamReader esr = new InputStreamReader(in);
            BufferedReader ebr = new BufferedReader(esr);
            while (true) {
                try {
                    if ((line = ebr.readLine()) == null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.println(line);
            }
        }).start();
    }

    private void strToStream(OutputStream out, String... content) throws IOException {
        try (BufferedOutputStream outStream = new BufferedOutputStream(out)) {
            for (String once : content) {
                outStream.write(once.getBytes());
                outStream.write("\n\n".getBytes());
            }
        }
    }

    private static String convertStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}
