import java.util.*;

/**
 * Tests harness for the Paxos voting system
 */
public class TestHarness {
    private final Council council;
    private static final int TEST_DURATION_MS = 5000;
    private static final int PROPOSAL_DELAY_MS = 500;
    
    public TestHarness() {
        council = new Council();
    }
    
    /**
     * Runs all test scenarios
     */
    public void runTests() {
        System.out.println("=== Adelaide Suburbs Council Election Test Results ===\n");
        try {
            testSimultaneousProposals();
            testImmediateResponses();
            testDelayedResponses();
            testOfflineMembers();
        } catch (Exception e) {
            System.err.println("Error in test suite: " + formatError(e));
        } finally {
            // Ensure cleanup happens
            try {
                council.stopCouncil();
            } catch (Exception e) {
                System.err.println("Error stopping council: " + formatError(e));
            }
        }
        System.out.println("\n=== Test Suite Complete ===");
    }

    private String formatError(Exception e) {
        return String.format("Error type: %s, Message: %s, Location: %s",
            e.getClass().getSimpleName(),
            e.getMessage(),
            e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "unknown");
    }

    /**
     * Tests when two members propose simultaneously
     * Validates requirement: "Paxos implementation works when two councillors send voting proposals at the same time"
     */
    private void testSimultaneousProposals() {
        System.out.println("Test Case 1: Simultaneous Proposals");
        System.out.println("-----------------------------------");
        try {
            council.resetMemberProfiles();  // Ensure clean state
            council.startCouncil();
            
            // Give council time to start up
            Thread.sleep(1000);
            
            // Track initial proposal values
            Map<Integer, Integer> initialValues = new HashMap<>();
            for (CouncilMember member : council.getMembers()) {
                initialValues.put(member.getId(), member.getHighestAcceptedValue());
            }
            
            // Simulate M1 and M2 proposing simultaneously
            Thread m1Thread = new Thread(() -> {
                try {
                    Thread.sleep(100);  // Small delay to ensure both are ready
                    System.out.println("M1 starting proposal");
                    council.getMembers().get(0).proposePresident();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("M1 proposal interrupted: " + formatError(e));
                } catch (RuntimeException e) {
                    System.err.println("Unexpected error in M1 proposal: " + formatError(e));
                }
            });
            
            Thread m2Thread = new Thread(() -> {
                try {
                    Thread.sleep(100);  // Small delay to ensure both are ready
                    System.out.println("M2 starting proposal");
                    council.getMembers().get(1).proposePresident();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("M2 proposal interrupted: " + formatError(e));
                } catch (RuntimeException e) {
                    System.err.println("Unexpected error in M2 proposal: " + formatError(e));
                }
            });
            
            m1Thread.start();
            m2Thread.start();
            
            try {
                Thread.sleep(TEST_DURATION_MS);
                m1Thread.join(PROPOSAL_DELAY_MS);
                m2Thread.join(PROPOSAL_DELAY_MS);

                boolean changesDetected = false;
                for (CouncilMember member : council.getMembers()) {
                    int initialValue = initialValues.get(member.getId());
                    int currentValue = member.getHighestAcceptedValue();
                    if (initialValue != currentValue) {
                        System.out.println("Member M" + member.getId() + 
                            " changed value from " + initialValue + " to " + currentValue);
                        changesDetected = true;
                    }
                }

                if (!changesDetected) {
                    System.out.println("Warning: No value changes detected during simultaneous proposals");
                }
            
                // Compare final values
                verifyConsensus("Simultaneous Proposals");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Test interrupted: " + formatError(e));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Test interrupted before completion: " + formatError(e));
        } catch (SecurityException e) {
            System.err.println("Security violation during thread operations: " + formatError(e));
        } catch (RuntimeException e) {
            System.err.println("Unexpected error during test execution: " + formatError(e));
        } finally {
            council.stopCouncil();
            // Give time for cleanup
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Cleanup interrupted: " + formatError(e));
            }
        }
        System.out.println();
    }

    /**
     * Tests immediate response scenario
     * Validates requirement: "Paxos implementation works in the case where all M1-M9 have immediate responses"
     */
    private void testImmediateResponses() {
        System.out.println("Test Case 2: Immediate Responses");
        System.out.println("--------------------------------");

        try {
            // Set all members to IMMEDIATE profile
            for (CouncilMember member : council.getMembers()) {
                member.setResponseProfile(CouncilMember.ResponseProfile.IMMEDIATE);
            }

            council.startCouncil();

            // Debug output
            System.out.println("Council started, attempting proposal...");

            runSingleProposal(0); // Let M1 propose

            // Debug output
            System.out.println("Proposal complete, stopping council...");

            council.stopCouncil();
            System.out.println("Council stopped successfully");
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error in immediate responses test: " + formatError(e));
            // Ensure council is stopped even if test fails
            council.stopCouncil();
        }
    }

    /**
     * Tests delayed response scenario
     * Validates requirement: "Paxos implementation works when M1-M9 have varying response profiles"
     */
    private void testDelayedResponses() {
        System.out.println("Test Case 3: Delayed Responses");
        System.out.println("------------------------------");

        // Reset to original profiles
        council.resetMemberProfiles();
        council.startCouncil();
        runSingleProposal(1); // Let M2 propose
        council.stopCouncil();
        System.out.println();
    }

    /**
     * Tests offline members scenario
     * Validates requirement: "Paxos implementation works when members go offline"
     */
    private void testOfflineMembers() {
        System.out.println("Test Case 4: Offline Members");
        System.out.println("----------------------------");

        council.startCouncil();

        // Simulate M2 and M3 going offline
        council.getMembers().get(1).stop();
        council.getMembers().get(2).stop();
        
        System.out.println("M2 and M3 are offline");
        runSingleProposal(0); // Let M1 propose

        council.stopCouncil();
        System.out.println();
    }

    /**
     * Helper method to run a single proposal and verify results
     * @param proposerId ID of the proposing member
     */
    private void runSingleProposal(int proposerId) {
        try {
            // Give time for council to be ready
            Thread.sleep(1000);
            
            System.out.println("Starting proposal from M" + (proposerId + 1));
            boolean result = council.getMembers().get(proposerId).proposePresident();
            System.out.println("Proposal from M" + (proposerId + 1) + " result: " + (result ? "Succeeded" : "Failed"));
            
            // Wait for consensus to be reached
            Thread.sleep(TEST_DURATION_MS);
            
            verifyConsensus("Single Proposal M" + (proposerId + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Test interrupted: " + formatError(e));
        } catch (Exception e) {
            System.err.println("Error in proposal: " + formatError(e));
        }
    }

    /**
     * Verifies that consensus has been reached among active members
     * @param testName Name of the test being verified
     */
    private void verifyConsensus(String testName) {
        try {
            Map<Integer, Integer> finalValues = new HashMap<>();
            int consensusValue = -1;
            int consensusCount = 0;
        
            // Collect final values and find most common value
            for (CouncilMember member : council.getMembers()) {
                int value = member.getHighestAcceptedValue();
                finalValues.put(member.getId(), value);

                if (value != -1) {
                    if (consensusValue == -1) {
                        consensusValue = value;
                        consensusCount = 1;
                    } else if (value == consensusValue) {
                        consensusCount++;
                    }
                }
            }
        
            // Print detailed results including all member values
            System.out.println("Test: " + testName);
            System.out.println("Final values by member:");
            finalValues.forEach((memberId, value) -> System.out.println("M" + memberId + ": " + (value == -1 ? "No value" : value)));

            System.out.println("Concensus reached: " + (consensusCount >= Council.MAJORITY_SIZE));
            System.out.println("Winning value: " + (consensusValue != -1 ? "M" + consensusValue : "None"));
            System.out.println("Agreement count: " + consensusCount + "/" + Council.TOTAL_MEMBERS);
        } catch (Exception e) {
            System.err.println("Error verifying consensus: " + formatError(e));
        }
    }
}
