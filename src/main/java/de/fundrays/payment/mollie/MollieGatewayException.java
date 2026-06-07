package de.fundrays.payment.mollie;

public class MollieGatewayException extends RuntimeException
{
	public MollieGatewayException(String message)
	{
		super(message);
	}
}
