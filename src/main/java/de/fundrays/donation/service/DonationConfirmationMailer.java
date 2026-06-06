package de.fundrays.donation.service;

import de.fundrays.donation.domain.Donation;
import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.repository.OrganizationSettingsRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.jboss.logging.Logger;

@ApplicationScoped
public class DonationConfirmationMailer
{
	private static final Logger LOG = Logger.getLogger(DonationConfirmationMailer.class);
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

	@Inject
	Mailer mailer;

	@Inject
	OrganizationSettingsRepository settingsRepository;

	@CheckedTemplate(basePath = "mail")
	static class Templates
	{
		private Templates()
		{
			/* This utility class should not be instantiated */
		}

		static native TemplateInstance donorConfirmation(
			String orgName,
			String campaignTitle,
			String donorName,
			String amount,
			String date,
			String reference);

		static native TemplateInstance adminNotification(
			String orgName,
			String campaignTitle,
			String donorName,
			String donorEmail,
			String amount,
			String date,
			String reference);
	}

	public void sendConfirmation(Donation donation)
	{
		if (donation.donorEmail == null || donation.donorEmail.isBlank())
		{
			LOG.debugf("Skipping donor confirmation for donation %s — no donor email", donation.id);
			return;
		}

		OrganizationSettings settings = settingsRepository.load().orElse(new OrganizationSettings());

		String body = Templates.donorConfirmation(
			nullToDash(settings.orgName),
			donation.campaign.title,
			nullToDash(donation.donorName),
			formatEuros(donation.amount),
			formatDate(donation),
			nullToDash(donation.paymentProviderRef))
			.render();

		Mail mail = Mail.withText(donation.donorEmail.strip(),
			"Vielen Dank für Ihre Spende",
			body);
		applyFrom(mail, settings);
		mailer.send(mail);
	}

	public void sendAdminNotification(Donation donation)
	{
		OrganizationSettings settings = settingsRepository.load().orElse(null);
		if (settings == null || settings.adminNotificationEmail == null || settings.adminNotificationEmail.isBlank())
		{
			LOG.debugf("Skipping admin notification for donation %s — no admin email configured", donation.id);
			return;
		}

		String body = Templates.adminNotification(
			nullToDash(settings.orgName),
			donation.campaign.title,
			nullToDash(donation.donorName),
			nullToDash(donation.donorEmail),
			formatEuros(donation.amount),
			formatDate(donation),
			nullToDash(donation.paymentProviderRef))
			.render();

		Mail mail = Mail.withText(settings.adminNotificationEmail.strip(),
			"Neue bestätigte Spende",
			body);
		applyFrom(mail, settings);
		mailer.send(mail);
	}

	private static void applyFrom(Mail mail, OrganizationSettings settings)
	{
		if (settings.smtpFrom != null && !settings.smtpFrom.isBlank())
		{
			mail.setFrom(settings.smtpFrom.strip());
		}
	}

	private static String formatEuros(long cents)
	{
		return String.format(Locale.GERMAN, "%,.2f €", cents / 100.0);
	}

	private static String formatDate(Donation donation)
	{
		var instant = donation.confirmedAt != null ? donation.confirmedAt : donation.createdAt;
		return DATE_FMT.format(instant.atZone(ZoneId.systemDefault()));
	}

	private static String nullToDash(String value)
	{
		return value == null || value.isBlank() ? "–" : value;
	}
}
