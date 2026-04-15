package com.example.msforum.services;

import java.io.ByteArrayInputStream;

public interface PdfService {
    ByteArrayInputStream generateParticipantPdf(Long eventId, Long participantId);
}
