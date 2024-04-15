/**
 * 
 */
package no.hvl.dat110.middleware;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.LamportClock;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 *
 */
public class MutualExclusion {
		
	private static final Logger logger = LogManager.getLogger(MutualExclusion.class);
	/** lock variables */
	private boolean CS_BUSY = false;						// indicate to be in critical section (accessing a shared resource) 
	private boolean WANTS_TO_ENTER_CS = false;				// indicate to want to enter CS
	private List<Message> queueack; 						// queue for acknowledged messages
	private List<Message> mutexqueue;						// queue for storing process that are denied permission. We really don't need this for quorum-protocol
	
	private LamportClock clock;								// lamport clock
	private Node node;
	
	public MutualExclusion(Node node) throws RemoteException {
		this.node = node;
		
		clock = new LamportClock();
		queueack = new ArrayList<Message>();
		mutexqueue = new ArrayList<Message>();
	}
	
	public synchronized void acquireLock() {
		CS_BUSY = true;
	}
	
	public void releaseLocks() {
		WANTS_TO_ENTER_CS = false;
		CS_BUSY = false;
	}

	public boolean doMutexRequest(Message message, byte[] updates) throws RemoteException {

		logger.info(node.nodename + " wants to access CS");

		// Clear the acknowledgment queue and mutex queue
		queueack.clear();
		mutexqueue.clear();

		// Increment the local clock
		clock.increment();

		// Adjust the clock on the message
		message.setClock(clock.getClock());

		// Set the appropriate lock variable
		// Assuming 'isAccessingCS' is a boolean indicating whether the node is accessing the critical section
		WANTS_TO_ENTER_CS = true;

		// Start MutualExclusion algorithm
		removeDuplicatePeersBeforeVoting();

		// Multicast the message to active nodes
		multicastMessage(message, queueack);

		// Check that all replicas have replied (permission)
		if (areAllMessagesReturned(queueack.size())) {
			// Acquire lock and send updates to all replicas
			node.broadcastUpdatetoPeers(updates);
			mutexqueue.clear(); // Clear the mutex queue after updates are sent
			return true; // Permission granted
		}

		return false;
	}
	
	// multicast message to other processes including self
	private void multicastMessage(Message message, List<Message> activenodes) throws RemoteException {
		
		logger.info("Number of peers to vote = "+activenodes.size());

		for (Message peer : activenodes) {
			NodeInterface nodeStub = Util.getProcessStub(peer.getNodeName(), peer.getPort());
			nodeStub.onMutexRequestReceived(message);
		}
		
	}
	
	public void onMutexRequestReceived(Message message) throws RemoteException {
		
		// increment the local clock
		
		// if message is from self, acknowledge, and call onMutexAcknowledgementReceived()
			
		int caseid = -1;
		
		/* write if statement to transition to the correct caseid in the doDecisionAlgorithm */
		
			// caseid=0: Receiver is not accessing shared resource and does not want to (send OK to sender)
		
			// caseid=1: Receiver already has access to the resource (dont reply but queue the request)
		
			// caseid=2: Receiver wants to access resource but is yet to - compare own message clock to received message's clock
		
		// check for decision
		doDecisionAlgorithm(message, mutexqueue, caseid);
	}
	
	public void doDecisionAlgorithm(Message message, List<Message> queue, int condition) throws RemoteException {

		String procName = message.getNodeName();
		int port = message.getPort();

		switch (condition) {

			/** case 1: Receiver is not accessing shared resource and does not want to (send OK to sender) */
			case 0: {

				// get a stub for the sender from the registry

				// acknowledge message

				// send acknowledgement back by calling onMutexAcknowledgementReceived()

				break;
			}

			/** case 2: Receiver already has access to the resource (dont reply but queue the request) */
			case 1: {

				// queue this message
				break;
			}

			/**
			 *  case 3: Receiver wants to access resource but is yet to (compare own message clock to received message's clock
			 *  the message with lower timestamp wins) - send OK if received is lower. Queue message if received is higher
			 */
			case 2: {

				// check the clock of the sending process (note that the correct clock is in the received message)

				// own clock of the receiver (note that the correct clock is in the node's message)

				// compare clocks, the lowest wins

				// if clocks are the same, compare nodeIDs, the lowest wins

				// if sender wins, acknowledge the message, obtain a stub and call onMutexAcknowledgementReceived()

				// if sender looses, queue it

				break;
			}

			default:
				break;
		}
	}


		public void onMutexAcknowledgementReceived (Message message) throws RemoteException {

			// add message to queueack

		}

		// multicast release locks message to other processes including self
		public void multicastReleaseLocks (Set < Message > activenodes) {
			logger.info("Releasing locks from = " + activenodes.size());

			// iterate over the activenodes

			// obtain a stub for each node from the registry

			// call releaseLocks()
		}

		private boolean areAllMessagesReturned ( int numvoters) throws RemoteException {
			logger.info(node.getNodeName() + ": size of queueack = " + queueack.size());

			// check if the size of the queueack is the same as the numvoters

			// clear the queueack

			// return true if yes and false if no

			return false;
		}

		private List<Message> removeDuplicatePeersBeforeVoting () {

			List<Message> uniquepeer = new ArrayList<Message>();
			for (Message p : node.activenodesforfile) {
				boolean found = false;
				for (Message p1 : uniquepeer) {
					if (p.getNodeName().equals(p1.getNodeName())) {
						found = true;
						break;
					}
				}
				if (!found)
					uniquepeer.add(p);
			}
			return uniquepeer;
		}
	}