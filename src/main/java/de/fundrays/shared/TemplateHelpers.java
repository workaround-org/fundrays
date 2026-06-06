package de.fundrays.shared;

import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.time.Instant;
import java.time.ZoneOffset;

public class TemplateHelpers
{
	private static final Parser MARKDOWN_PARSER = Parser.builder().build();
	private static final HtmlRenderer MARKDOWN_RENDERER = HtmlRenderer.builder().build();

	/** {someAmount.euros()} → "12.34 €" */
	@TemplateExtension
	static String euros(Long cents)
	{
		return String.format("%.2f €", cents / 100.0);
	}

	/** {someAmount.eurosAmount()} → "12.34" (no symbol, for form inputs) */
	@TemplateExtension
	static String eurosAmount(Long cents)
	{
		return String.format("%.2f", cents / 100.0);
	}

	/** {someInstant.isoDate()} → "2026-01-15" or "" if null */
	@TemplateExtension
	static String isoDate(Instant instant)
	{
		if (instant == null) return "";
		return instant.atZone(ZoneOffset.UTC).toLocalDate().toString();
	}

	/** {fmt:percent(raised, goal)} → 0-100 int, capped at 100 */
	@TemplateExtension(namespace = "fmt")
	static int percent(long part, long total)
	{
		if (total == 0) return 0;
		return (int)Math.min(100L, part * 100L / total);
	}

	/**
	 * {someText.markdown()} → CommonMark rendered to HTML, returned as a
	 * RawString so Qute does not re-escape it. Raw HTML in the source passes
	 * through unescaped, so this must only be used on trusted, admin-authored
	 * content (e.g. campaign.description, set exclusively
	 * via @RolesAllowed("admin") routes). Add a sanitizer before applying it to
	 * any non-admin input.
	 */
	@TemplateExtension
	static RawString markdown(String text)
	{
		if (text == null || text.isBlank()) return new RawString("");
		return new RawString(MARKDOWN_RENDERER.render(MARKDOWN_PARSER.parse(text)));
	}
}
