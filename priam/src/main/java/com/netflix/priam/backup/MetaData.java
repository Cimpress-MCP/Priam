/*
 * Copyright 2013 Netflix, Inc.
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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.netflix.priam.IConfiguration;
import com.netflix.priam.backup.AbstractBackupPath.BackupFileType;
import com.netflix.priam.backup.IMessageObserver.BACKUP_MESSAGE_TYPE;
import com.netflix.priam.utils.RetryableCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to create a meta data file with a list of snapshot files. Also list the
 * contents of a meta data file.
 */
public class MetaData {
    private static final Logger logger = LoggerFactory.getLogger(MetaData.class);
    private final Provider<AbstractBackupPath> pathFactory;
    static List<IMessageObserver> observers = new ArrayList<IMessageObserver>();
    private final List<String> metaRemotePaths = new ArrayList<String>();
    private final IBackupFileSystem fs;

    @Inject
    public MetaData(Provider<AbstractBackupPath> pathFactory, IFileSystemContext backupFileSystemCtx, IConfiguration config)

    {
        this.pathFactory = pathFactory;
        this.fs = backupFileSystemCtx.getFileStrategy(config);
    }

    @SuppressWarnings("unchecked")
    public AbstractBackupPath set(List<AbstractBackupPath> bps, String snapshotName) throws Exception {
        File metafile = createTmpMetaFile();
        FileWriter fr = new FileWriter(metafile);
        try {
            JSONArray jsonObj = new JSONArray();
            for (AbstractBackupPath filePath : bps)
                jsonObj.add(filePath.getRemotePath());
            fr.write(jsonObj.toJSONString());
        } finally {
            IOUtils.closeQuietly(fr);
        }
        AbstractBackupPath backupfile = decorateMetaJson(metafile, snapshotName);
        try {
            upload(backupfile);

            addToRemotePath(backupfile.getRemotePath());
            if (metaRemotePaths.size() > 0) {
                notifyObservers();
            }
        } finally {
            FileUtils.deleteQuietly(metafile);
        }

        return backupfile;
    }

    /*
    From the meta.json to be created, populate its meta data for the backup file.
     */
    public AbstractBackupPath decorateMetaJson(File metafile, String snapshotName) throws ParseException {
        AbstractBackupPath backupfile = pathFactory.get();
        backupfile.parseLocal(metafile, BackupFileType.META);
        backupfile.time = backupfile.parseDate(snapshotName);
        return backupfile;
    }


    /*
     * Determines the existence of the backup meta file.  This meta file could be snapshot (meta.json) or 
     * incrementals (meta_keyspace_cf..json).
     * 
     * @param backup meta file to search
     * @return true if backup meta file exist, false otherwise.
     */
    public Boolean doesExist(final AbstractBackupPath meta) {
        try {
            new RetryableCallable<Void>() {
                @Override
                public Void retriableCall() throws Exception {
                    fs.download(meta, new FileOutputStream(meta.newRestoreFile())); //download actual file to disk
                    return null;
                }
            }.call();

        } catch (Exception e) {
            logger.error("Error downloading the Meta data try with a diffrent date...", e);
        }

        return meta.newRestoreFile().exists();

    }

    private void upload(final AbstractBackupPath bp) throws Exception {
        new RetryableCallable<Void>() {
            @Override
            public Void retriableCall() throws Exception {
                fs.upload(bp, bp.localReader());
                return null;
            }
        }.call();

        bp.setCompressedFileSize(fs.getBytesUploaded());
    }

    public File createTmpMetaFile() throws IOException {
        File metafile = File.createTempFile("meta", ".json");
        File destFile = new File(metafile.getParent(), "meta.json");
        if (destFile.exists())
            destFile.delete();
        FileUtils.moveFile(metafile, destFile);
        return destFile;
    }

    public static void addObserver(IMessageObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(IMessageObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (IMessageObserver observer : observers) {
            if (observer != null) {
                logger.debug("Updating snapshot observers now ...");
                observer.update(BACKUP_MESSAGE_TYPE.META, metaRemotePaths);
            } else
                logger.info("Observer is Null, hence can not notify ...");
        }
    }

    protected void addToRemotePath(String remotePath) {
        metaRemotePaths.add(remotePath);
    }

    public List<AbstractBackupPath> toJson(File input) {
        List<AbstractBackupPath> files = Lists.newArrayList();
        try {
            JSONArray jsonObj = (JSONArray) new JSONParser().parse(new FileReader(input));
            for (int i = 0; i < jsonObj.size(); i++) {
                AbstractBackupPath p = pathFactory.get();
                p.parseRemote((String) jsonObj.get(i));
                files.add(p);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error transforming file " + input.getAbsolutePath() + " to JSON format.  Msg:" + ex.getLocalizedMessage(), ex);
        }

        logger.debug("Transformed file {} to JSON.  Number of JSON elements: {}", input.getAbsolutePath(), files.size());
        return files;
    }
}
