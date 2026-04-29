package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.RefundRequest;

import java.util.List;

public interface IRefundService {
    RefundRequest createRefundRequest(String learnerId, Long invoiceId, String reason);
    RefundRequest approveRefund(Long refundId, String adminName);
    RefundRequest rejectRefund(Long refundId, String adminName, String reason);
    List<RefundRequest> getPendingRefunds();
    List<RefundRequest> getRefundsByLearner(String learnerId);
    List<RefundRequest> getAllRefunds();

}