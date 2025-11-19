package client.discovery;

import java.util.ArrayList;
import java.util.List;
import common.NetworkConstants;

public class StubLanGameDiscoveryService implements LanGameDiscoveryService {

    private final List<DiscoveredGame> dummyGames = new ArrayList<>();

    public StubLanGameDiscoveryService() {
        // Create some dummy data for testing
        dummyGames.add(new DiscoveredGame(
                "The Serpent's Kiss",
                "Sherlock",
                true,
                null,
                "192.168.1.101",
                NetworkConstants.DEFAULT_PORT,
                1,
                2,
                "session123"
        ));
        dummyGames.add(new DiscoveredGame(
                "The Crimson Heirloom",
                "Moriarty",
                true,
                null,
                "192.168.1.102",
                NetworkConstants.DEFAULT_PORT,
                1,
                2,
                "session456"
        ));
        dummyGames.add(new DiscoveredGame(
                "A Study in Scarlet",
                "Watson",
                false, // Private game
                "ABCDE",
                "192.168.1.103",
                NetworkConstants.DEFAULT_PORT,
                1,
                2,
                "session789"
        ));
    }

    @Override
    public List<DiscoveredGame> getCurrentGames() {
        // In a real implementation, this would return a cached list
        // that is updated by a background discovery thread.
        return new ArrayList<>(dummyGames);
    }

    @Override
    public void refreshAsync() {
        // In a real implementation, this would trigger a new UDP multicast.
        // For the stub, we can just print a message.
        System.out.println("[STUB DISCOVERY] Refresh triggered. No-op for stub service.");
    }
}
