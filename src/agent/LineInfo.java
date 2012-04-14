package agent;

//***************************************************************************
//
//	This class holds visual information about line
//
//	Extract from ObjectInfo, since we need to use it from other packages.
//***************************************************************************
public class LineInfo extends ObjectInfo
{
public char m_kind;  // l|r|t|b

//===========================================================================
// Initialization member functions
public LineInfo()
{
  super("line");
}

public LineInfo(char kind)
{
  super("line");
  m_kind = kind;
}
}
