package no.hvl.dat110.util;


/**
 * @author tdoy
 * dat110 - project 3
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.rpc.interfaces.NodeInterface;

public class FileManager {
	
	private static final Logger logger = LogManager.getLogger(FileManager.class);
	
	private BigInteger[] replicafiles;							// array stores replicated files for distribution to matching nodes
	private int numReplicas;									// let's assume each node manages nfiles (5 for now) - can be changed from the constructor
	private NodeInterface chordnode;
	private String filepath; 									// absolute filepath
	private String filename;									// only filename without path and extension
	private BigInteger hash;
	private byte[] bytesOfFile;
	private String sizeOfByte;
	
	private Set<Message> activeNodesforFile = null;
	
	public FileManager(NodeInterface chordnode) throws RemoteException {
		this.chordnode = chordnode;
	}
	
	public FileManager(NodeInterface chordnode, int N) throws RemoteException {
		this.numReplicas = N;
		replicafiles = new BigInteger[N];
		this.chordnode = chordnode;
	}
	
	public FileManager(NodeInterface chordnode, String filepath, int N) throws RemoteException {
		this.filepath = filepath;
		this.numReplicas = N;
		replicafiles = new BigInteger[N];
		this.chordnode = chordnode;
	}

	public void createReplicaFiles() {

		if (filename == null) {
			System.err.println("Error: Filename not initialized.");
			return;
		}

		replicafiles = new BigInteger[numReplicas];

		for(int i = 0; i < numReplicas; i++) {

			String replicaFileName = filename + i;
			BigInteger hash = Hash.hashOf(replicaFileName);

			replicafiles[i] = hash;
		}
	}
	
    /**
     * 
     * @param bytesOfFile
     * @throws RemoteException 
     */
    public int distributeReplicastoPeers() throws RemoteException {

		if (filename == null) {
			System.err.println("Error: Filename not initialized.");
			return 0;
		}

		Random rnd = new Random();
		int primaryIndex = rnd.nextInt(numReplicas); // Choose a random index for the primary replica

		int counter = 0;

		for (int i = 0; i < numReplicas; i++) {
			BigInteger replicaKey = replicafiles[i];

			// Find the successor (peer) responsible for this replicaKey
			NodeInterface successor = chordnode.findSuccessor(replicaKey);

			// Check if the replica should be added to this successor's responsibility
			NodeInterface predecessor = successor.getPredecessor();
			BigInteger predId = predecessor.getNodeID();
			BigInteger peerId = successor.getNodeID();

			if (predId.compareTo(replicaKey) < 0 && replicaKey.compareTo(peerId) <= 0) {
				// Add the replica to the peer's responsibility
				successor.addKey(replicaKey);

				// Determine if this replica is the primary
				boolean isPrimary = (i == primaryIndex);

				// Get the bytes of the file content (replace this with your method to retrieve file content)
				byte[] bytesOfFile = getBytesOfFile();

				// Save the file content on the successor with primary status
				successor.saveFileContent(filename, replicaKey, bytesOfFile, isPrimary);

				// Increment counter for each successfully distributed replica
				counter++;
			}
		}

		return counter;
	}
	
	/**
	 * 
	 * @param filename
	 * @return list of active nodes having the replicas of this file
	 * @throws RemoteException 
	 */
	public Set<Message> requestActiveNodesForFile(String filename) throws RemoteException {

		this.filename = filename;
		activeNodesforFile = new HashSet<Message>();
		createReplicaFiles();

		for (int i = 0; i < numReplicas; i++) {
			BigInteger replicaKey = replicafiles[i];

			// Find the successor (peer) responsible for this replicaKey
			NodeInterface successor = chordnode.findSuccessor(replicaKey);

			// Get metadata (Message) of the replica from the successor (active peer)
			Message replicaMetadata = successor.getFilesMetadata(replicaKey);

			// Save the metadata in the set of activeNodesForFile
			activeNodesforFile.add(replicaMetadata);
		}

		return activeNodesforFile;
	}
	
	/**
	 * Find the primary server - Remote-Write Protocol
	 * @return 
	 */
	public NodeInterface findPrimaryOfItem() {

		if (activeNodesforFile == null || activeNodesforFile.isEmpty()) {
			System.err.println("No active nodes for the file.");
			return null;
		}

		for (Message message : activeNodesforFile) {
			if (message.isPrimaryServer()) {

				return Util.getProcessStub(message.getNodeName(), message.getPort());
			}
		}

		System.err.println("Primary not found among active nodes for the file.");
		return null;
	}
	
    /**
     * Read the content of a file and return the bytes
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    public void readFile() throws IOException, NoSuchAlgorithmException {
    	
    	File f = new File(filepath);
    	
    	byte[] bytesOfFile = new byte[(int) f.length()];
    	
		FileInputStream fis = new FileInputStream(f);
        
        fis.read(bytesOfFile);
		fis.close();
		
		//set the values
		filename = f.getName().replace(".txt", "");		
		hash = Hash.hashOf(filename);
		this.bytesOfFile = bytesOfFile;
		double size = (double) bytesOfFile.length/1000;
		NumberFormat nf = new DecimalFormat();
		nf.setMaximumFractionDigits(3);
		sizeOfByte = nf.format(size);
		
		logger.info("filename="+filename+" size="+sizeOfByte);
    	
    }
    
    public void printActivePeers() {
    	
    	activeNodesforFile.forEach(m -> {
    		String peer = m.getNodeName();
    		String id = m.getNodeID().toString();
    		String name = m.getNameOfFile();
    		String hash = m.getHashOfFile().toString();
    		int size = m.getBytesOfFile().length;
    		
    		logger.info(peer+": ID = "+id+" | filename = "+name+" | HashOfFile = "+hash+" | size ="+size);
    		
    	});
    }

	/**
	 * @return the numReplicas
	 */
	public int getNumReplicas() {
		return numReplicas;
	}
	
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	/**
	 * @return the hash
	 */
	public BigInteger getHash() {
		return hash;
	}
	/**
	 * @param hash the hash to set
	 */
	public void setHash(BigInteger hash) {
		this.hash = hash;
	}
	/**
	 * @return the bytesOfFile
	 */ 
	public byte[] getBytesOfFile() {
		return bytesOfFile;
	}
	/**
	 * @param bytesOfFile the bytesOfFile to set
	 */
	public void setBytesOfFile(byte[] bytesOfFile) {
		this.bytesOfFile = bytesOfFile;
	}
	/**
	 * @return the size
	 */
	public String getSizeOfByte() {
		return sizeOfByte;
	}
	/**
	 * @param size the size to set
	 */
	public void setSizeOfByte(String sizeOfByte) {
		this.sizeOfByte = sizeOfByte;
	}

	/**
	 * @return the chordnode
	 */
	public NodeInterface getChordnode() {
		return chordnode;
	}

	/**
	 * @return the activeNodesforFile
	 */
	public Set<Message> getActiveNodesforFile() {
		return activeNodesforFile;
	}

	/**
	 * @return the replicafiles
	 */
	public BigInteger[] getReplicafiles() {
		return replicafiles;
	}

	/**
	 * @param filepath the filepath to set
	 */
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
}