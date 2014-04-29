/**
 * 
 */
package com.petpet.c3po.adaptor.tika;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;

import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;

/**
 * @author wpalmer
 *
 */
public class TIKASequenceFileReader {

	private final static Logger LOG = Logger.getLogger(TIKASequenceFileReader.class);
	
    /**
     * Sequence file from Hadoop
     */
    public static final String SEQ_FILE = ".seqfile";
    
	/**
	 * Extract files from a sequence file 
	 * NOTE: this is reliant on the format used within Nanite
	 * @param filePath
	 * @param tmp
	 */
	public static void extract(String filePath, String tmp) {
		
		File outputDir = new File(tmp);
		
		if(!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		SequenceFile.Reader reader = open(filePath);
		if(null==reader) return;
		
		LOG.info("Extracting SequenceFile...");
		LOG.info("Info: "+reader.toString());
		LOG.info("Compression: "+reader.getCompressionType()+" "+reader.getCompressionCodec().getClass().getName());
		
		Text key = new Text();
		Text value = new Text();
		
		try {
			int count = 0;
			while(reader.next(key, value)) {
				// de-serialise the metadata object
				try {
					ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decodeBase64(value.getBytes())));
					Object o = null;
					try {
						o = ois.readObject();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ois.close();
					Metadata metadata = null;
					if(o instanceof Metadata) {
						metadata = (Metadata)o;
					}
					if(metadata!=null) {
						//LOG.info("Metadata object reconstructed");
						// now save the Metadata object as text in the tmp directory
						// output to mimic "java -jar tika-app.jar -m file"
						try {
							metadataToFile(count, metadata, tmp);
						} catch(IOException e2) {
							LOG.error("Could not write metadata to file");
							e2.printStackTrace();
						}
					}
				} catch(StreamCorruptedException e) {
					LOG.error("Cannot deserialize Metadata object: "+e.getMessage());
				}
				count++;
			}
			LOG.info("Extracted objects: "+count);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			LOG.info(e1.getMessage());
			e1.printStackTrace();
		}
		
		if(reader!=null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private static void metadataToFile(int count, Metadata metadata, String tmp) throws IOException {
		File output = new File(tmp+"/"+String.format("%08d", count)+".txt");
		// don't overwrite an existing file
		if(output.exists()) return;
		
		PrintWriter pw = new PrintWriter(new FileWriter(output));
		
		String[] names = metadata.names();
		for(String name : names) {
			for(String value : metadata.getValues(name)) {
				pw.println(name+": "+value);
			}
		}
		
		pw.close();

	}

	private static SequenceFile.Reader open(String filePath) {
		File file = new File(filePath);
		if(!file.exists()) return null;
		SequenceFile.Reader reader = null;
		try {
			reader = new SequenceFile.Reader(new Configuration(), Reader.file(new Path(file.getAbsolutePath())));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(null==reader) return null;
		
		return reader;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		extract("test.seqfile", "./tmp/");
	}
	
}
