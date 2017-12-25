package com.zt.upload.utils;

import com.zt.upload.controller.UploadListener;
import com.zt.upload.utils.module.UploadFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author zhangtong
 * Created by on 2017/11/28
 */
public class FileUtils {

    protected final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 上传文件，并获取上传文件（单文件上传）
     *
     * @param request    [requewt请求]
     * @param folderPath [文件保存路]
     * @return [文件自定义实体类]
     */
    public UploadFile getFile(HttpServletRequest request, String folderPath) {
        // 创建一个通用的多部分解析器
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
        // 新建目录
        File directory = new File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try {
            // 判断 request 是否有文件上传,即多部分请求
            if (multipartResolver.isMultipart(request)) {
                // 转换成多部分request
                MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
                // 取得request中的所有文件名
                Iterator<String> iter = multiRequest.getFileNames();
                while (iter.hasNext()) {
                    UploadFile f = transferFile(folderPath, multiRequest.getFile(iter.next()));
                    if (f != null) {
                        return f;
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 上传文件，并获取上传文件列表（多文件上传）
     *
     * @param request    [requewt请求]
     * @param folderPath [文件保存路]
     * @return [文件自定义实体类]
     */
    public List<UploadFile> getFiles(HttpServletRequest request, String folderPath) {
        HttpSession session = request.getSession();
        UploadListener uploadListener = new UploadListener(session);
        // 创建一个通用的多部分解析器
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
        // 新建目录
        File directory = new File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        List<UploadFile> files = new ArrayList<>();
        // 判断 request 是否有文件上传,即多部分请求
        if (multipartResolver.isMultipart(request)) {
            // 转换成多部分request
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            // 取得request中的所有文件名
            Iterator<String> iter = multiRequest.getFileNames();
            while (iter.hasNext()) {
                List<MultipartFile> multipartFiles = multiRequest.getFiles(iter.next());
                multipartFiles.stream().forEach(multipartFile -> {
                    UploadFile f = null;
                    try {
                        f = transferFile(folderPath, multipartFile);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    if (f != null) {
                        files.add(f);
                    }
                });

            }
        }
        return files;
    }

    /**
     * 文件写入磁盘
     *
     * @param folderPath [description]
     * @param file       [description]
     * @return [description]
     * @throws IOException [description]
     */
    private UploadFile transferFile(String folderPath, MultipartFile file) throws IOException {
        // 取得上传文件
//        MultipartFile file = multiRequest.getFile(path);

        if (file != null) {
            // 取得当前上传文件的文件名称
            String fileMD5 = DigestUtils.md5Hex(file.getBytes());
            String originalFileName = file.getOriginalFilename();//原名称 带后缀
            String fileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));//原名称
            String suffixName = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);//后缀名

            double size = (file.getSize() * 1.0) / (1024 * 1.0) / (1024 * 1.0);
            BigDecimal bg = new BigDecimal(size);
            double f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            String fileSize = f1 + "";

            // 如果名称不为“”,说明该文件存在，否则说明该文件不存在
            if (originalFileName.trim() != "") {
                // 重命名上传后的文件名
                String newName = UUID.randomUUID().toString() + "." + suffixName;
                // 定义上传路径
                String tarpath = folderPath + "/" + newName;
                File localFile = new File(tarpath);
                file.transferTo(localFile);
                UploadFile uploadFile = new UploadFile()
                        .setFile(localFile)
                        .setFullFileName(newName)
                        .setFileName(fileName)
                        .setOriginalFileName(originalFileName)
                        .setSuffixName(suffixName)
                        .setFileSize(fileSize)
                        .setFileMD5(fileMD5);
                return uploadFile;
            }
        }
        return null;
    }

    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 删除文件与目录
     *
     * @param filePath
     * @return
     */
    public static boolean deleteFolder(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 判断目录或文件是否存在
        if (!file.exists()) { // 不存在返回 false
            return flag;
        } else {
            // 判断是否为文件
            if (file.isFile()) { // 为文件时调用删除文件方法
                return deleteFile(filePath);
            } else { // 为目录时调用删除目录方法
                return deleteDirectory(filePath);
            }
        }
    }

    /**
     * 删除目录
     *
     * @param filePath
     * @return
     */
    public static boolean deleteDirectory(String filePath) {
        boolean flag = false;
        // 如果sPath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        // 删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // 删除子文件
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag)
                    break;
            } // 删除子目录
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag) {
            return false;
        }
        // 删除当前目录
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 下载文件
     *
     * @param request
     * @param response
     * @param filePath
     * @return
     * @throws Exception
     */
    public static boolean fileDownLoad(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String filePath) throws Exception {
        boolean bl = false;
        if (response != null && request != null && filePath != null
                && !filePath.equals("")) {
            String browserType = request.getParameter("browserType");
            final String userAgent = request.getHeader("USER-AGENT");
            logger.info("userAgent==:" + userAgent);

            // filePath是指欲下载的文件的路径。
            filePath = URLDecoder.decode(filePath, "UTF-8");
            File file = new File(filePath);
            if (!file.exists()) {
                logger.info("############ FilePath: " + filePath);
                throw new Exception("文件不存在！");
            }
            if (!file.isFile()) {
                logger.info("############ FilePath: " + filePath);
                throw new Exception("非文件类型！");
            }

            // 取得文件名。
            String fileName = file.getName();
            if ("IE".equals(browserType)) {// IE浏览器,页面传过来的值，只用于判断是否为IE浏览器
                logger.info("ie浏览器");
                fileName = URLEncoder.encode(fileName, "UTF8");
            } else {
                if (userAgent.contains("Mozilla")) {// google,火狐浏览器
                    fileName = new String(fileName.getBytes(), "ISO8859-1");
                    logger.info("火狐浏览器");
                } else {
                    logger.info("其他浏览器");
                    fileName = URLEncoder.encode(fileName, "UTF8");// 其他浏览器
                }
            }
            // 提示框设置
            response.reset(); // reset the response
            // response.setCharacterEncoding("UTF-8");
            response.setContentType("application/octet-stream");//告诉浏览器输出内容为流
            response.setHeader("content-disposition", "attachment; filename=\""
                    + fileName + "\"");

            //读出文件到i/o流
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream buff = new BufferedInputStream(fis);
            byte[] ary_byte = new byte[1024];//缓存
            long k = 0;//该值用于计算当前实际下载了多少字节
            // 输出流
            OutputStream out = response.getOutputStream();
            //开始循环下载
            while (k < file.length()) {
                int j = buff.read(ary_byte, 0, 1024);
                k += j;
                //将b中的数据写到客户端的内存
                out.write(ary_byte, 0, j);
            }
            // 关闭输出流
            if (out != null) {
                out.flush();
                out.close();
                fis.close();
                buff.close();
            }
            bl = true;
            logger.info("文件下载完毕！");
        } else {
            new NullPointerException(
                    "HttpServletRequest Or HttpServletResponse Or fileName Is Null !");
        }
        return bl;
    }

    /**
     * 复制文件
     *
     * @param sourcePath
     * @param destPath
     * @throws IOException
     */
    public static boolean copyFileUsingFileStreams(String sourcePath, String destPath)
            throws IOException {
        File source = new File(sourcePath);
        File dest = new File(destPath);
        if (!source.exists()) {
            throw new IOException("文件复制失败：源文件（" + source + "） 不存在");
        }
        if (dest.isDirectory()) {
            throw new IOException("文件复制失败：复制路径（" + dest + "） 错误");
        }
        File parent = dest.getParentFile();
        // 创建复制路径
        if (!parent.exists()) {
            parent.mkdirs();
        }
        // 创建复制文件
        if (!dest.exists()) {
            dest.createNewFile();
        }
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(dest);

        BufferedInputStream bis = new BufferedInputStream(fis);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] KB = new byte[1024];
        int index;
        while ((index = bis.read(KB)) != -1) {
            bos.write(KB, 0, index);
        }

        bos.close();
        bis.close();
        fos.close();
        fis.close();
        if (!dest.exists()) {
            return false;
        } else if (dest.length() != dest.length()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 复制文件
     *
     * @param sourcePath
     * @param destPath
     * @throws IOException
     */
    public static void copyFileUsingJava7Files(String sourcePath, String destPath)
            throws IOException {
        File source = new File(sourcePath);
        File dest = new File(destPath);
        Files.copy(source.toPath(), dest.toPath());
    }
}
