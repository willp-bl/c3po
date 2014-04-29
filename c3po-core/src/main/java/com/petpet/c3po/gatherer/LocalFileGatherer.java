/*******************************************************************************
 * Copyright 2013 Petar Petrov <me@petarpetrov.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.petpet.c3po.gatherer;

import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.petpet.c3po.adaptor.tika.TIKASequenceFileReader;
import com.petpet.c3po.api.gatherer.MetaDataGatherer;
import com.petpet.c3po.api.model.helper.MetadataStream;
import com.petpet.c3po.common.Constants;

/**
 * A gatherer of a local file system. It is a {@link Runnable} class that reads
 * the meta data files into memory and stores them into a processing queue.
 *
 * @author Petar Petrov <me@petarpetrov.org>
 *
 */
public class LocalFileGatherer implements MetaDataGatherer {

    /**
     * A default logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( LocalFileGatherer.class );

    /**
     * A static integer for tmp folders.
     */
    private static int folder = 0;

    /**
     * A list of supported archive extensions.
     */
    private static final String[] ARCHIVE_EXTENSIONS = { ".zip", ".tar", ".tar.gz", ".tgz", ".gz", TIKASequenceFileReader.SEQ_FILE};

    /**
     * The configuration of the gatherer.
     */
    private Map<String, String> config;

    /**
     * A queue where the {@link FileMetadataStream} objects are stored.
     */
    private final LinkedBlockingQueue<MetadataStream> queue;

    /**
     * The count of objects gathered.
     */
    private long count;

    /**
     * A flag denoting whether this gatherer is ready traversing the file system.
     */
    private boolean ready;

    /**
     * A lock for synchronization with other workers.
     */
    private Object lock;

    /**
     * Creates a new gatherer.
     */
    public LocalFileGatherer() {
        this.queue = new LinkedBlockingQueue<MetadataStream>(10000);
        this.ready = false;
    }

    /**
     * Creates a new gatherer with the given config.
     *
     * @param config
     */
    public LocalFileGatherer(Map<String, String> config) {
        this();
        this.config = config;
    }

    /**
     * Creates a new gatherer with the given object lock.
     *
     * @param lock
     */
    public LocalFileGatherer(Object lock) {
        this();
        this.lock = lock;
    }

    @Override
    public LinkedBlockingQueue<MetadataStream> getQueue(){
        return queue;
    }

    /**
     * Runs this gatherer and traverses the file system (optionally in a recursive
     * fashion) . Once the files are gathered, all waiting threads on the locks
     * monitor are notified.
     */
    @Override
    public synchronized void run() {
        String path = this.config.get( Constants.OPT_COLLECTION_LOCATION );
        boolean recursive = Boolean.valueOf( this.config.get( Constants.OPT_RECURSIVE ) );

        this.ready = false;

        this.traverseFiles( new File( path ), recursive, true );
        System.out.println( this.count + " files were submitted for processing" );
        LOG.info( "{} files were submitted for processing successfully", this.count );


        //synchronized ( lock ) {
        //  this.lock.notifyAll();

        // }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized MetadataStream getNext() {
        return queue.poll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig( Map<String, String> config ) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        return this.ready;
    }

    /**
     * Traverses the file system starting from the given file. If the recursive
     * flag is true, then the traversal is recursive. Processes files and
     * archives. If an archive is encountered, then the it is extracted in a tmp
     * direcotory, which is then traversed recursively. At the end of the process
     * the tmp directory is removed.
     *
     * @param file
     *          the directory to traverse.
     * @param recursive
     *          whether or not to do it recursively.
     * @param firstLevel
     *          denotes whether this is the first level of traversal.
     */
    private void traverseFiles( File file, boolean recursive, boolean firstLevel ) {

        if ( file.isDirectory() && (recursive || firstLevel) ) {

            File[] files = file.listFiles();
            for ( File f : files ) {
                traverseFiles( f, recursive, false );
            }
        } else {
            String filePath = file.getAbsolutePath();

            if ( isArchive( filePath ) ) {

                extractArchive( filePath );

            } else {

                submitMetadataResult( filePath );

            }
        }

    }

    /**
     * Checks if the file denoted by the given name is an archive based on the
     * supported extensions.
     *
     * @param name
     *          the name to check.
     * @return true if it is an archive, false otherwise.
     */
    private boolean isArchive( String name ) {
        for ( String ext : ARCHIVE_EXTENSIONS ) {
            if ( name.endsWith( ext ) ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Processes an archive file denoted by the given file path.
     *
     * @param filePath
     *          the file path to the archive.
     */
    private void extractArchive( String filePath ) {
        String tmp = FileUtils.getTempDirectory().getPath() + File.separator + "c3poarchives" + File.separator + folder++;

        File tmpDir = new File( tmp );
        tmpDir.mkdirs();

        if(filePath.toLowerCase().endsWith(TIKASequenceFileReader.SEQ_FILE)) {
        	// We have a Tika SequenceFile from Hadoop
        	TIKASequenceFileReader.extract( filePath, tmp);
        } else {
        	FileExtractor.extract( filePath, tmp );
        }
        traverseFiles( tmpDir, true, true );

    }

    private void submitMetadataResult( String filePath ) {
        FileMetadataStream ms = new FileMetadataStream( filePath );
        try {
            this.queue.put( ms );
            count++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!this.ready)
        {
            this.ready=true;
        }
        if ( (this.count % 10000) == 0 ) {
            LOG.info( "{} files were submitted for processing", this.count );
            System.out.println( this.count + " files were submitted for processing" );
            //  synchronized ( lock ) {
            //    this.lock.notify();
            //
            // }
        }

        //if ( this.queue.size() > 10000 && this.count % 1000 == 0 ) {
        //  synchronized ( lock ) {
        //    this.lock.notifyAll();
        //  }
        // }

        // if ( this.count % 10000 == 0 ) {
        //    System.out.println( this.count + " files were submitted for processing" );
        //  }

    }
}
