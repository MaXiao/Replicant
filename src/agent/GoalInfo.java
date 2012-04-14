package agent;

//***************************************************************************
//
//	This class holds visual information about goal
//
//	Extract from ObjectInfo, since we need to use it from other packages.
//***************************************************************************
public class GoalInfo extends ObjectInfo
{
public char m_side;
//===========================================================================
// Initialization member functions
public GoalInfo()
{
  super("goal");
  m_side = ' ';
}

public GoalInfo(char side)
{
  super("goal " + side);
  m_side = side;
}

public char getSide()
{
  return m_side;
}
}
