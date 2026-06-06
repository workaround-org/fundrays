package de.fundrays.campaign.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/d")
public class ShortLinkResource
{
	@GET
	@Path("/{slug}")
	public Response redirect(@PathParam("slug") String slug)
	{
		return Response.status(Response.Status.FOUND)
			.location(URI.create("/donate/" + slug))
			.build();
	}
}
