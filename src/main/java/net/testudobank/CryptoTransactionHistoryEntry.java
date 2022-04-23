package net.testudobank;

import lombok.Builder;
import lombok.Getter;

/**
 * Crypto Transaction History Model
 */
@Builder
@Getter
public class CryptoTransactionHistoryEntry {
    public final String time;
    public final String action;
    public final String amount;
    public final String cryptoName;
}
