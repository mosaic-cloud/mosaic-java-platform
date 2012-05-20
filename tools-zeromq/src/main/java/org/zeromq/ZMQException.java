
package org.zeromq;


/**
 * ZeroMQ runtime exception.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQException
		extends RuntimeException
{
	public ZMQException (final String message, final int errorCode)
	{
		super (message);
		this.errorCode = errorCode;
	}
	
	/**
	 * @return error code
	 */
	public int getErrorCode ()
	{
		return this.errorCode;
	}
	
	@Override
	public String toString ()
	{
		return super.toString () + "(0x" + Integer.toHexString (this.errorCode) + ")";
	}
	
	private int errorCode = 0;
	private static final long serialVersionUID = -978820750094924644L;
}
