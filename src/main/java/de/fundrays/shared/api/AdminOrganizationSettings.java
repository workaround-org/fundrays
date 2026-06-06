package de.fundrays.shared.api;

import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.service.OrganizationSettingsService;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.time.LocalDate;
import org.jboss.resteasy.reactive.RestForm;

@Path("/admin/settings")
@RolesAllowed("admin")
public class AdminOrganizationSettings extends Controller
{
	@Inject
	OrganizationSettingsService settingsService;

	@CheckedTemplate
	static class Templates
	{
		static native TemplateInstance index(OrganizationSettings settings, String exemptionDateValue, boolean smtpPasswordSet);
	}

	@GET
	@Path("/")
	public TemplateInstance index()
	{
		OrganizationSettings settings = settingsService.loadOrCreate();
		String exemptionDateValue = settings.orgExemptionDate != null ? settings.orgExemptionDate.toString() : "";
		boolean smtpPasswordSet = settings.smtpPassword != null && !settings.smtpPassword.isBlank();
		return Templates.index(settings, exemptionDateValue, smtpPasswordSet);
	}

	@POST
	@Path("/")
	public void save(
		@RestForm String orgName,
		@RestForm String orgStreet,
		@RestForm String orgZip,
		@RestForm String orgCity,
		@RestForm String orgTaxId,
		@RestForm String orgIssuingAuthority,
		@RestForm String orgExemptionDate,
		@RestForm String orgPurpose,
		@RestForm String smtpHost,
		@RestForm Integer smtpPort,
		@RestForm String smtpUser,
		@RestForm String smtpPassword,
		@RestForm String smtpFrom,
		@RestForm String adminNotificationEmail)
	{

		OrganizationSettings updates = new OrganizationSettings();
		updates.orgName = blankToNull(orgName);
		updates.orgStreet = blankToNull(orgStreet);
		updates.orgZip = blankToNull(orgZip);
		updates.orgCity = blankToNull(orgCity);
		updates.orgTaxId = blankToNull(orgTaxId);
		updates.orgIssuingAuthority = blankToNull(orgIssuingAuthority);
		updates.orgExemptionDate = parseDate(orgExemptionDate);
		updates.orgPurpose = blankToNull(orgPurpose);
		updates.smtpHost = blankToNull(smtpHost);
		updates.smtpPort = smtpPort;
		updates.smtpUser = blankToNull(smtpUser);
		updates.smtpFrom = blankToNull(smtpFrom);
		updates.adminNotificationEmail = blankToNull(adminNotificationEmail);

		settingsService.update(updates, smtpPassword);

		flash("message", "Einstellungen gespeichert.");
		index();
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static LocalDate parseDate(String value)
	{
		if (value == null || value.isBlank()) return null;
		return LocalDate.parse(value);
	}
}
