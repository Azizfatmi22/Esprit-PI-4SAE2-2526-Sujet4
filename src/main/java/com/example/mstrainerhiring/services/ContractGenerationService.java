package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.entities.TrainerHiring;

public interface ContractGenerationService {
    byte[] generateContractPdf(TrainerHiring trainer) throws Exception;
}
