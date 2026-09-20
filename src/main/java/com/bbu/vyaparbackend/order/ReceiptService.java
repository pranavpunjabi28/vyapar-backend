package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptService {
    private final OrderQueryService orders;
    private final PaymentService payments;

    public ReceiptService(OrderQueryService orders, PaymentService payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public byte[] pdf(Outlet outlet, SalesOrder order) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA), bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream c = new PDPageContentStream(doc, page)) {
                float y = 790;
                y = line(c, bold, 18, 50, y, outlet.getBusiness().getName());
                y = line(c, regular, 10, 50, y - 4, outlet.getName() + " | " + nullable(outlet.getPhone()));
                y = line(c, regular, 10, 50, y, "Invoice: " + order.getInvoiceNumber() + "    Date: " + order.getClosedAt());
                y = line(c, bold, 11, 50, y - 8, "Item                                      Qty       Price       Amount");
                List<OrderItem> receiptItems = orders.items(order.getId());
                Map<String, List<OrderItemAddon>> addonsByItem = orders.addonsByOrderItemIds(
                        receiptItems.stream().map(OrderItem::getId).toList());
                for (OrderItem i : receiptItems) {
                    y = line(c, regular, 10, 50, y, String.format("%-38s %7s %11s %11s", truncate(i.getProductName(), 36), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()));
                    for (OrderItemAddon addon : addonsByItem.getOrDefault(i.getId(), List.of())) {
                        y = line(c, regular, 9, 62, y, "+ " + truncate(addon.getOptionName(), 34)
                                + "  " + addon.getUnitPrice());
                    }
                    if (i.getNote() != null && !i.getNote().isBlank()) {
                        y = line(c, regular, 9, 62, y, "Note: " + truncate(i.getNote(), 50));
                    }
                }
                y = line(c, regular, 10, 50, y - 5, "Subtotal: " + order.getSubtotal());
                y = line(c, regular, 10, 50, y, "Discount: " + order.getDiscountAmount());
                y = line(c, bold, 12, 50, y, "Total: " + order.getTotal());
                y = line(c, regular, 10, 50, y, "Paid: " + order.getPaidAmount() + "    Due: " + order.getDueAmount());
                List<String> paymentLines = payments.list(order.getId()).stream().map(p -> p.getMethod() + ": " + p.getAmount()).toList();
                for (String p : paymentLines) y = line(c, regular, 9, 50, y, p);
                if (outlet.getUpiId() != null && !outlet.getUpiId().isBlank()) {
                    String uri = "upi://pay?pa=" + enc(outlet.getUpiId()) + "&pn=" + enc(outlet.getBusiness().getName()) + "&am=" + order.getDueAmount() + "&cu=" + outlet.getCurrency();
                    BufferedImage qr = MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 180, 180));
                    c.drawImage(LosslessFactory.createFromImage(doc, qr), 50, Math.max(90, y - 190), 140, 140);
                }
                line(c, regular, 10, 50, 70, outlet.getReceiptFooter() == null ? "Thank you for your business." : outlet.getReceiptFooter());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate receipt", e);
        }
    }

    private float line(PDPageContentStream c, PDFont font, float size, float x, float y, String text) throws Exception {
        c.beginText();
        c.setFont(font, size);
        c.newLineAtOffset(x, y);
        c.showText(safe(text));
        c.endText();
        return y - 16;
    }

    private String safe(String s) {
        return s == null ? "" : s.replaceAll("[^\\x20-\\x7E]", "");
    }

    private String nullable(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
