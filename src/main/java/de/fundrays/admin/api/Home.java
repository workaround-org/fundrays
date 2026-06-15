package de.fundrays.admin.api;

import io.quarkiverse.renarde.Controller;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/")
public class Home extends Controller
{
	@GET
	@Path("/")
	public Response index()
	{
		// The start page is the admin login.
		return Response.seeOther(URI.create("/login")).build();
	}
}
