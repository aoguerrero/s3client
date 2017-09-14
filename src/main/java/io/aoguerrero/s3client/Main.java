package io.aoguerrero.s3client;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {

		/* S3 Connection */
		BasicAWSCredentials credentials = new BasicAWSCredentials(Config.getInstance().getValue("accessKey"),
				Config.getInstance().getValue("secretKey"));
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(Config.getInstance().getValue("region")).build();
		S3Client s3Client = new S3Client(amazonS3);

		/* Local files list */
		String localDirName = System.getProperty("user.dir");
		File localDir = new File(localDirName);
		List<String> localFileNames = new ArrayList<String>();
		localDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (!file.isDirectory()) {
					String fileName = file.getName();
					localFileNames.add(fileName);
					logger.info("Local file " + fileName);
				}
				return false;
			}
		});

		/* Remote files list */
		String directoryName = Config.getInstance().getValue("bucket");		
		List<String> remoteFileNames = s3Client.listFiles(directoryName);

		/* Upload and delete */
		localFileNames.removeAll(remoteFileNames);
		List<String> deletedFileNames = new ArrayList<String>();
		List<String> excludedFiles = Arrays.asList(Config.getInstance().getValue("exclude").split(","));
		for (String fileName : localFileNames) {
			if (!fileName.endsWith(".delete")) {
				if (!excludedFiles.contains(fileName)) {
					String filePath = localDirName + File.separator + fileName;
					s3Client.uploadFile(directoryName, fileName, filePath);
				}
			} else {
				String fileNameToDelete = fileName.substring(0, fileName.length() - 7);
				s3Client.deleteFile(directoryName, fileNameToDelete);
				deletedFileNames.add(fileNameToDelete);
				File file = new File(fileName);
				file.delete();
			}
		}
		remoteFileNames.removeAll(deletedFileNames);
		
		/* Download */
		for (String fileName : remoteFileNames) {
			String filePath = localDirName + File.separator + fileName;
			File file = new File(filePath);
			if (!file.exists()) {
				s3Client.downloadFile(directoryName, fileName, filePath);
			}
		}
	}

}
