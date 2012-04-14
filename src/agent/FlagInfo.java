package agent;

//***************************************************************************
//
//	This class holds visual information about flag
//
//  Extract from ObjectInfo, since we need to use it from other packages.
//***************************************************************************
public class FlagInfo extends ObjectInfo
{
public char m_type;  // p|g
public char m_pos1;  // t|b|l|c|r
public char m_pos2;  // l|r|t|c|b
public int m_num;    // 0|10|20|30|40|50
public boolean m_out;

//===========================================================================
// Initialization member functions
public FlagInfo()
{
  super("flag");
  m_type = ' ';
  m_pos1 = ' ';
  m_pos2 = ' ';
  m_num = 0;
  m_out = false;
}

public FlagInfo(String flagType, char type, char pos1, char pos2,
                int num, boolean out)
{
  super(flagType);
  m_type = type;
  m_pos1 = pos1;
  m_pos2 = pos2;
  m_num = num;
  m_out = out;
}

public FlagInfo(char type, char pos1, char pos2, int num, boolean out)
{
  super("flag");
  m_type = type;
  m_pos1 = pos1;
  m_pos2 = pos2;
  m_num = num;
  m_out = out;
}
}
