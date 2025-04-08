import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final int senderId;
    private final int proposalNumber;
    private final int value;

    public Message(MessageType type, int senderId, int proposalNumber, int value) {
        this.type = type;
        this.senderId = senderId;
        this.proposalNumber = proposalNumber;
        this.value = value;
    }

    // Getters
    public MessageType getType() { return type; }
    public int getSenderId() { return senderId; }
    public int getProposalNumber() { return proposalNumber; }
    public int getValue() { return value; }
}
