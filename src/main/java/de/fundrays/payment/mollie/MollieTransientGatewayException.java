package de.fundrays.payment.mollie;

public class MollieTransientGatewayException extends MollieGatewayException
{
	public MollieTransientGatewayException(String message)
	{
		super(message);
	}
}
