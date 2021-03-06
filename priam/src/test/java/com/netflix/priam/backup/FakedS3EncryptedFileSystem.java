/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.priam.backup;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Iterator;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.priam.IConfiguration;
import com.netflix.priam.ICredential;
import com.netflix.priam.compress.ICompression;
import com.netflix.priam.cryptography.IFileCryptography;

@Singleton
public class FakedS3EncryptedFileSystem implements IBackupFileSystem {

	@Inject
	public FakedS3EncryptedFileSystem( Provider<AbstractBackupPath> pathProvider, ICompression compress, final IConfiguration config, ICredential cred
	, @Named("filecryptoalgorithm") IFileCryptography fileCryptography
	) {
		
	}
	
	@Override
	public void download(AbstractBackupPath path, OutputStream os)
			throws BackupRestoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void download(AbstractBackupPath path, OutputStream os,
			String filePath) throws BackupRestoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void upload(AbstractBackupPath path, InputStream in)
			throws BackupRestoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public Iterator<AbstractBackupPath> list(String path, Date start, Date till) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<AbstractBackupPath> listPrefixes(Date date) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getActivecount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public long getBytesUploaded() {
		return 0;
	}

	@Override
	public int getAWSSlowDownExceptionCounter() {
		return 0;
	}

}
