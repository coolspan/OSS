package com.oos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

import com.aliyun.openservices.ClientException;
import com.aliyun.openservices.ServiceException;
import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.OSSErrorCode;
import com.aliyun.openservices.oss.OSSException;
import com.aliyun.openservices.oss.model.CannedAccessControlList;
import com.aliyun.openservices.oss.model.GetObjectRequest;
import com.aliyun.openservices.oss.model.OSSObjectSummary;
import com.aliyun.openservices.oss.model.ObjectListing;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.aliyun.openservices.oss.model.PutObjectResult;

/**
 * 该示例代码展示了如果在OSS中创建和删除一个Bucket，以及如何上传和下载一个文件。
 * 
 * 该示例代码的执行过程是： 1. 创建一个Bucket（如果已经存在，则忽略错误码）； 2. 上传一个文件到OSS； 3. 下载这个文件到本地； 4.
 * 清理测试资源：删除Bucket及其中的所有Objects。
 * 
 * 尝试运行这段示例代码时需要注意： 1. 为了展示在删除Bucket时除了需要删除其中的Objects,
 * 示例代码最后为删除掉指定的Bucket，因为不要使用您的已经有资源的Bucket进行测试！ 2.
 * 请使用您的API授权密钥填充ACCESS_ID和ACCESS_KEY常量； 3.
 * 需要准确上传用的测试文件，并修改常量uploadFilePath为测试文件的路径； 修改常量downloadFilePath为下载文件的路径。 4.
 * 该程序仅为示例代码，仅供参考，并不能保证足够健壮。
 * 
 */
public class OSSObjectSample {

	private static final String ACCESS_ID = "u39NioN3ulOE09Ux";
	private static final String ACCESS_KEY = "9SO8xw6rsiYBF1HwLkHPgt3d48a3Aj";
	private static final String OSS_ENDPOINT = "http://oss.aliyuncs.com/";// http://images2.lejoying.com

	public static void main(String[] args) throws Exception {
		String bucketName = "wxgs";
		String key = "images/oos3.jpg";
		String uploadFilePath = "G:/108.png";
		String downloadFilePath = "G:/108_copy.png";

		// 使用默认的OSS服务器地址创建OSSClient对象。
		OSSClient client = new OSSClient(OSS_ENDPOINT, ACCESS_ID, ACCESS_KEY);
		ensureBucket(client, bucketName);

		try {
			setBucketPublicReadable(client, bucketName);

			System.out.println("正在上传...");
			uploadFile(client, bucketName, key, uploadFilePath);
			md5AndSha1 md5AndSha1 = getFileMD5(new File(uploadFilePath));
			System.out.println(md5AndSha1.md5 + "---" + md5AndSha1.sha1);
			// System.out.println("正在下载...");
			// downloadFile(client, bucketName, key, downloadFilePath);
		} finally {
			// deleteBucket(client, bucketName);
		}
	}

	static class md5AndSha1 {
		public String md5;
		public String sha1;

		md5AndSha1(String md5, String sha1) {
			this.md5 = md5;
			this.sha1 = sha1;
		}
	}

	public static md5AndSha1 getFileMD5(File file) {
		if (!file.isFile()) {
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] data;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
				bos.write(buffer, 0, len);
			}
			bos.flush();
			data = bos.toByteArray();
			bos.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		String sha1 = new SHA1().getDigestOfString(data);
		BigInteger bigInt = new BigInteger(1, digest.digest());
		String md5 = bigInt.toString(16).toUpperCase();
		md5AndSha1 md5AndSha1 = new md5AndSha1(md5, sha1);
		return md5AndSha1;
	}

	// 创建Bucket.
	private static void ensureBucket(OSSClient client, String bucketName)
			throws OSSException, ClientException {

		try {
			// 创建bucket
			client.createBucket(bucketName);
		} catch (ServiceException e) {
			if (!OSSErrorCode.BUCKES_ALREADY_EXISTS.equals(e.getErrorCode())) {
				// 如果Bucket已经存在，则忽略
				throw e;
			}
		}
	}

	// 删除一个Bucket和其中的Objects
	@SuppressWarnings("unused")
	private static void deleteBucket(OSSClient client, String bucketName)
			throws OSSException, ClientException {

		ObjectListing ObjectListing = client.listObjects(bucketName);
		List<OSSObjectSummary> listDeletes = ObjectListing.getObjectSummaries();
		for (int i = 0; i < listDeletes.size(); i++) {
			String objectName = listDeletes.get(i).getKey();
			// 如果不为空，先删除bucket下的文件
			client.deleteObject(bucketName, objectName);
		}
		client.deleteBucket(bucketName);
	}

	// 把Bucket设置为所有人可读
	private static void setBucketPublicReadable(OSSClient client,
			String bucketName) throws OSSException, ClientException {
		// 创建bucket
		client.createBucket(bucketName);

		// 设置bucket的访问权限，public-read-write权限
		client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
	}

	// 上传文件
	private static void uploadFile(OSSClient client, String bucketName,
			String key, String filename) throws OSSException, ClientException,
			FileNotFoundException {
		File file = new File(filename);

		ObjectMetadata objectMeta = new ObjectMetadata();
		objectMeta.setContentLength(file.length());
		// 可以在metadata中标记文件类型
		objectMeta.setContentType("image/jpeg");

		InputStream input = new FileInputStream(file);
		PutObjectResult putObjectResult = client.putObject(bucketName, key,
				input, objectMeta);
		String ETag = putObjectResult.getETag();// 新创建的OSSObject的ETag值,setETag
	}

	// 下载文件
	private static void downloadFile(OSSClient client, String bucketName,
			String key, String filename) throws OSSException, ClientException {
		ObjectMetadata objectMetadata = client.getObject(new GetObjectRequest(
				bucketName, key), new File(filename));// ObjectMetadata
														// OSS中Object的元数据
		System.out.println(objectMetadata.getETag());
	}
}
