package de.fundrays.campaign.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.repository.CampaignRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

@Path("/api/og")
public class OgImageResource
{
	static {
		// Required for headless server environments (no display)
		System.setProperty("java.awt.headless", "true");
	}

	private static final int WIDTH = 1200;
	private static final int HEIGHT = 630;
	private static final Color BG = new Color(0x16, 0x0d, 0x14);
	private static final Color BRAND = new Color(0xed, 0x2a, 0x91);
	private static final Color BRAND_DARK = new Color(0xc4, 0x1e, 0x77);

	@Inject
	CampaignRepository campaignRepository;

	@GET
	@Path("/{slug}.png")
	@Produces("image/png")
	public Response generate(@PathParam("slug") String slug)
	{
		Campaign campaign = campaignRepository.findBySlug(slug).orElse(null);
		if (campaign == null)
		{
			return Response.status(404).build();
		}

		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		// Background
		g.setColor(BG);
		g.fillRect(0, 0, WIDTH, HEIGHT);

		// Left accent stripe
		GradientPaint stripe = new GradientPaint(0, 0, BRAND_DARK, 0, HEIGHT, BRAND);
		g.setPaint(stripe);
		g.fillRect(0, 0, 12, HEIGHT);

		// "fundrays" label top-left
		g.setColor(BRAND);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
		g.drawString("fundrays", 60, 84);

		// Campaign title — large, white, word-wrapped
		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 72));
		drawWrapped(g, campaign.title, 60, WIDTH - 60, 240, 96);

		// Bottom brand line
		GradientPaint bottom = new GradientPaint(0, 0, BRAND_DARK, WIDTH, 0, BRAND);
		g.setPaint(bottom);
		g.fillRect(0, HEIGHT - 8, WIDTH, 8);

		g.dispose();

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "png", baos);
			return Response.ok(baos.toByteArray())
				.header("Cache-Control", "public, max-age=3600")
				.build();
		}
		catch (Exception e)
		{
			return Response.serverError().build();
		}
	}

	private void drawWrapped(Graphics2D g, String text, int x, int maxX, int startY, int lineHeight)
	{
		FontMetrics fm = g.getFontMetrics();
		int availableWidth = maxX - x;
		String[] words = text.split(" ");
		StringBuilder line = new StringBuilder();
		int y = startY;

		for (String word : words)
		{
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (fm.stringWidth(candidate) > availableWidth)
			{
				g.drawString(line.toString(), x, y);
				y += lineHeight;
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(candidate);
			}
		}
		if (!line.isEmpty())
		{
			g.drawString(line.toString(), x, y);
		}
	}
}
