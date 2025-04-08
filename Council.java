import java.util.*;

/**
 * Represents the Adelaide Suburbs Council and manages all council members
 */
public class Council {
    public static final int TOTAL_MEMBERS = 9;
    public static final int MAJORITY_SIZE = (TOTAL_MEMBERS / 2) + 1;
    private final List<CouncilMember> members;

    public Council() {
        members = new ArrayList<>();
        initialiseMembers();
    }

    public List<CouncilMember> getMembers () {
        return members;
    }

    /**
     * Initialises all council members with their respective profiles
     */
    private void initialiseMembers() {
        // M1 - Very responsive
        members.add(new CouncilMember(1, "M1", 8001, CouncilMember.ResponseProfile.IMMEDIATE));

        // M2 - Unreliable internet
        members.add(new CouncilMember(2, "M2", 8002, CouncilMember.ResponseProfile.UNRELIABLE));

        // M3 - Sometimes completely offline
        members.add(new CouncilMember(3, "M3", 8003, CouncilMember.ResponseProfile.UNRELIABLE));

        // M4-M9 - Variable response times
        for (int i = 4; i <= 9; i++) {
            members.add(new CouncilMember(i, "M" + i, 8000 + i, CouncilMember.ResponseProfile.SMALL_DELAY));
        }
    }

    /**
     * Resets all members to their original response profiles
     */
    public void resetMemberProfiles() {
        members.get(0).setResponseProfile(CouncilMember.ResponseProfile.IMMEDIATE); // M1
        members.get(1).setResponseProfile(CouncilMember.ResponseProfile.UNRELIABLE); // M2
        members.get(2).setResponseProfile(CouncilMember.ResponseProfile.UNRELIABLE); // M3
        for (int i = 3; i < TOTAL_MEMBERS; i++) {
            members.get(i).setResponseProfile(CouncilMember.ResponseProfile.SMALL_DELAY);
        }
    }

    /**
     * Starts all council members
     */
    public void startCouncil() {
        for (CouncilMember member : members) {
            member.start();
        }
    }

    /**
     * Stops all council members
     */
    public void stopCouncil() {
        for (CouncilMember member : members) {
            member.stop();
        }
    }
}
