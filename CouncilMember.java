// CouncilMember.java
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Represents a council member in the Adelaide Suburbs Council.
 * Implements the Paxos protocol for consensus on council president election.
 */
public class CouncilMember {
    private final int id;
    private final String name;
    private final int port;
    private ResponseProfile profile;
    private ServerSocket serverSocket;
    private final Map<Integer, ProposalValue> acceptedProposals;
    private int currentProposalNum;
    private int acceptedValue;
    private boolean isRunning;
    private final List<String> peerAddresses;
    private static final double UNRELIABLE_FAILURE_RATE = 0.3;
    private static final long SMALL_DELAY_MS = 100;
    private static final long LARGE_DELAY_MS = 1000;
    private static final long MAX_RANDOM_DELAY_MS = 2000;
    private static final int SOCKET_TIMEOUT = 5000;
    
    private static class ProposalValue {
        int proposalNum;
        int value;
        
        ProposalValue(int proposalNum, int value) {
            this.proposalNum = proposalNum;
            this.value = value;
        }

        int getProposalNum() {
            return proposalNum;
        }

        int getValue() {
            return value;
        }
    }

    public int getId() {
        return id;
    }

    public ResponseProfile getResponseProfile() {
        return profile;
    }

    public void setResponseProfile (ResponseProfile profile) {
        this.profile = profile;
    }
    
    public enum ResponseProfile {
        IMMEDIATE,
        SMALL_DELAY,
        LARGE_DELAY,
        UNRELIABLE
    }
    
    /**
     * Creates a new council member with specified characteristics
     */
    public CouncilMember(int id, String name, int port, ResponseProfile profile) {
        this.id = id;
        this.name = name;
        this.port = port;
        this.profile = profile;
        this.acceptedProposals = new HashMap<>();
        this.currentProposalNum = 0;
        this.acceptedValue = -1;
        this.isRunning = true;
        this.peerAddresses = new ArrayList<>();
        // Initialize peer addresses (other council members)
        for (int i = 1; i <= 9; i++) {
            if (i != id) {
                peerAddresses.add("localhost:" + (8000 + i));
            }
        }
    }
    
    /**
     * Starts the council member's server to listen for messages
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            new Thread(this::listen).start();
        } catch (IOException e) {
            System.err.println("Failed to start council member " + name + ": " + e.getMessage());
        }
    }
    
    /**
     * Listens for incoming messages and processes them according to Paxos protocol
     */
    private void listen() {
        while (isRunning) {
            Socket client = null;
            try {
                client = serverSocket.accept();
                handleClientConnection(client);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting connection for " + name + ": " + e.getMessage());
                }
            } finally {
                if (client != null && !client.isClosed()) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client socket: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void handleClientConnection(Socket client) {
        try (ObjectInputStream in = new ObjectInputStream(client.getInputStream()); 
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())) {
            
            Message message = (Message) in.readObject();
            processMessage(message, out);

        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                System.err.println("Error processing message for " + name + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Processes incoming messages according to the Paxos protocol
     */
    private void processMessage(Message message, ObjectOutputStream out) throws IOException {
        // Simulate response delays based on profile
        simulateResponseDelay();
        
        switch (message.getType()) {
            case PREPARE -> handlePrepare(message, out);
            case ACCEPT -> handleAccept(message, out);
            case LEARN -> handleLearn(message);
            case PROMISE, ACCEPTED, NACK -> {
                // These message types are handled by the proposer
                System.err.println("Unexpected message type received: " + message.getType());
            }
        }
    }
    
    /**
     * Simulates response delays based on member's profile
     */
    private void simulateResponseDelay() throws IOException {
        try {
            switch (profile) {
                case SMALL_DELAY -> Thread.sleep(SMALL_DELAY_MS);
                case LARGE_DELAY -> Thread.sleep(LARGE_DELAY_MS);
                case UNRELIABLE -> {
                    if (Math.random() < UNRELIABLE_FAILURE_RATE) { // 30% chance of not responding
                        throw new IOException("Connection lost");
                    }
                    Thread.sleep((long) (Math.random() * MAX_RANDOM_DELAY_MS));
                }
                case IMMEDIATE -> { /* No delay */ }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during delay simulation", e);
        }
    }
    
    /**
     * Sends prepare messages to all other members
     */
    private List<Message> sendPrepare(int proposalNum) {
        List<Message> responses = new ArrayList<>();
        for (String peerAddress : peerAddresses) {
            try {
                String[] parts = peerAddress.split(":");
                String host = parts[0];
                int peerPort = Integer.parseInt(parts[1]);
                
                try (Socket socket = new Socket(host, peerPort)) {
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    
                    Message prepareMsg = new Message(MessageType.PREPARE, id, proposalNum, -1);
                    out.writeObject(prepareMsg);
                    
                    Message response = (Message) in.readObject();
                    if (response.getType() == MessageType.PROMISE) {
                        responses.add(response);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                // Handle connection failures - peer might be offline
                System.err.println("Failed to send prepare to " + peerAddress + ": " + e.getMessage());
            }
        }
        return responses;
    }
    
    /**
     * Sends accept messages to all other members
     */
    private List<Message> sendAccept(int proposalNum, int value) {
        List<Message> responses = new ArrayList<>();
        for (String peerAddress : peerAddresses) {
            try {
                String[] parts = peerAddress.split(":");
                String host = parts[0];
                int peerPort = Integer.parseInt(parts[1]);
                
                try (Socket socket = new Socket(host, peerPort)) {
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    
                    Message acceptMsg = new Message(MessageType.ACCEPT, id, proposalNum, value);
                    out.writeObject(acceptMsg);
                    
                    Message response = (Message) in.readObject();
                    if (response.getType() == MessageType.ACCEPTED) {
                        responses.add(response);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Failed to send accept to " + peerAddress + ": " + e.getMessage());
            }
        }
        return responses;
    }
    
    /**
     * Broadcasts learn messages to all other members
     */
    private void broadcastLearn(int value) {
        for (String peerAddress : peerAddresses) {
            try {
                String[] parts = peerAddress.split(":");
                String host = parts[0];
                int peerPort = Integer.parseInt(parts[1]);
                
                try (Socket socket = new Socket(host, peerPort);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    
                    Message learnMsg = new Message(MessageType.LEARN, id, currentProposalNum, value);
                    out.writeObject(learnMsg);
                }
            } catch (IOException e) {
                System.err.println("Failed to send learn to " + peerAddress + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Proposes self as council president
     * @return true if successfully elected, false otherwise
     */
    public boolean proposePresident() {
        System.out.println(name + " starting proposal process...");
        currentProposalNum = generateProposalNumber();

        // Phase 1: Prepare
        System.out.println(name + " sending prepare messages with proposal number " + currentProposalNum);
        List<Message> promises = sendPrepare(currentProposalNum);
        System.out.println(name + " received " + promises.size() + " promises");
        
        if (promises.size() > Council.MAJORITY_SIZE) {
            // Find highest accepted value among responses
            int highestValue = -1;
            int highestProposal = -1;

            for (Message promise : promises) {
                if (promise.getProposalNumber() > highestProposal) {
                    highestProposal = promise.getProposalNumber();
                    highestValue = promise.getValue();
                }
            }

            // If no value was previously accepted, propose self
            int valueToPropose = (highestValue == -1) ? id : highestValue;

            // Phase 2: Accept
            System.out.println(name + " sending accept messages with value " + valueToPropose);
            List<Message> accepts = sendAccept(currentProposalNum, valueToPropose);
            System.out.println(name + " received " + accepts.size() + " accepts");

            if (accepts.size() >= Council.MAJORITY_SIZE) {
                System.out.println(name + " achieved consensus with value " + valueToPropose);
                acceptedValue = valueToPropose;
                broadcastLearn(valueToPropose);
                return true;
            }
        }
        System.out.println(name + " failed to achieve consensus");
        return false;
    }
    
    /**
     * Generates a unique proposal number
     */
    private int generateProposalNumber() {
        return (int) (System.currentTimeMillis() * 1000 + id);
    }
    
    /**
     * Stops the council member's server
     */
    public void stop() {
        isRunning = false;
        try {
            // First close the server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // Small delay to allow pending operations to complete
            Thread.sleep(100);
        } catch (IOException e) {
            System.err.println("Error stopping " + name + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while stopping " + name);
        }
    }
    
    private void handlePrepare(Message message, ObjectOutputStream out) throws IOException {
        int proposalNum = message.getProposalNumber();
        System.out.println(name + " received prepare with number " + proposalNum + " (current: " + currentProposalNum + ")");
        
        if (proposalNum > currentProposalNum) {
            currentProposalNum = proposalNum;
            Message response = new Message(MessageType.PROMISE, id, proposalNum, acceptedValue);
            out.writeObject(response);
            System.out.println(name + " promised to proposal " + proposalNum);
        } else {
            Message response = new Message(MessageType.NACK, id, currentProposalNum, -1);
            out.writeObject(response);
            System.out.println(name + " rejected proposal " + proposalNum);
        }
    }
    
    private void handleAccept(Message message, ObjectOutputStream out) throws IOException {
        int proposalNum = message.getProposalNumber();
        System.out.println(name + " received accept request for proposal " + proposalNum + " with value " + message.getValue());
        
        if (proposalNum >= currentProposalNum) {
            currentProposalNum = proposalNum;
            acceptedValue = message.getValue();
            acceptedProposals.put(proposalNum, new ProposalValue(proposalNum, acceptedValue));

            Message response = new Message(MessageType.ACCEPTED, id, proposalNum, acceptedValue);
            out.writeObject(response);
            System.out.println(name + " accepted proposal " + proposalNum + " with value " + acceptedValue);
        } else {
            Message response = new Message(MessageType.NACK, id, currentProposalNum, -1);
            out.writeObject(response);
            System.out.println(name + " rejected accept request for proposal " + proposalNum);
        }
    }
    
    private void handleLearn(Message message) {
        System.out.println(name + " learned that member " + message.getValue() + 
                          " has been elected as president.");
    }

    /**
     * Gets the highest accepted proposal value
     * @return The value of the highest numbered proposal, or -1 if no proposals exist
     */
    public int getHighestAcceptedValue() {
        if (acceptedProposals.isEmpty()) {
            return -1;
        }
        
        return acceptedProposals.values().stream()
            .max(Comparator.comparingInt(ProposalValue::getProposalNum))
            .map(ProposalValue::getValue)
            .orElse(-1);
    }
}