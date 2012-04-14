//
//	File:			ObjectInfo.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28

//  Modified by:  Paul Marlow, Amir Ghavam, Yoga Selvaraj
//  Course:       Software Agents
//  Date Due:     November 30, 2000

//  Modified by:  Edgar Acosta
//  Date:         March 4, 2008

//***************************************************************************
//
//	This is base class for different classese with visual information
//	about objects
//
//***************************************************************************
package agent;

public class ObjectInfo
{
  public String m_type;
  public float m_distance;
  public float m_direction;
  public float m_distChange;
  public float m_dirChange;

  //===========================================================================
  // Initialization member functions
  public ObjectInfo(String type)
  {
    m_type = type;
  }

  public float getDistance()
  {
    return m_distance;
  }

  public float getDirection()
  {
    return m_direction;
  }

  public float getDistChange()
  {
    return m_distChange;
  }

  public float getDirChange()
  {
    return m_dirChange;
  }

  public String getType()
  {
    return m_type;
  }
}




//***************************************************************************
//
//	This class holds visual information about ball
//
//***************************************************************************
class BallInfo extends ObjectInfo
{
  //===========================================================================
  // Initialization member functions
  public BallInfo()
  {
    super("ball");
  }
}
