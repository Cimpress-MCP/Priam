/**
 * Copyright 2017 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.priam.restore;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.netflix.priam.ICassandraProcess;
import com.netflix.priam.IConfiguration;
import com.netflix.priam.ICredentialGeneric;
import com.netflix.priam.backup.*;
import com.netflix.priam.backup.AbstractBackupPath.BackupFileType;
import com.netflix.priam.compress.ICompression;
import com.netflix.priam.cryptography.IFileCryptography;
import com.netflix.priam.health.InstanceState;
import com.netflix.priam.identity.InstanceIdentity;
import com.netflix.priam.scheduler.NamedThreadPoolExecutor;
import com.netflix.priam.utils.RetryableCallable;
import com.netflix.priam.utils.Sleeper;
import com.netflix.priam.utils.SystemUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides common functionality applicable to all restore strategies
 */
public abstract class EncryptedRestoreBase extends AbstractRestore{
    private static final Logger logger = LoggerFactory.getLogger(EncryptedRestoreBase.class);

    private String jobName;
    private ICredentialGeneric pgpCredential;
    private IFileCryptography fileCryptography;
    private ICompression compress;
    private final ThreadPoolExecutor executor;
    private AtomicInteger count = new AtomicInteger();

    protected EncryptedRestoreBase(IConfiguration config, IBackupFileSystem fs, String jobName, Sleeper sleeper,
                                   ICassandraProcess cassProcess, Provider<AbstractBackupPath> pathProvider,
                                   InstanceIdentity instanceIdentity, RestoreTokenSelector tokenSelector, ICredentialGeneric pgpCredential,
                                   IFileCryptography fileCryptography, ICompression compress, MetaData metaData, InstanceState instanceState) {
        super(config, fs, jobName, sleeper, pathProvider, instanceIdentity, tokenSelector, cassProcess, metaData, instanceState);

        this.jobName = jobName;
        this.pgpCredential = pgpCredential;
        this.fileCryptography = fileCryptography;
        this.compress = compress;
        executor = new NamedThreadPoolExecutor(config.getMaxBackupDownloadThreads(), jobName);
        executor.allowCoreThreadTimeOut(true);
        logger.info("Trying to restore cassandra cluster with filesystem: {}, RestoreStrategy: {}, Encryption: ON, Compression: {}",
                fs.getClass(), jobName, compress.getClass());
    }

    @Override
    protected final void downloadFile(final AbstractBackupPath path, final File restoreLocation) throws  Exception{
        final char[] passPhrase = new String(this.pgpCredential.getValue(ICredentialGeneric.KEY.PGP_PASSWORD)).toCharArray();
        File tempFile = new File(restoreLocation.getAbsolutePath() + ".tmp");
        count.incrementAndGet();

        try {
            executor.submit(new RetryableCallable<Integer>() {

                @Override
                public Integer retriableCall() throws Exception {

                    //== download object from source bucket
                    try {

                        logger.info("Downloading file from: {} to: {}", path.getRemotePath(), tempFile.getAbsolutePath());
                        fs.download(path, new FileOutputStream(tempFile), tempFile.getAbsolutePath());
                        tracker.adjustAndAdd(path);
                        logger.info("Completed downloading file from: {} to: {}", path.getRemotePath(), tempFile.getAbsolutePath());


                    } catch (Exception ex) {
                        //This behavior is retryable; therefore, lets get to a clean state before each retry.
                        if (tempFile.exists()) {
                            tempFile.createNewFile();
                        }

                        throw new Exception("Exception downloading file from: " + path.getRemotePath() + " to: " + tempFile.getAbsolutePath(), ex);
                    }

                    //== object downloaded successfully from source, decrypt it.
                    File decryptedFile = new File(tempFile.getAbsolutePath() + ".decrypted");
                    try(OutputStream fOut = new BufferedOutputStream(new FileOutputStream(decryptedFile)); //destination file after decryption)
                        InputStream in    = new BufferedInputStream(new FileInputStream(tempFile.getAbsolutePath()))) {
                        InputStream encryptedDataInputStream = fileCryptography.decryptStream(in, passPhrase, tempFile.getAbsolutePath());
                        Streams.pipeAll(encryptedDataInputStream, fOut);
                        logger.info("Completed decrypting file: {} to final file dest: {}", tempFile.getAbsolutePath(), decryptedFile.getAbsolutePath());

                    } catch (Exception ex) {
                        //This behavior is retryable; therefore, lets get to a clean state before each retry.
                        if (tempFile.exists()) {
                            tempFile.createNewFile();
                        }

                        if (decryptedFile.exists()) {
                            decryptedFile.createNewFile();
                        }

                        throw new Exception("Exception during decryption file:  " + decryptedFile.getAbsolutePath(), ex);
                    }

                    //== object downloaded and decrypted successfully, now uncompress it
                    logger.info("Start uncompressing file: {} to the FINAL destination stream", decryptedFile.getAbsolutePath());

                    try(InputStream is = new BufferedInputStream(new FileInputStream(decryptedFile));
                    BufferedOutputStream finalDestination = new BufferedOutputStream(new FileOutputStream(restoreLocation))) {
                        compress.decompressAndClose(is, finalDestination);
                    } catch (Exception ex) {
                        throw new Exception("Exception uncompressing file: " + decryptedFile.getAbsolutePath() + " to the FINAL destination stream", ex);
                    }

                    logger.info("Completed uncompressing file: {} to the FINAL destination stream "
                            + " current worker: {}", decryptedFile.getAbsolutePath(), Thread.currentThread().getName());
                    //if here, everything was successful for this object, lets remove unneeded file(s)
                    if (tempFile.exists())
                        tempFile.delete();

                    if (decryptedFile.exists()) {
                        decryptedFile.delete();
                    }

                    return count.decrementAndGet();
                }

            });
        }catch (Exception e){
            throw new Exception("Exception in download of:  " + path.getFileName() + ", msg: " + e.getLocalizedMessage(), e);
        }

    }

    @Override
    protected final void waitToComplete() {
        while (count.get() != 0) {
            try {
                sleeper.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Interrupted: ", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public String getName() {
        return this.jobName;
    }
}