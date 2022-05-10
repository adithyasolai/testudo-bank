package net.testudobank;

import lombok.Builder;
import lombok.Getter;

/**
 * Transaction History Model
 */
@Builder
@Getter
public class TransactionHistoryEntry {
    public final String time;
    public final String action;
    public final String amount;
}
