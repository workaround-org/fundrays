package de.fundrays.shared;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class BadGatewayException extends WebApplicationException
{

	public BadGatewayException(String message)
	{
		super(Response.status(Response.Status.BAD_GATEWAY)
			.entity(new ErrorResponse(message))
			.type(MediaType.APPLICATION_JSON)
			.build());
	}
}
