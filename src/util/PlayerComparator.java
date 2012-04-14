package util;

import java.util.Comparator;

import agent.PlayerInfo;

public class PlayerComparator implements Comparator<PlayerInfo> {
	@Override
	public int compare(PlayerInfo o1, PlayerInfo o2) {		
		if (o1.m_distance < o2.m_distance)
			return -1;
		else if (o1.m_distance > o2.m_distance)
			return 1;
		else
			return 0;
	}
}
