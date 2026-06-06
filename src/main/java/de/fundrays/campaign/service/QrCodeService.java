package de.fundrays.campaign.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@ApplicationScoped
public class QrCodeService
{
	private static final int MARGIN = 1;

	public byte[] renderPng(String payload, int size)
	{
		BitMatrix matrix = encode(payload, size);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
			return baos.toByteArray();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Failed to render QR code PNG", e);
		}
	}

	public String renderSvg(String payload, int size)
	{
		BitMatrix matrix = encode(payload, size);
		int width = matrix.getWidth();
		int height = matrix.getHeight();

		StringBuilder sb = new StringBuilder(width * height);
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
			.append("viewBox=\"0 0 ").append(width).append(' ').append(height).append("\" ")
			.append("shape-rendering=\"crispEdges\">");
		sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>");
		sb.append("<path fill=\"#000000\" d=\"");
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if (matrix.get(x, y))
				{
					sb.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
				}
			}
		}
		sb.append("\"/></svg>");
		return sb.toString();
	}

	private BitMatrix encode(String payload, int size)
	{
		Map<EncodeHintType, Object> hints = Map.of(
			EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
			EncodeHintType.MARGIN, MARGIN,
			EncodeHintType.CHARACTER_SET, "UTF-8");
		try
		{
			return new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
		}
		catch (WriterException e)
		{
			throw new IllegalArgumentException("Failed to encode QR payload: " + payload, e);
		}
	}
}
