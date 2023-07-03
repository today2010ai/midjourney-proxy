package com.github.novicezk.midjourney.util;


import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 文件上传工具
 * @author bool
 * @date 2019-07-30 21:00
 */
@Component
public class OssUtils {

    private static String endpoint;
    private static String accessKeyId;
    private static String accessKeySecret;

    @Value("${aliyun.oss.endpoint}")
    public void setEndpoint(String endpoint) {
        OssUtils.endpoint = endpoint;
    }

    @Value("${aliyun.accessKeyId}")
    public void setAccessKeyId(String accessKeyId) {
        OssUtils.accessKeyId = accessKeyId;
    }

    @Value("${aliyun.accessKeySecret}")
    public void setAccessKeySecret(String accessKeySecret) {
        OssUtils.accessKeySecret = accessKeySecret;
    }

    public static OSSClient getOSSClient(String bucketName){
        return (OSSClient) new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 后缀分割符号
     */
    private static final String SUFFIX_SPLIT = ".";

    public static String uploadFile(MultipartFile file,String bucketName,String path,String objectName) throws IOException {
        path = nomalizePath(path);
        // 创建OSSClient实例。
        OSSClient ossClient = getOSSClient(bucketName);
        // 上传内容到指定的存储空间（bucketName）并保存为指定的文件名称（objectName）。
        ossClient.putObject(bucketName, path+objectName, new ByteArrayInputStream(file.getBytes()));
        // 关闭OSSClient。
        ossClient.shutdown();
        //String resize = resize(bucketName, path+"o"+objectName,path+objectName,190,190);
        return generateDownloadUrlByBucket(bucketName,path+objectName);
    }

    private static String nomalizePath(String path){
        if(path.startsWith("/"))//如果是以/开头
            path = path.substring(1,path.length());
        if(!path.endsWith("/"))//如果不是以/结尾
            path = path.concat("/");
        return path;
    }



    public static String generateDownloadUrlByBucket(String bucketName,String objectName){
        if (bucketName==null || bucketName.equals("") || objectName==null || objectName.equals(""))
            return null;
        else
            return "https://cdn.kamalyoga.fit/"+objectName;
    }


    /**
     * 上传文件方法
     *
     */
    public static String uploadFileByByte(byte[] buffer,String bucketName,String path,String objectName) throws IOException {
        path = nomalizePath(path);
        // 创建OSSClient实例。
        OSSClient ossClient = getOSSClient(bucketName);
        // 上传内容到指定的存储空间（bucketName）并保存为指定的文件名称（objectName）。
        ossClient.putObject(bucketName, path+objectName, new ByteArrayInputStream(buffer));
        // 关闭OSSClient。
        ossClient.shutdown();
        return generateDownloadUrlByBucket(bucketName,path+objectName);
    }

    /**
     * 上传文件方法
     *
     */
    public static String uploadFileByUrl(String url,String bucketName,String path,String objectName) throws IOException {
        path = nomalizePath(path);
        // 创建OSSClient实例。
        OSSClient ossClient = getOSSClient(bucketName);
        // 从 URL 获取图片输入流
        InputStream inputStream = new URL(url).openStream();
        ossClient.putObject(bucketName, path+objectName, inputStream);
        // 关闭OSSClient。
        ossClient.shutdown();
        return generateDownloadUrlByBucket(bucketName,path+objectName);
    }
}
