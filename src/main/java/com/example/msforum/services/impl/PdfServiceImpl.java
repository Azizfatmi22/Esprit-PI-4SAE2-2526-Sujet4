package com.example.msforum.services.impl;

import com.example.msforum.entities.Event;
import com.example.msforum.entities.Participant;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.EventRepository;
import com.example.msforum.repositories.ParticipantRepository;
import com.example.msforum.services.PdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfServiceImpl implements PdfService {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    @Override
    public ByteArrayInputStream generateParticipantPdf(Long eventId, Long participantId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id " + participantId));

        if (!participant.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("Participant does not belong to this event");
        }

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font definitions
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(79, 172, 254)); // Matching
                                                                                                            // cyan
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("Event Participation Ticket", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Separator
            document.add(new LineSeparator());

            // Event Details Section
            Paragraph eventHeader = new Paragraph("Event Details", headerFont);
            eventHeader.setSpacingBefore(20);
            eventHeader.setSpacingAfter(10);
            document.add(eventHeader);

            addDetail(document, "Event Name:", event.getName(), labelFont, valueFont);

            String dateStr = event.getDate() != null
                    ? event.getDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm"))
                    : "N/A";
            addDetail(document, "Date:", dateStr, labelFont, valueFont);
            addDetail(document, "Location:", event.getLocation(), labelFont, valueFont);
            addDetail(document, "Category:", event.getCategory(), labelFont, valueFont);

            // Participant Details Section
            Paragraph userHeader = new Paragraph("Participant Information", headerFont);
            userHeader.setSpacingBefore(20);
            userHeader.setSpacingAfter(10);
            document.add(userHeader);

            addDetail(document, "Name:", participant.getName(), labelFont, valueFont);
            addDetail(document, "Email:", participant.getEmail(), labelFont, valueFont);
            addDetail(document, "Status:", participant.getStatus().toString(), labelFont, valueFont);

            // Footer / Disclaimer
            document.add(new LineSeparator());
            Paragraph footer = new Paragraph(
                    "\nThis is a valid participation confirmation. Please present this when requested.",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            log.error("Error creating PDF: {}", e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addDetail(Document doc, String label, String value, Font labelFont, Font valueFont)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));
        p.setSpacingAfter(5);
        doc.add(p);
    }
}
