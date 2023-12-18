package net.diaowen.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 这个类提供了 ZIP 文件相关的工具方法，用于创建 ZIP 文件和文件夹的压缩包，并支持文件的清理操作。
 */
public class ZipUtil {
     private static final Logger log = LoggerFactory.getLogger(ZipUtil.class);

    private ZipUtil() {
    }
    /**
     * 创建ZIP文件
     * @param sourcePath 文件或文件夹路径
     * @param zipPath 生成的zip文件存在路径（包括文件名）
     * @param isDrop  是否删除原文件:true删除、false不删除
     */
  public static void createZip(String sourcePath, String zipPath,Boolean isDrop) {
        //创建文件输出流
        FileOutputStream fos = null;
        //创建zip输出流
        ZipOutputStream zos = null;
        try {
            //创建文件输出流
            fos = new FileOutputStream(zipPath);
            //创建zip输出流
            zos = new ZipOutputStream(fos);
            //创建xml文件
            //createXmlFile(sourcePath,"293.xml");
            //写入zip文件
            writeZip(new File(sourcePath), "", zos,isDrop);
        } catch (FileNotFoundException e) {
            log.error("创建ZIP文件失败",e);
        } finally {
            try {
                //关闭zip输出流
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                log.error("创建ZIP文件失败",e);
            }

        }
    }
    /**
     * 清空文件和文件目录
     *
     * @param f
     */
   public static void clean(File f) throws Exception {
        //获取文件夹下的文件列表
        String cs[] = f.list();
        //如果文件列表为空，则删除文件夹
        if (cs == null || cs.length <= 0) {
            boolean isDelete = f.delete();
            if (!isDelete) {
                throw new Exception(f.getName() + "文件删除失败！");
            }
        //如果文件列表不为空，则遍历文件列表，删除文件
        } else {
            for (int i = 0; i < cs.length; i++) {
                String cn = cs[i];
                String cp = f.getPath() + File.separator + cn;
                File f2 = new File(cp);
                //如果文件存在且是文件，则删除
                if (f2.exists() && f2.isFile()) {
                    boolean isDelete = f2.delete();
                    if (!isDelete) {
                        throw new Exception(f2.getName() + "文件删除失败！");
                    }
                //如果文件存在且是文件夹，则递归调用clean方法
                } else if (f2.exists() && f2.isDirectory()) {
                    clean(f2);
                }
            }
            //删除文件夹
            boolean isDelete = f.delete();
            if (!isDelete) {
                throw new Exception(f.getName() + "文件删除失败！");
            }
        }
    }
   /**
     * writeZip方法用于将指定文件夹或文件添加到ZipOutputStream中。
     *
     * @param file       要添加到ZipOutputStream中的文件
     * @param parentPath  文件相对于ZipOutputStream的父路径
     * @param zos         ZipOutputStream对象
     * @param isDrop      是否删除原始文件
     */
    private static void writeZip(File file, String parentPath, ZipOutputStream zos, Boolean isDrop) {
        if (file.exists()) {
            if (file.isDirectory()) { // 处理文件夹
                parentPath += file.getName() + File.separator;
                File[] files = file.listFiles();
                if (files.length != 0) {
                    for (File f : files) {
                        writeZip(f, parentPath, zos, isDrop);
                    }
                } else { // 空目录则创建当前目录
                    try {
                        zos.putNextEntry(new ZipEntry(parentPath));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    ZipEntry ze = new ZipEntry(parentPath + file.getName());
                    zos.putNextEntry(ze);
                    byte[] content = new byte[1024];
                    int len;
                    while ((len = fis.read(content)) != -1) {
                        zos.write(content, 0, len);
                    }
                } catch (FileNotFoundException e) {
                    log.error("创建ZIP文件失败", e);
                } catch (IOException e) {
                    log.error("创建ZIP文件失败", e);
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                        if (isDrop) {
                            clean(file);
                        }
                    } catch (IOException e) {
                        log.error("创建ZIP文件失败", e);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public static void main(String[] args) {
//        ZipUtil.createZip("/Users/keyuan/Documents/GIT/my-gitlab/dw/dwsurvey_b1/dwsurvey/target/dwsurvey/file/402880e864e15c150164e3917b930000", "/Users/keyuan/Documents/GIT/my-gitlab/dw/dwsurvey_b1/dwsurvey/target/dwsurvey/file/402880e864e15c150164e3917b930000.zip",false);
    }
}