package io.github.kischang.kisiot.maven.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作文件和目录的工具类
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static boolean copyFileToPath(File src, File dest) {
        return copyFileToPath(src, dest, false);
    }

    public static boolean copyFileToPath(File src, File dest, boolean overwrite) {
        String fileName = src.getName();
        File tarFile = new File(dest.getPath() + File.separator + fileName);
        return copyFileToFile(src, tarFile, overwrite);
    }

    public static boolean copyFileToFile(File src, File dest) {
        return copyFileToFile(src, dest, false);
    }

    /**
     * @return  目标文件是否存在
     */
    public static boolean copyFileToFile(File src, File dest, boolean overwrite) {
        if (!src.exists() || src.isDirectory()){
            logger.error(src.getPath()+"文件不存在！！无法完成拷贝");
            return false;
        }
        boolean stat = false;
        if(dest.exists()){
            if (!overwrite) {
                logger.info("copyFile>>" + src + "，目标文件已存在，不执行覆盖，跳过该文件");
                return true;
            }
            stat = true;
            logger.warn("copyFile>>" + src + "，目标文件已存在，执行覆盖");
        }
        logger.debug("copy:"+src.getPath()+"TO>>>"+dest.getPath());
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stat;
    }

    public static void copyFolder(File src, File dest, List<String[]> list) {
        copyFolder(src, dest, false, list);
    }

    /**
     * 目录——》目录
     * 复制一个目录下的子目录、文件到另外一个目录
     * @return 所有拷贝过的文件，数组1元素：文件名，数组2元素：目标文件是否存在
     */
    public static void copyFolder(File src, File dest, boolean overwrite,List<String[]> list) {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // 递归复制
                copyFolder(srcFile, destFile, overwrite,list);
            }
        } else {
            if(list == null){
                copyFileToFile(src, dest, overwrite);
            }else{
                list.add(new String[]{dest.getPath(), String.valueOf(copyFileToFile(src, dest, overwrite))});
            }
        }
    }

    /**
     * 删除目录，同时会删除该目录
     * @param dir   要删除的目录
     * @param list  传入用来存储删除了哪些文件的List数组，传入null会不存储，不会报错
     */
    public static void deleteDir(File dir,List<String[]> list){
        if(dir.isDirectory()){
            for(File file : dir.listFiles()){
                deleteDir(file,list);
            }
        }
        logger.debug("delete>"+dir.getPath());
        if(list == null){
            dir.delete();
        }else{
            list.add(new String[]{dir.getPath(),dir.delete()?"TRUE":"FALSE"});
        }
    }

    /**
     * 删除指定目录下的所有空目录，如果执行完成之后该目录下也被清空（该目录下都为文件夹且文件夹都是空目录），则该目录也会被删除
     * @param dir   要执行删除的目录
     * @param list  所有删除的文件夹的路径
     */
    public static void deleteEmptyDir(File dir,List<String> list){
        if(dir.isDirectory()){
            for(File file : dir.listFiles()){
                if(file.isDirectory()){
                    deleteEmptyDir(file,list);
                }
            }
            File[] temp = dir.listFiles();
            if(temp == null || temp.length == 0){
                dir.delete();
                if(list != null){
                    list.add(dir.getPath());
                }
            }
        }
    }

    public static boolean deleteFile(File delFile) {
        // 路径为文件且不为空则进行删除
        if (delFile.isFile() && delFile.exists()) {
            delFile.delete();
            return true;
        }
        return false;
    }
    /**
     * 删除单个文件
     * @param   delFilePath    被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String delFilePath) {
        return deleteFile(new File(delFilePath));
    }

    public static boolean deleteDirectory(File delDirFile) {
        //如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!delDirFile.exists() || !delDirFile.isDirectory()) {
            return false;
        }
        for(File childFile : delDirFile.listFiles()){
            //删除子文件
            if (childFile.isFile()) {
                deleteFile(childFile);
            } //删除子目录
            else {
                deleteDirectory(childFile);
            }
        }
        //删除当前目录
        if (delDirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 删除目录（文件夹）以及目录下的文件
     * @param   delPath 被删除目录的文件路径
     * @return  目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String delPath) {
        if (delPath == null || "".equals(delPath)){
            return false;
        }
        if(!delPath.endsWith(File.separator)){
            delPath = delPath + File.separator;
        }
        return deleteDirectory(new File(delPath));
    }

    public static void main(String[] args) {
        String testPath = "D:\\Temp\\";
//        copyFileToPath(new File(testPath + "src\\sour.txt"), new File(testPath + "tarPath"));
        /*List<String[]> list = new ArrayList<String[]>();
        copyFolder(new File(testPath + "src"), new File(testPath + "tar"),list);
        for(String[] s : list) {
            System.out.println(s[0]+">>>"+s[1]);
        }*/
        /*File file = new File(testPath+"asd");
        List<String[]> list = new ArrayList<String[]>();
        deleteDir(file,list);
        for(String[] s : list) {
            System.out.println(s[0]+">>>"+s[1]);
        }*/
        List<String> list = new ArrayList<String>();
        deleteEmptyDir(new File(testPath),list);
        for(String str : list){
            System.out.println(str);
        }
    }
}
