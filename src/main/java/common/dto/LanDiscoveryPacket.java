package common.dto;

import java.io.Serializable;

public class LanDiscoveryPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String caseTitle;
    private final String hostDisplayName;
    private final boolean isPublic;
    private final String joinCode;
    private final int tcpPort;

    public LanDiscoveryPacket(String sessionId, String caseTitle, String hostDisplayName, boolean isPublic, String joinCode, int tcpPort) {
        this.sessionId = sessionId;
        this.caseTitle = caseTitle;
        this.hostDisplayName = hostDisplayName;
        this.isPublic = isPublic;
        this.joinCode = joinCode;
        this.tcpPort = tcpPort;
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public String getHostDisplayName() {
        return hostDisplayName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public int getTcpPort() {
        return tcpPort;
    }
}
