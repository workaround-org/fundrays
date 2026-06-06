package de.fundrays.admin.api;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/login")
public class Login extends Controller
{
	@CheckedTemplate
	static class Templates
	{
		private Templates()
		{
			/* This utility class should not be instantiated */
		}

		static native TemplateInstance login(boolean error);
	}

	@GET
	@Path("/")
	public TemplateInstance login(@QueryParam("error") boolean error)
	{
		return Templates.login(error);
	}
}
