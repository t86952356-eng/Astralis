package astralis.ui.screen;

import java.util.List;

public final class NiceAltsHistoryEntry {
    final String orderId;
    final String productId;
    final String timestamp;
    final List<String> items;
    final String accountNames;
    final String refundStatus;

    NiceAltsHistoryEntry(
            String orderId,
            String productId,
            String timestamp,
            List<String> items,
            String accountNames,
            String refundStatus) {
        super();
        this.orderId = orderId != null && !orderId.isBlank() ? orderId : null;
        this.productId = productId != null && !productId.isBlank() ? productId : null;
        this.timestamp =
                timestamp != null && !timestamp.isBlank()
                        ? timestamp
                        : NiceAltsScreen.currentTimestamp();
        this.items = List.copyOf(items == null ? List.of() : items);
        this.accountNames =
                accountNames != null && !accountNames.isBlank() ? accountNames : "Generated";
        this.refundStatus = refundStatus == null ? "" : refundStatus;
    }
}
